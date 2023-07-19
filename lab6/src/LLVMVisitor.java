import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;



import java.util.List;

import static org.bytedeco.llvm.global.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.LLVMSetInitializer;


public class LLVMVisitor extends SysYParserBaseVisitor<LLVMValueRef>{
    //创建module
    LLVMModuleRef module;

    //初始化IRBuilder，后续将使用这个builder去生成LLVM IR
    LLVMBuilderRef builder;

    //考虑到我们的语言中仅存在int一个基本类型，可以通过下面的语句为LLVM的int型重命名方便以后使用
    LLVMTypeRef i32Type;

    LLVMTypeRef i1Type;
    //创建一个常量,这里是常数0
    LLVMValueRef zero ;
    // 空类型
    LLVMTypeRef voidType;

    LLVMValueRef trueVal;
    LLVMValueRef falseVal;

    private Scope currentScope = null;

    private GlobalScope globalScope = null;

    private int localScopeCounter = 0;

    private final RuleNames ruleNames;

    private boolean assign = false;

    private boolean returnFlag = false;

    private LLVMValueRef currentFunc = null;

    private WhileBlock whileBlock = null;

    public LLVMVisitor(RuleNames ruleNames){
        //初始化LLVM
        LLVMInitializeCore(LLVMGetGlobalPassRegistry());
        LLVMLinkInMCJIT();
        LLVMInitializeNativeAsmPrinter();
        LLVMInitializeNativeAsmParser();
        LLVMInitializeNativeTarget();
        //创建module
        this.module = LLVMModuleCreateWithName("module");

        //初始化IRBuilder，后续将使用这个builder去生成LLVM IR
        this.builder = LLVMCreateBuilder();

        //考虑到我们的语言中仅存在int一个基本类型，可以通过下面的语句为LLVM的int型重命名方便以后使用
        this.i32Type = LLVMInt32Type();
        this.i1Type = LLVMInt1Type();

        this.voidType = LLVMVoidType();

        this.ruleNames = ruleNames;

        this.zero = LLVMConstInt(i32Type,0,0);
        this.trueVal = LLVMConstInt(i1Type,1,0);
        this.falseVal = LLVMConstInt(i1Type,0,0);
    }

    @Override
    public LLVMValueRef visitProgram(SysYParser.ProgramContext ctx) {
        globalScope = new GlobalScope(null);
        currentScope = globalScope;
        return super.visitProgram(ctx);
    }

    @Override
    public LLVMValueRef visitBlock(SysYParser.BlockContext ctx) {
        // 创建局部作用域
        LocalScope localScope = new LocalScope(currentScope);
        String localScopeName = localScope.getName() + localScopeCounter;
        localScope.setName(localScopeName);
        localScopeCounter++;
        currentScope = localScope;
        return super.visitBlock(ctx);
    }

