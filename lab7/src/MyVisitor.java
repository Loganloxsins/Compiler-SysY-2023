import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;

import static org.bytedeco.llvm.global.LLVM.*;

import java.util.HashMap;
import java.util.List;
import java.util.Stack;


public class MyVisitor extends SysYParserBaseVisitor<LLVMValueRef> {
    private final RuleNames ruleNames;
    private LLVMValueRef globalVar;
    public String llPath;
    private GlobalScope globalScope = null;

    private Scope currentScope = null;

    private LLVMModuleRef module;
    private LLVMBuilderRef builder;
    private LLVMTypeRef i32Type;
    LLVMTypeRef voidType;
    LLVMTypeRef i32ArrayType;
    LLVMValueRef[] constDigit;
    private boolean assign = false;

    private boolean returnFlag = false;

    private LLVMValueRef currentFunc = null;

    private WhileBlock whileBlock = null;
    // 空类型
    LLVMTypeRef i1Type;
    private int localScopeCounter = 0;
    LLVMValueRef zero;
    LLVMValueRef trueVal;
    LLVMValueRef falseVal;
    HashMap<LLVMValueRef, LLVMTypeRef> retTypes = new HashMap<>();
    HashMap<Integer, String> Kinds = new HashMap<>();
    String des;

    Stack<LLVMBasicBlockRef> whileConds = new Stack<>();

    Stack<LLVMBasicBlockRef> whileExits = new Stack<>();

    public static final BytePointer error = new BytePointer();


    public MyVisitor(RuleNames ruleNames) {

        //1. 初始化LLVM
        LLVMInitializeCore(LLVMGetGlobalPassRegistry());
        LLVMLinkInMCJIT();
        LLVMInitializeNativeAsmPrinter();
        LLVMInitializeNativeAsmParser();
        LLVMInitializeNativeTarget();

        //2. 创建module
        module = LLVMModuleCreateWithName("moudle");

        //3. 初始化IRBuilder，后续将使用这个builder去生成LLVM IR
        builder = LLVMCreateBuilder();

        //4. 考虑到我们的语言中仅存在int一个基本类型，可以通过下面的语句为LLVM的int型重命名方便以后使用
        i32Type = LLVMInt32Type();

        voidType = LLVMVoidType();

        i32ArrayType = LLVMPointerType(i32Type, 0);

        this.ruleNames = ruleNames;
        this.i1Type = LLVMInt1Type();
        this.zero = LLVMConstInt(i32Type, 0, 0);
        this.trueVal = LLVMConstInt(i1Type, 1, 0);
        this.falseVal = LLVMConstInt(i1Type, 0, 0);

        initSth();
    }

    private void initSth() {
        //1 init constDigit
        constDigit = new LLVMValueRef[10];
        for (int i = 0; i <= 9; i++) {
            constDigit[i] = LLVMConstInt(i32Type, i, 0);
        }

//        //2 init Kinds
//        Kinds.put(LLVMIntSLE, "LE");
//        Kinds.put(LLVMIntSLT, "LT");
//        Kinds.put(LLVMIntSGT, "GT");
//        Kinds.put(LLVMIntSGE, "GE");
//        Kinds.put(LLVMIntEQ, "EQ");
//        Kinds.put(LLVMIntNE, "NEQ");
//        Kinds.put(LLVMAnd, "AND");
//        Kinds.put(LLVMOr, "OR");
    }
    //-----------------------------------------------------------------------------------------------------------------------
    @Override
    public LLVMValueRef visitProgram(SysYParser.ProgramContext ctx) {
        globalScope = new GlobalScope(null);
        currentScope = globalScope;
        super.visitProgram(ctx);
        if (LLVMPrintModuleToFile(module, des, error) != 0) {    // moudle是你自定义的LLVMModuleRef对象
            LLVMDisposeMessage(error);
        }
        currentScope = currentScope.getEnclosingScope();
        return null;
    }
    private LLVMValueRef currentFunction = null;
    private boolean isReturned = false;
    @Override
    public LLVMValueRef visitFuncDef(SysYParser.FuncDefContext ctx) {
        //生成返回值类型 LLVMTypeRef i32ArrayType;
        LLVMTypeRef returnType;

        String _return;
        if (ctx.funcType().INT() != null) {
            returnType = i32Type;
            _return = "int";
        } else {
            returnType = voidType;
            _return = "void";
        }
        int paramsCount = 0;
        if (ctx.funcFParams() != null) {
            paramsCount = ctx.funcFParams().funcFParam().size();
        }
        SysYParser.FuncFParamsContext funcFParamsCtx = ctx.funcFParams();
        PointerPointer<Pointer> mainParamTypes;
        LLVMTypeRef funcType;
//        LLVMTypeRef retType = ctx.funcType().getText().equals("void") ? voidType : i32Type;
        LLVMTypeRef retType = getTypeRef(ctx.funcType().getText());

        if (funcFParamsCtx != null) {
            List<SysYParser.FuncFParamContext> funcFParamCtxs = funcFParamsCtx.funcFParam();
            mainParamTypes = new PointerPointer<>(funcFParamCtxs.size());
            for (int i = 0; i < funcFParamCtxs.size(); i++) {
                if (!funcFParamCtxs.get(i).L_BRACKT().isEmpty()) {
                    mainParamTypes = mainParamTypes.put(i, i32ArrayType);
                } else {
                    mainParamTypes = mainParamTypes.put(i, i32Type);
                }
            }
            funcType = LLVMFunctionType(retType, mainParamTypes, funcFParamCtxs.size(), 0);
        } else {
            mainParamTypes = new PointerPointer<>(0);
            funcType = LLVMFunctionType(retType, mainParamTypes, 0, 0);
        }

        LLVMValueRef curFunction = LLVMAddFunction(module, ctx.IDENT().getText(), funcType);
        LLVMBasicBlockRef curEntry = LLVMAppendBasicBlock(curFunction, ctx.IDENT().getText() + "Entry");
        LLVMPositionBuilderAtEnd(builder, curEntry);

        globalScope.define(ctx.IDENT().getText(), curFunction, "function");
        retTypes.put(curFunction, retType);

        currentScope = new FunctionScope(ctx.IDENT().getText(), currentScope);
        currentScope.setCurFunction(curFunction);// set current function

        if (funcFParamsCtx != null) {
            List<SysYParser.FuncFParamContext> funcFParamCtxs = funcFParamsCtx.funcFParam();
            for (int i = 0; i < funcFParamCtxs.size(); i++) {
                LLVMValueRef argI = LLVMGetParam(curFunction, i);
                String key = funcFParamCtxs.get(i).IDENT().getText();
                LLVMValueRef value;
                if (funcFParamCtxs.get(i).L_BRACKT().isEmpty()) {
                    value = LLVMBuildAlloca(builder, i32Type, /*pointerName:String*/key);
                } else {
                    value = LLVMBuildAlloca(builder, i32ArrayType, "i32Array");
                    currentScope.putPointer(key, true);
                }
                LLVMBuildStore(builder, argI, value);             //将数值存入该内存
                currentScope.define(key, value, "int");
            }
        }
        isReturned = false;
        super.visitFuncDef(ctx);                                    //this line

//        if (!currentScope.getIsBuildRet()) {
        if (ctx.funcType().getText().equals("void"))
            LLVMBuildRetVoid(builder);
        else
            LLVMBuildRet(builder, LLVMConstInt(i32Type, 0, 0));
//        }
        isReturned = false;
        currentScope = currentScope.getEnclosingScope();

        return null;
    }
    private LLVMTypeRef getTypeRef(String typeName) {
        if (typeName.equals("int")) {
            return i32Type;
        } else {
            return voidType;
        }
    }
    @Override
    public LLVMValueRef visitVarDecl(SysYParser.VarDeclContext ctx) {
        return super.visitVarDecl(ctx);
    }