    @Override
    public LLVMValueRef visitTerminal(TerminalNode node) {
        String lexerName = node.getSymbol().getType() > 0 ? ruleNames.lexerRuleNames[node.getSymbol().getType() - 1] : "EOF";
        // 退出作用域
        if (lexerName.equals("R_BRACE")) {
            // 获取 "}" 的父节点名
            String parentName = ruleNames.parserRuleNames[((RuleNode) node.getParent()).getRuleContext().getRuleIndex()];
            // 如果是block则此时退出当前局部作用域
            if (parentName.equals("block")) {
                currentScope = currentScope.getEnclosingScope();
            }
            // 进一步获取block的父节点名
            parentName = ruleNames.parserRuleNames[((RuleNode) node.getParent().getParent()).getRuleContext().getRuleIndex()];
            // 如果是funcDef则需要再退出外部的函数作用域
            if (parentName.equals("funcDef")) {
                if (!returnFlag){
                    LLVMBuildRetVoid(builder);
                }
                currentScope = currentScope.getEnclosingScope();
                returnFlag = false;
                currentFunc = null;
            }
        }
        // 遇到EOF则退出当前作用域(全局作用域)
        if (lexerName.equals("EOF")) {
            currentScope = currentScope.getEnclosingScope();
        }
        return super.visitTerminal(node);
    }
    // 函数定义
    @Override
    public LLVMValueRef visitFuncDef(SysYParser.FuncDefContext ctx) {
        //生成返回值类型
        LLVMTypeRef returnType;
        String _return;
        if (ctx.funcType().INT() != null){
            returnType = i32Type;
            _return = "int";
        } else {
            returnType = voidType;
            _return = "void";
        }
        //生成函数类型
        LLVMTypeRef ft ;
        if (ctx.funcFParams() == null) {
            ft = LLVMFunctionType(returnType,i32Type,0,0);
        }
        else {
            SysYParser.FuncFParamsContext funcFParamsContext = ctx.funcFParams();
            int funcParamsNumber = funcFParamsContext.funcFParam().size();
            // 生成并增加参数
            PointerPointer<Pointer> paramsTypes = new PointerPointer<>(funcParamsNumber);
            for (int i=0;i<funcParamsNumber;i++){
                paramsTypes.put(i, i32Type);
            }
            // 生成函数类型
            ft = LLVMFunctionType(returnType,paramsTypes,funcParamsNumber,0);
        }

        //生成函数，即向之前创建的module中添加函数
        String functionName = ctx.IDENT().getText();
        LLVMValueRef function = LLVMAddFunction(module, functionName, ft);
        LLVMSetFunctionCallConv(function,LLVMCCallConv);
        // 当前作用域添加函数符号,切换作用域
        currentScope.define(functionName,function,_return);
        currentScope = new FunctionScope(functionName,currentScope);
        currentFunc = function;
        // 将参数添加至当前作用域
        if (ctx.funcFParams() != null){
            SysYParser.FuncFParamsContext funcFParamsContext = ctx.funcFParams();
            int funcParamsNumber = funcFParamsContext.funcFParam().size();
            for (int i=0;i<funcParamsNumber;i++){
                SysYParser.FuncFParamContext paramContext = funcFParamsContext.funcFParam(i);
                String paramName = paramContext.IDENT().getText();
                LLVMValueRef param = LLVMGetParam(function,i);
                currentScope.define(paramName, param,"int");
            }
        }
        //通过如下语句在函数中加入基本块
        String blockName = functionName + "Entry";
        LLVMBasicBlockRef basicBlock = LLVMAppendBasicBlock(function, "FunctionBlock: " + blockName);
        LLVMPositionBuilderAtEnd(builder, basicBlock);
        return super.visitFuncDef(ctx);
    }
    // 函数参数定义
    @Override
    public LLVMValueRef visitFuncFParam(SysYParser.FuncFParamContext ctx) {
        String name = ctx.IDENT().getText();
        LLVMValueRef valueRef = currentScope.resolve(name);
        LLVMValueRef valueRef1 = LLVMBuildAlloca(builder,i32Type,"param: " + name);
        LLVMBuildStore(builder,valueRef,valueRef1);
        currentScope.define(name,valueRef1,"int");
        return valueRef1;
    }
    //变量声明

    @Override
    public LLVMValueRef visitVarDecl(SysYParser.VarDeclContext ctx) {
        List<SysYParser.VarDefContext> varDefContexts = ctx.varDef();
        if (currentScope==globalScope){
            // 全局变量
            for (SysYParser.VarDefContext var : varDefContexts) {
                String varName = var.IDENT().getText();
                LLVMValueRef valueRef;

                int arrayLen = 0;
                // int变量
                valueRef = LLVMAddGlobal(module,i32Type,"globalVar: " + varName);

                if (var.initVal() != null){
                    LLVMValueRef globalVarValueInit = visit(var.initVal().exp());
                    LLVMSetInitializer(valueRef,globalVarValueInit);
                }
                // 无显示初始化，初始化为0
                else {
                    LLVMSetInitializer(valueRef, zero);

                }
                currentScope.define(varName, valueRef, "global int");
            }
        }
        else {
            for (SysYParser.VarDefContext var : varDefContexts) {
                String varName = var.IDENT().getText();
                LLVMValueRef valueRef;

                int arrayLen = 0;
                valueRef = LLVMBuildAlloca(builder, i32Type, "var: " + varName);

                // 有初值
                if (var.initVal() != null) {
                    LLVMValueRef init = visit(var.initVal().exp());
                    LLVMBuildStore(builder, init, valueRef);
                }
                currentScope.define(varName, valueRef, "int");
            }
        }
        return null;
    }

    // 常量声明

    @Override
    public LLVMValueRef visitConstDecl(SysYParser.ConstDeclContext ctx) {
        if (currentScope==globalScope){
            List<SysYParser.ConstDefContext> constDefContexts = ctx.constDef();
            for (SysYParser.ConstDefContext cons : constDefContexts) {
                String consName = cons.IDENT().getText();
                LLVMValueRef valueRef;

                int arrayLen = 0;
                valueRef = LLVMAddGlobal(module,i32Type,"globalConstVar: " + consName);

                LLVMValueRef globalVarValueInit = visit(cons.constInitVal().constExp());
                LLVMSetInitializer(valueRef,globalVarValueInit);
                currentScope.define(consName, valueRef, "global int");
            }
        }
        else {
            List<SysYParser.ConstDefContext> constDefContexts = ctx.constDef();
            for (SysYParser.ConstDefContext cons : constDefContexts) {
                String consName = cons.IDENT().getText();
                LLVMValueRef valueRef;

                int arrayLen = 0;
                valueRef = LLVMBuildAlloca(builder, i32Type, "cons: " + consName);
                // 一定有初值
                LLVMValueRef init = visit(cons.constInitVal().constExp());
                LLVMBuildStore(builder, init, valueRef);
                currentScope.define(consName, valueRef, "int");
            }
        }
        return null;
    }



    @Override
    public LLVMValueRef visitReturn(SysYParser.ReturnContext ctx) {
        returnFlag = true;
        // void
        if (ctx.exp() == null) {
            LLVMBuildRetVoid(builder);
            return null;
        }
        // int
        else {
            // 获得返回值
            LLVMValueRef result = visit(ctx.exp());
            // 生成函数返回指令
            LLVMBuildRet(builder, /*result:LLVMValueRef*/result);
            return result;
        }
    }

    @Override
    public LLVMValueRef visitFunCall(SysYParser.FunCallContext ctx) {
        String funcName = ctx.IDENT().getText();
        LLVMValueRef valueRef = null;
        // 获取函数符号
        LLVMValueRef function = globalScope.resolve(funcName);
        String funcType = globalScope.getSymbolType(funcName);
        // 无传参的情况
        if (ctx.funcRParams() == null) {
            // 直接调用
            if (funcType.equals("void")) {
                LLVMBuildCall(builder, function, null, 0, "");
            } else {
                valueRef = LLVMBuildCall(builder, function, null, 0, "FuncCallRes");
            }
        }
        // 有传参的情况
        else {
            // 获取参数
            int paramsNum = ctx.funcRParams().param().size();
            PointerPointer<Pointer> params = new PointerPointer<>(paramsNum);
            SysYParser.FuncRParamsContext funcRParamsContext = ctx.funcRParams();
            List<SysYParser.ParamContext> paramContexts = funcRParamsContext.param();
            for (int i = 0; i < paramsNum; i++) {
                SysYParser.ParamContext param = paramContexts.get(i);
                LLVMValueRef paramValue = visit(param.exp());
                params.put(i, paramValue);
            }
            // 传参调用
            if (funcType.equals("void")) {
                LLVMBuildCall(builder, function, params, paramsNum, "");
            } else {
                valueRef = LLVMBuildCall(builder, function, params, paramsNum, "FuncCallRes");
            }
        }
        return valueRef;
    }