    @Override
    public LLVMValueRef visitVarDef(SysYParser.VarDefContext ctx) {

        if (currentScope == globalScope) {
            if (ctx.L_BRACKT().isEmpty()) {
                LLVMValueRef globalInt = LLVMAddGlobal(module, i32Type, ctx.IDENT().getText());
                SysYParser.InitValContext initValCtx = ctx.initVal();
                LLVMValueRef right = LLVMConstInt(i32Type, 0, 0);
                if (initValCtx != null) right = this.visit(initValCtx);
                LLVMSetInitializer(globalInt, right);
                globalScope.define(ctx.IDENT().getText(), globalInt, "int");
            } else {
                LLVMValueRef visit = this.visit(ctx.constExp(0));
                int vecSize = (int) LLVMConstIntGetSExtValue(visit);
                LLVMTypeRef arrayType = LLVMArrayType(i32Type, vecSize);
                LLVMValueRef globalArray = LLVMAddGlobal(module, arrayType, ctx.IDENT().getText());  //left

                LLVMValueRef[] initVals = new LLVMValueRef[vecSize];
                int initSize = 0;
                if (ctx.initVal() != null) {
                    List<SysYParser.InitValContext> initValCtxs = ctx.initVal().initVal();
                    initSize = initValCtxs.size();
                    for (int j = 0; j < initSize; j++) initVals[j] = this.visit(initValCtxs.get(j));
                }
                for (int j = initSize; j < vecSize; j++) initVals[j] = LLVMConstInt(i32Type, 0, 0);
                PointerPointer<LLVMValueRef> pp = new PointerPointer<>(initVals);
                LLVMValueRef constArray = LLVMConstArray(i32Type, pp, vecSize);                        //right

                LLVMSetInitializer(globalArray, constArray);
                globalScope.putPointer(ctx.IDENT().getText(), false);
                globalScope.define(ctx.IDENT().getText(), globalArray, "array");
            }
            return super.visitVarDef(ctx);
        }

        if (ctx.L_BRACKT().isEmpty()) {   //one
            LLVMValueRef ref = LLVMBuildAlloca(builder, i32Type, ctx.IDENT().getText());
            SysYParser.InitValContext initValCtx = ctx.initVal();
            if (initValCtx != null) {
                LLVMValueRef initValueRef = this.visit(initValCtx);
                LLVMBuildStore(builder, initValueRef, ref);// ref是左边的
            }
            currentScope.define(ctx.IDENT().getText(), ref, "int");
        } else {          // array
            LLVMValueRef visit = this.visit(ctx.constExp(0));
            int vecSize = (int) LLVMConstIntGetSExtValue(visit);

            LLVMTypeRef arrayType = LLVMArrayType(i32Type, vecSize);
            LLVMValueRef arrayPointer = LLVMBuildAlloca(builder, arrayType, ctx.IDENT().getText());

            if (ctx.initVal() != null) {
                List<SysYParser.InitValContext> initValCtxs = ctx.initVal().initVal();
                int initSize = initValCtxs.size();
                LLVMValueRef[] initVals = new LLVMValueRef[vecSize];
                for (int j = 0; j < initSize; j++) initVals[j] = this.visit(initValCtxs.get(j));
                for (int j = initSize; j < vecSize; j++) initVals[j] = LLVMConstInt(i32Type, 0, 0);

                LLVMValueRef[] refs = new LLVMValueRef[2];
                refs[0] =LLVMConstInt(i32Type, 0, 0);

                for (int j = 0; j < vecSize; j++) {
                    refs[1] = LLVMConstInt(i32Type, j, 0);
                    PointerPointer<Pointer> pp = new PointerPointer<>(refs);
                    LLVMValueRef pointer = LLVMBuildGEP(builder, arrayPointer, pp, 2, "pointer");
                    LLVMBuildStore(builder, initVals[j], pointer);
                }
            }
            currentScope.define(ctx.IDENT().getText(), arrayPointer, "array");
            currentScope.putPointer(ctx.IDENT().getText(), false);         //不是指针，但是可以作为指针（作为实参）
        }
        return super.visitVarDef(ctx);
    }