    // if 与 if-else
    @Override
    public LLVMValueRef visitIf(SysYParser.IfContext ctx) {
        // 确定条件表达式最终结果
        LLVMValueRef cond = visit(ctx.cond());
        cond = LLVMBuildICmp(builder,LLVMIntNE,cond,zero,"not zero");
        // 创建三个块并使用跳转指令
        LLVMBasicBlockRef ifTrue = LLVMAppendBasicBlock(currentFunc, "if true: ");
        LLVMBasicBlockRef ifFalse = LLVMAppendBasicBlock(currentFunc, "if false: ");
        LLVMBasicBlockRef ifComplete = LLVMAppendBasicBlock(currentFunc, "if complete");
        LLVMBuildCondBr(builder,cond,ifTrue,ifFalse);
        // 执行if
        LLVMPositionBuilderAtEnd(builder,ifTrue);
        visit(ctx.stmt(0));
        LLVMBuildBr(builder,ifComplete);
        LLVMPositionBuilderAtEnd(builder,ifFalse);
        // 若有else，执行else
        if (ctx.ELSE() != null){
            visit(ctx.stmt(1));
        }
        LLVMBuildBr(builder,ifComplete);

        LLVMPositionBuilderAtEnd(builder,ifComplete);
        return null;
    }
    // while
    @Override
    public LLVMValueRef visitWhile(SysYParser.WhileContext ctx) {
        // while条件基本快
        LLVMBasicBlockRef whileCond = LLVMAppendBasicBlock(currentFunc, "while cond: ");
        LLVMBuildBr(builder, whileCond);
        LLVMPositionBuilderAtEnd(builder, whileCond);
        LLVMValueRef cond = visit(ctx.cond());
        cond = LLVMBuildICmp(builder, LLVMIntNE, cond, zero, "not zero");
        // 创建两个块并使用条件跳转指令
        LLVMBasicBlockRef whileStart = LLVMAppendBasicBlock(currentFunc, "while start: ");
        LLVMBasicBlockRef whileComplete = LLVMAppendBasicBlock(currentFunc, "while complete");
        LLVMBuildCondBr(builder, cond, whileStart, whileComplete);
        // 循环执行while子句
        whileBlock = new WhileBlock(whileCond, whileComplete, whileBlock);
        LLVMPositionBuilderAtEnd(builder, whileStart);
        visit(ctx.stmt());
        LLVMBuildBr(builder, whileCond);
        // 结束while循环
        LLVMPositionBuilderAtEnd(builder, whileComplete);
        whileBlock = whileBlock.getEnclosingWhileBlock();
        return null;
    }
    // break
    @Override
    public LLVMValueRef visitBreak(SysYParser.BreakContext ctx) {
        LLVMBuildBr(builder, whileBlock.getWhileComplete());
        return null;
    }
    // continue
    @Override
    public LLVMValueRef visitContinue(SysYParser.ContinueContext ctx) {
        LLVMBuildBr(builder, whileBlock.getWhileCond());
        return null;
    }

    @Override
    public LLVMValueRef visitAssign(SysYParser.AssignContext ctx) {
        assign = true;
        LLVMValueRef left = visit(ctx.lVal());
        assign = false;
        LLVMValueRef right = visit(ctx.exp());
        LLVMBuildStore(builder, right, left);
        return left;
    }

    @Override
    public LLVMValueRef visitRecurse(SysYParser.RecurseContext ctx) {
        return visit(ctx.exp());
    }

    @Override
    public LLVMValueRef visitLeftValue(SysYParser.LeftValueContext ctx) {
        return visit(ctx.lVal());
    }

    @Override
    public LLVMValueRef visitIsNum(SysYParser.IsNumContext ctx) {
        return visit(ctx.number());
    }

    // ||
    @Override
    public LLVMValueRef visitOr(SysYParser.OrContext ctx) {
        LLVMValueRef left = visit(ctx.cond(0));
        LLVMValueRef right = visit(ctx.cond(1));

        left = LLVMBuildICmp(builder,LLVMIntNE,left,zero,"not zero");
        right = LLVMBuildICmp(builder,LLVMIntNE,right,zero,"not zero");
        LLVMValueRef cond = LLVMBuildOr(builder,left,right,"cond: ||");
        cond = LLVMBuildZExt(builder,cond,i32Type,"to i32");
        return cond;
    }
    // &&
    @Override
    public LLVMValueRef visitAnd(SysYParser.AndContext ctx) {
        LLVMValueRef left = visit(ctx.cond(0));
        LLVMValueRef right = visit(ctx.cond(1));

        left = LLVMBuildICmp(builder,LLVMIntNE,left,zero,"not zero");
        right = LLVMBuildICmp(builder,LLVMIntNE,right,zero,"not zero");
        LLVMValueRef cond = LLVMBuildAnd(builder,left,right,"cond: &&");
        cond = LLVMBuildZExt(builder,cond,i32Type,"to i32");
        return cond;
    }

    // < > <= >=
    @Override
    public LLVMValueRef visitLt(SysYParser.LtContext ctx) {
        LLVMValueRef left = visit(ctx.cond(0));
        LLVMValueRef right = visit(ctx.cond(1));
        LLVMValueRef cond;
        // <
        if (ctx.LT() != null) {
            cond = LLVMBuildICmp(builder, LLVMIntSLT, left, right, "cond: <");
        }
        // <=
        else if (ctx.LE() != null) {
            cond = LLVMBuildICmp(builder, LLVMIntSLE, left, right, "cond: <=");
        }
        // >
        else if (ctx.GT() != null) {
            cond = LLVMBuildICmp(builder, LLVMIntSGT, left, right, "cond: >");
        }
        // >=
        else {
            cond = LLVMBuildICmp(builder, LLVMIntSGE, left, right, "cond: >=");
        }
        return LLVMBuildZExt(builder, cond, i32Type, "to i32");
    }
    // == !=
    @Override
    public LLVMValueRef visitEq(SysYParser.EqContext ctx) {
        LLVMValueRef left = visit(ctx.cond(0));
        LLVMValueRef right = visit(ctx.cond(1));
        LLVMValueRef condition;
        // ==
        if (ctx.EQ() != null) {
            condition = LLVMBuildICmp(builder, LLVMIntEQ, left, right, "cond: ==");
        }
        // !=
        else {
            condition = LLVMBuildICmp(builder, LLVMIntNE, left, right, "cond: !=");
        }
        return LLVMBuildZExt(builder, condition, i32Type, "to i32");
    }


    //二目运算符： + -
    @Override
    public LLVMValueRef visitPlus(SysYParser.PlusContext ctx) {
        LLVMValueRef l = visit(ctx.exp(0));
        LLVMValueRef r = visit(ctx.exp(1));
        LLVMValueRef result;
        // +
        if (ctx.PLUS() != null){
            result = LLVMBuildAdd(builder,l,r,"addRes");
        }
        // -
        else {
            result = LLVMBuildSub(builder,l,r,"subRes");
        }
        return result;
    }

    // 二目运算符： * / %
    @Override
    public LLVMValueRef visitMul(SysYParser.MulContext ctx) {
        LLVMValueRef l = visit(ctx.exp(0));
        LLVMValueRef r = visit(ctx.exp(1));
        LLVMValueRef result;
        if (ctx.MUL() != null){
            result = LLVMBuildMul(builder,l,r,"mulRes");
        }
        else if (ctx.DIV() != null){
            result = LLVMBuildSDiv(builder,l,r,"divRes");
        }
        else{
            result = LLVMBuildSRem(builder,l,r,"remRes");
        }
        return result;
    }
    // 单目运算符：！ - +
    @Override
    public LLVMValueRef visitUnaryOperator(SysYParser.UnaryOperatorContext ctx) {
        LLVMValueRef value = visit(ctx.exp());
        if (ctx.unaryOp().NOT() != null){
            value = LLVMBuildICmp(builder, LLVMIntNE, value, zero, "cond: value != 0");
            value = LLVMBuildXor(builder,value,trueVal,"xor: value ^ true");
            value = LLVMBuildZExt(builder,value,i32Type,"extension: int1 -> int32");
        }
        else if (ctx.unaryOp().MINUS() != null){
            value = LLVMBuildSub(builder,zero,value,"sub: 0 - value");
        }
        return value;
    }

    @Override
    public LLVMValueRef visitIsExp(SysYParser.IsExpContext ctx) {
        return visit(ctx.exp());
    }

    @Override
    public LLVMValueRef visitNumber(SysYParser.NumberContext ctx) {
        String value = ctx.INTEGR_CONST().getText();
        // 十六进制与八进制转成十进制
        if (value.startsWith("0X") || value.startsWith("0x")) {
            value = String.valueOf(Integer.parseInt(value.substring(2), 16));
        } else if (value.startsWith("0") && value.length() > 1) {
            value = String.valueOf(Integer.parseInt(value.substring(1), 8));
        }
        return LLVMConstInt(i32Type, Integer.parseInt(value), 0);
    }

    @Override
    public LLVMValueRef visitLVal(SysYParser.LValContext ctx) {
        String name = ctx.IDENT().getText();
        LLVMValueRef valueRef = currentScope.resolve(name);
        if(assign) return valueRef;
        valueRef = LLVMBuildLoad(builder,valueRef,"var: " + name);
        return valueRef;
    }

    public void console(){
        LLVMDumpModule(module);
    }

    public void save(String path){
        BytePointer error = new BytePointer();
        LLVMPrintModuleToFile(module,path,error);
    }
}