    @Override
    public LLVMValueRef visitConstDef(SysYParser.ConstDefContext ctx) {
        if (currentScope == globalScope) {
            if (ctx.L_BRACKT().isEmpty()) {
                LLVMValueRef globalInt = LLVMAddGlobal(module, i32Type, ctx.IDENT().getText());
                SysYParser.ConstInitValContext constInitValCtx = ctx.constInitVal();
                LLVMValueRef right = LLVMConstInt(i32Type, 0, 0);
                if (constInitValCtx != null) right = this.visit(constInitValCtx);
                LLVMSetInitializer(globalInt, right);
                globalScope.define(ctx.IDENT().getText(), globalInt, "int");
                globalScope.putConst(ctx.IDENT().getText(), (int) LLVMConstIntGetSExtValue(right));
            } else {
                LLVMValueRef visit = this.visit(ctx.constExp(0));
                int vecSize = (int) LLVMConstIntGetSExtValue(visit);
                LLVMTypeRef arrayType = LLVMArrayType(i32Type, vecSize);
                LLVMValueRef globalVec = LLVMAddGlobal(module, arrayType, ctx.IDENT().getText());  //left

                LLVMValueRef[] initVals = new LLVMValueRef[vecSize];
                int initSize = 0;
                if (ctx.constInitVal() != null) {
                    List<SysYParser.ConstInitValContext> constInitValCtxs = ctx.constInitVal().constInitVal();
                    initSize = constInitValCtxs.size();
                    for (int j = 0; j < initSize; j++) initVals[j] = this.visit(constInitValCtxs.get(j));
                }
                for (int j = initSize; j < vecSize; j++) initVals[j] = constDigit[0];           // no init
                PointerPointer<LLVMValueRef> pp = new PointerPointer<>(initVals);
                LLVMValueRef constArray = LLVMConstArray(i32Type, pp, vecSize);                        //right

                LLVMSetInitializer(globalVec, constArray);

                globalScope.define(ctx.IDENT().getText(), globalVec, "array");
                globalScope.putPointer(ctx.IDENT().getText(), false);
            }
            return super.visitConstDef(ctx);
        }

        if (ctx.L_BRACKT().isEmpty()) {   //one
            LLVMValueRef ref = LLVMBuildAlloca(builder, i32Type, ctx.IDENT().getText());
            LLVMValueRef initValueRef = this.visit(ctx.constInitVal());    //一定有初始化的过程
            LLVMBuildStore(builder, initValueRef, ref);       // ref是左边的
            currentScope.define(ctx.IDENT().getText(), ref, "int");
            currentScope.putConst(ctx.IDENT().getText(), (int) LLVMConstIntGetSExtValue(initValueRef));
        } else {          // array
            LLVMValueRef visit = this.visit(ctx.constExp(0));
            int vecSize = (int) LLVMConstIntGetSExtValue(visit);

            LLVMTypeRef arrayType = LLVMArrayType(i32Type, vecSize);
            LLVMValueRef arrayPointer = LLVMBuildAlloca(builder, arrayType, ctx.IDENT().getText());

            List<SysYParser.ConstInitValContext> constInitValCtxs = ctx.constInitVal().constInitVal();
            int initSize = constInitValCtxs.size();
            LLVMValueRef[] initVals = new LLVMValueRef[vecSize];
            for (int j = 0; j < initSize; j++) initVals[j] = this.visit(constInitValCtxs.get(j));
            for (int j = initSize; j < vecSize; j++) initVals[j] = constDigit[0];

            LLVMValueRef[] refs = new LLVMValueRef[2];
            refs[0] = constDigit[0];

            for (int j = 0; j < vecSize; j++) {
                refs[1] = LLVMConstInt(i32Type, j, 0);
                PointerPointer<Pointer> pp = new PointerPointer<>(refs);
                LLVMValueRef pointer = LLVMBuildGEP(builder, arrayPointer, pp, 2, "pointer");
                LLVMBuildStore(builder, initVals[j], pointer);
            }

            currentScope.define(ctx.IDENT().getText(), arrayPointer, "array");
            currentScope.putPointer(ctx.IDENT().getText(), false);
        }
        return super.visitConstDef(ctx);
    }

    //--------------------------curBlock-------------------------------------------------



    @Override
    public LLVMValueRef visitReturnStmt(SysYParser.ReturnStmtContext ctx) {
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
    public LLVMValueRef visitCallFuncExp(SysYParser.CallFuncExpContext ctx) {
        LLVMValueRef funcRef = globalScope.resolve(ctx.IDENT().getText());
        //实参
        SysYParser.FuncRParamsContext funcRParamsCtx = ctx.funcRParams();
        PointerPointer<Pointer> arguments;
        LLVMValueRef retValueRef;
        String name = retTypes.get(funcRef).equals(voidType) ? "" : "name";

        if (funcRParamsCtx != null) {
            List<SysYParser.ParamContext> paramCtxs = funcRParamsCtx.param();       // 有可能是 empty()

            LLVMValueRef[] refs = new LLVMValueRef[paramCtxs.size()];

            for (int i = 0; i < paramCtxs.size(); i++) {
                String token = paramCtxs.get(i).getText();   //
                if (currentScope.getArrays(token)) {        //数组作为指针传进去, 如果是arr[2]传进去，那就是下面的visit
                    LLVMValueRef[] tmp = new LLVMValueRef[2];
                    tmp[0] = constDigit[0];
                    tmp[1] = constDigit[0];
                    PointerPointer<LLVMValueRef> pp = new PointerPointer<>(tmp);
                    LLVMValueRef arr = LLVMBuildGEP(builder, currentScope.resolve(token), pp, 2, "arr");// 数组作为指针传进去
                    refs[i] = arr;
//                    System.out.println("call array");
                } else if (currentScope.getPointer(token)) {
                    LLVMValueRef arrayPointer = currentScope.resolve(token);
                    LLVMValueRef arrPtr = LLVMBuildLoad(builder, arrayPointer, "arrPtr");
                    refs[i] = arrPtr;
//                    System.out.println("call pointer");
                } else {
                    refs[i] = this.visit(paramCtxs.get(i));
                }
            }
            arguments = new PointerPointer<>(refs);

            retValueRef = LLVMBuildCall(builder, funcRef, arguments, paramCtxs.size(), name);
        } else {
            arguments = new PointerPointer<>(0);
            retValueRef = LLVMBuildCall(builder, funcRef, arguments, 0, name);
        }

        return retValueRef;
    }

    @Override
    public LLVMValueRef visitIfStmt(SysYParser.IfStmtContext ctx) {

        LLVMValueRef ret = this.visit(ctx.cond());
        LLVMValueRef If = LLVMBuildICmp(builder, LLVMIntNE, ret, LLVMConstInt(i32Type, 0, 0), "icmp");    // 最后统一判断

        LLVMBasicBlockRef IfBody = LLVMAppendBasicBlock(currentScope.getCurFunction(), "IfBody");
        LLVMBasicBlockRef ELSE = LLVMAppendBasicBlock(currentScope.getCurFunction(), "Else");
        LLVMBasicBlockRef IfOut = LLVMAppendBasicBlock(currentScope.getCurFunction(), "IfOut");
        LLVMBuildCondBr(builder, If, IfBody, ELSE);

//        currentScope = new LocalScope(currentScope);
        LLVMPositionBuilderAtEnd(builder, IfBody);
        this.visit(ctx.stmt(0));
        LLVMBuildBr(builder, IfOut);

        LLVMPositionBuilderAtEnd(builder, ELSE);
        if (ctx.stmt(1) != null) this.visit(ctx.stmt(1));
        LLVMBuildBr(builder, IfOut);

        LLVMPositionBuilderAtEnd(builder, IfOut);

//        currentScope = currentScope.getEnclosingScope();
        return null;
    }

    @Override
    public LLVMValueRef visitBreakStmt(SysYParser.BreakStmtContext ctx) {
        LLVMBasicBlockRef exit = whileExits.peek();
        LLVMBuildBr(builder, exit);
        return super.visitBreakStmt(ctx);
    }
    @Override
    public LLVMValueRef visitContinueStmt(SysYParser.ContinueStmtContext ctx) {
        LLVMBasicBlockRef cond = whileConds.peek();
        LLVMBuildBr(builder, cond);
        return super.visitContinueStmt(ctx);
    }

    @Override
    public LLVMValueRef visitLtCond(SysYParser.LtCondContext ctx) {
        System.out.println("visitLtCond");
        int op = -1;
        if (ctx.LT() != null) {
            op = LLVMIntSLT;
        } else if (ctx.GT() != null) {
            op = LLVMIntSGT;
        } else if (ctx.LE() != null) {
            op = LLVMIntSLE;
        } else if (ctx.GE() != null) {
            op = LLVMIntSGE;
        }

        LLVMValueRef condition = LLVMBuildICmp(builder, op, visit(ctx.cond(0)), visit(ctx.cond(1)), "temp");//这行又报错

        LLVMValueRef zext = LLVMBuildZExt(builder, condition, i32Type, "compare");

        return zext;
    }

    @Override
    public LLVMValueRef visitEqCond(SysYParser.EqCondContext ctx) {
        int kind = -1;
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

    @Override
    public LLVMValueRef visitAndCond(SysYParser.AndCondContext ctx) {
        LLVMValueRef left = visit(ctx.cond(0));
        LLVMValueRef right = visit(ctx.cond(1));

        left = LLVMBuildICmp(builder, LLVMIntNE, left, zero, "not zero");
        right = LLVMBuildICmp(builder, LLVMIntNE, right, zero, "not zero");
        LLVMValueRef cond = LLVMBuildAnd(builder, left, right, "cond: &&");
        cond = LLVMBuildZExt(builder, cond, i32Type, "to i32");
        return cond;
    }

    @Override
    public LLVMValueRef visitOrCond(SysYParser.OrCondContext ctx) {
        LLVMValueRef left = visit(ctx.cond(0));
        LLVMValueRef right = visit(ctx.cond(1));
        int kind = LLVMOr;


        left = LLVMBuildICmp(builder, LLVMIntNE, left, zero, "not zero");
        right = LLVMBuildICmp(builder, LLVMIntNE, right, zero, "not zero");
        LLVMValueRef cond = LLVMBuildOr(builder, left, right, "cond: ||");
        cond = LLVMBuildZExt(builder, cond, i32Type, "to i32");
        return cond;
    }

    @Override
    public LLVMValueRef visitExpCond(SysYParser.ExpCondContext ctx) {
        return super.visitExpCond(ctx);
    }

    // 右边是左值
    @Override
    public LLVMValueRef visitLvalExp(SysYParser.LvalExpContext ctx) {
        String token = ctx.lVal().IDENT().getText();
        if (!ctx.lVal().L_BRACKT().isEmpty()) {
            LLVMValueRef[] refs = new LLVMValueRef[2];
            LLVMValueRef arrayPointer = currentScope.resolve(token);
            LLVMValueRef ptr;
            if (currentScope.getPointer(token)) {     // 是个指针数组(形参)
                refs[0] = this.visit(ctx.lVal().exp(0));
                PointerPointer<Pointer> pp = new PointerPointer<>(refs);
                LLVMValueRef arr = LLVMBuildLoad(builder, arrayPointer, "arr");
                ptr = LLVMBuildGEP(builder, arr, pp, 1, "arr");   // 形参不能按此操作
//                System.out.println("LvalExp pointer");
            } else {          // 是个实体数组
                refs[0] = constDigit[0];
                refs[1] = this.visit(ctx.lVal().exp(0));
                PointerPointer<Pointer> pp = new PointerPointer<>(refs);
                ptr = LLVMBuildGEP(builder, arrayPointer, pp, 2, "arr");   // 形参不能按此操作
//                System.out.println("LvalExp array");
            }
            return LLVMBuildLoad(builder, ptr, token);
        } else {
            if (currentScope.getConst(token) != -1) {
                return LLVMConstInt(i32Type, currentScope.getConst(token), 0);
            }
            return LLVMBuildLoad(builder, currentScope.resolve(token), token);
        }
    }


    @Override
    public LLVMValueRef visitAssignStmt(SysYParser.AssignStmtContext ctx) {
        LLVMValueRef rval = this.visit(ctx.exp());
        LLVMValueRef lval;
        String token = ctx.lVal().IDENT().getText();
        if (!ctx.lVal().L_BRACKT().isEmpty()) {
            LLVMValueRef[] refs = new LLVMValueRef[2];
            LLVMValueRef arrayPointer = currentScope.resolve(token);
            if (currentScope.getPointer(token)) {
                refs[0] = this.visit(ctx.lVal().exp(0));
                PointerPointer<LLVMValueRef> pp = new PointerPointer<>(refs);
                LLVMValueRef arr = LLVMBuildLoad(builder, arrayPointer, "arr");
                lval = LLVMBuildGEP(builder, arr, pp, 1, "lval");
//                System.out.println("AssignStmt pointer");
            } else {
                refs[0] = constDigit[0];
                refs[1] = this.visit(ctx.lVal().exp(0));
                PointerPointer<Pointer> pp = new PointerPointer<>(refs);
                lval = LLVMBuildGEP(builder, arrayPointer, pp, 2, "lval");
//                System.out.println("AssignStmt array");
            }
        } else {
            lval = currentScope.resolve(token);
        }
        LLVMBuildStore(builder, rval, lval);          //
        return null;
    }

    @Override
    public LLVMValueRef visitPARENS(SysYParser.PARENSContext ctx) {
        return this.visit(ctx.exp());
    }

    @Override
    public LLVMValueRef visitMulExp(SysYParser.MulExpContext ctx) {
        LLVMValueRef l = visit(ctx.exp(0));
        LLVMValueRef r = visit(ctx.exp(1));
        LLVMValueRef result;
        if (ctx.MUL() != null) {
            result = LLVMBuildMul(builder, l, r, "mulRes");
        } else if (ctx.DIV() != null) {
            result = LLVMBuildSDiv(builder, l, r, "divRes");
        } else {
            result = LLVMBuildSRem(builder, l, r, "remRes");
        }
        return result;
    }

    @Override
    public LLVMValueRef visitPlusExp(SysYParser.PlusExpContext ctx) {
        List<SysYParser.ExpContext> exps = ctx.exp();
        LLVMValueRef lhs = this.visit(exps.get(0));
        LLVMValueRef rhs = this.visit(exps.get(1));
        LLVMValueRef ret;
        if (ctx.PLUS() != null)
            ret = LLVMBuildAdd(builder, lhs, rhs, "add");
        else
            ret = LLVMBuildSub(builder, lhs, rhs, "sub");
        return ret;
    }

    @Override
    public LLVMValueRef visitUnaryOpExp(SysYParser.UnaryOpExpContext ctx) {
        LLVMValueRef value = visit(ctx.exp());
        if (ctx.unaryOp().NOT() != null) {
            value = LLVMBuildICmp(builder, LLVMIntNE, value, zero, "cond: value != 0");
            value = LLVMBuildXor(builder, value, trueVal, "xor: value ^ true");
            value = LLVMBuildZExt(builder, value, i32Type, "extension: int1 -> int32");
        } else if (ctx.unaryOp().MINUS() != null) {
            value = LLVMBuildSub(builder, zero, value, "sub: 0 - value");
        }
        return value;
    }

    @Override
    public LLVMValueRef visitNumber(SysYParser.NumberContext ctx) {
        int num = baseTrans(ctx.getText());
        return LLVMConstInt(i32Type, num, 0);
    }

    @Override
    public LLVMValueRef visitNumberExp(SysYParser.NumberExpContext ctx) {
        return this.visitNumber(ctx.number());
    }

    private int baseTrans(String text) {
        if (text.charAt(0) == '0' && text.length() >= 2) {
            int i;
            if (text.charAt(1) == 'x' || text.charAt(1) == 'X') {
                i = Integer.parseInt(text.substring(2), 16);
            } else {
                i = Integer.parseInt(text, 8);
            }
            return i;
        } else {
            return Integer.parseInt(text);
        }
    }
    public void save(String path) {
        BytePointer error = new BytePointer();
        LLVMPrintModuleToFile(module, path, error);
    }
    public void console() {
        LLVMDumpModule(module);
    }

}
