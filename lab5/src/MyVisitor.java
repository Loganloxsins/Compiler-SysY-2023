import org.bytedeco.llvm.LLVM.*;

import static org.bytedeco.llvm.global.LLVM.*;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;

import java.util.List;

public class MyVisitor extends SysYParserBaseVisitor<LLVMValueRef> {
    private GlobalScope globalScope;

    private Scope currentScope;
    //你可以将手册中的module与builder与i32Type作为你的Visitor的成员变量
    private LLVMModuleRef module;
    private LLVMBuilderRef builder;
    private LLVMTypeRef i32Type;
    private LLVMValueRef zero;
    private LLVMValueRef globalVar;
    public String llPath;

    //输出到文件时用
    public static final BytePointer error = new BytePointer();


    public MyVisitor() {
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
        //创建一个常量,这里是常数0
        this.zero = LLVMConstInt(i32Type, 0, /* signExtend */ 0);
    }

    /*
    调用流程我们很清楚，先Program，再CompUnit，再rerurn，再exp----------------------------------------------------
     */
    @Override
    public LLVMValueRef visitProgram(SysYParser.ProgramContext ctx) {
        globalScope = new GlobalScope("", null);
        currentScope = globalScope;
        //所有起点，因为main，只执行一次
        // this.visitCompUnit(ctx.compUnit());
        super.visitProgram(ctx);
        //输出到文件
        if (LLVMPrintModuleToFile(module, llPath, error) != 0) {
            LLVMDisposeMessage(error);
        }
        currentScope = currentScope.getEnclosingScope();
        return null;
    }

    @Override
    public LLVMValueRef visitFuncDef(SysYParser.FuncDefContext ctx) {
        if ("main".equals(ctx.IDENT().getText())) {
            currentScope = new BaseScope("main", currentScope);
            //要分析的main没有参数
            PointerPointer<Pointer> argumentTypes = new PointerPointer<>(0);
            //生成返回值类型
            LLVMTypeRef returnType = i32Type;
            //生成函数类型
            LLVMTypeRef ft = LLVMFunctionType(i32Type, argumentTypes, 0, 0);
            //生成函数，即向之前创建的module中添加函数
            LLVMValueRef function_main = LLVMAddFunction(module, "main", ft);
            //生成函数的入口基本块，对于这道题这也是唯一的块
            LLVMBasicBlockRef block_entry = LLVMAppendBasicBlock(function_main, "mainEntry");//曹原来命名有要求
            LLVMPositionBuilderAtEnd(builder, block_entry);
        } else {
            System.err.println("不是main函数");
        }
        return super.visitFuncDef(ctx);
    }

    @Override
    public LLVMValueRef visitReturnStmt(SysYParser.ReturnStmtContext ctx) {
        if (ctx.exp() != null) {
            LLVMValueRef ret = this.visit(ctx.exp());
            LLVMBuildRet(builder, ret);
            return ret;
        }
        return super.visitReturnStmt(ctx);
    }

    //-----------------------------------------------------------------------------------------------------------------------

    @Override
    public LLVMValueRef visitVarDecl(SysYParser.VarDeclContext ctx) {
        return super.visitVarDecl(ctx);

    }

    @Override
    public LLVMValueRef visitVarDef(SysYParser.VarDefContext ctx) {
        if (currentScope == globalScope) {
            //全局变量
            String varName = ctx.IDENT().getText();
            LLVMValueRef globalVar;
            SysYParser.InitValContext initValCtx = ctx.initVal();
            globalVar = LLVMAddGlobal(module, i32Type, varName);
            LLVMValueRef right = null;
            if (initValCtx != null) {
                right = this.visit(initValCtx);
            }
            LLVMSetInitializer(globalVar, right);
            currentScope.getIntSymbols().define(ctx.IDENT().getText(), globalVar);
            return super.visitVarDef(ctx);
        } else {
            String varName = ctx.IDENT().getText();
          //  System.out.println(varName);
            LLVMValueRef varPointer;
            SysYParser.InitValContext initValCtx = ctx.initVal();
            LLVMValueRef ref = LLVMBuildAlloca(builder, i32Type, ctx.IDENT().getText());
            if (initValCtx != null) {
                LLVMValueRef initValueRef = this.visit(initValCtx);
                LLVMBuildStore(builder, initValueRef, ref);// ref是左边的
            }
            currentScope.getIntSymbols().define(ctx.IDENT().getText(), ref);
            return super.visitVarDef(ctx);
        }
    }

    @Override
    public LLVMValueRef visitConstDecl(SysYParser.ConstDeclContext ctx) {
        return super.visitConstDecl(ctx);
    }

    @Override
    public LLVMValueRef visitConstDef(SysYParser.ConstDefContext ctx) {
        if (currentScope == globalScope) {
            //全局变量
            String varName = ctx.IDENT().getText();
            LLVMValueRef globalVar;
            SysYParser.ConstInitValContext initValCtx = ctx.constInitVal();
            globalVar = LLVMAddGlobal(module, i32Type, varName);
            LLVMValueRef right = null;
            if (initValCtx != null) {
                right = this.visit(initValCtx);
            }
            LLVMSetInitializer(globalVar, right);
            currentScope.getIntSymbols().define(ctx.IDENT().getText(), globalVar);
            currentScope.getConstSymbols().define(ctx.IDENT().getText(), (int) LLVMConstIntGetSExtValue(right));

        } else {
            LLVMValueRef ref = LLVMBuildAlloca(builder, i32Type, ctx.IDENT().getText());
            LLVMValueRef initValueRef = this.visit(ctx.constInitVal());    //一定有初始化的过程
            LLVMBuildStore(builder, initValueRef, ref);       // ref是左边的
            currentScope.getIntSymbols().define(ctx.IDENT().getText(), ref);
            currentScope.getConstSymbols().define(ctx.IDENT().getText(), (int) LLVMConstIntGetSExtValue(initValueRef));

        }
        return super.visitConstDef(ctx);
    }

    //---------------------------------------------------------------------------
    @Override
    public LLVMValueRef visitAssignStmt(SysYParser.AssignStmtContext ctx) {
        LLVMValueRef rval = this.visit(ctx.exp());
        LLVMValueRef lval;
        String token = ctx.lVal().IDENT().getText();

        lval = currentScope.resolveInt(token);

        LLVMBuildStore(builder, rval, lval);          //
        return null;
    }

    @Override
    public LLVMValueRef visitLvalExp(SysYParser.LvalExpContext ctx) {
        String token = ctx.lVal().IDENT().getText();

//        if (currentScope.getBaseSymbols().isDefinedSymbol(token)) {
//            return LLVMConstInt(i32Type, currentScope.getBaseSymbols().getConst(token), 0);
//        }
        return LLVMBuildLoad(builder, currentScope.resolveInt(token), token);
    }

    // 定义：unaryOp exp   #UnaryOpExp
//    @Override
//    public LLVMValueRef visitUnaryOpExp(SysYParser.UnaryOpExpContext ctx) {
//        String operator = ctx.unaryOp().getText();
//        LLVMValueRef expValue = visit(ctx.exp());
//        switch (operator) {
//            case "+": {
//                return expValue;
//            }
//            case "-": {
//                return LLVMBuildNeg(builder, expValue, "tmp_");
//            }
//            case "!": {
//                long numValue = LLVMConstIntGetZExtValue(expValue);
//                if (numValue == 0) {
//                    return LLVMConstInt(i32Type, 1, 1);
//                } else {
//                    return LLVMConstInt(i32Type, 0, 1);
//                }
//            }
//            default: {
//                System.err.println("unaryOp error:" + operator + "是未定义的单目运算符");
//            }
//        }
//        return super.visitUnaryOpExp(ctx);
//    }
    @Override
    public LLVMValueRef visitUnaryOpExp(SysYParser.UnaryOpExpContext ctx) {
        String operator = ctx.unaryOp().getText();
        LLVMValueRef expValue = visit(ctx.exp());
        switch (operator) {
            case "+":
                return expValue;
            case "-":
                return LLVMBuildNeg(builder, expValue, "tmp_");
            case "!":
                LLVMTypeRef boolType = LLVMInt1Type();
                LLVMValueRef zero = LLVMConstInt(boolType, 0, 1);
                LLVMValueRef cmp = LLVMBuildICmp(builder, LLVMIntEQ, expValue, zero, "tmp_");
                return LLVMBuildZExt(builder, cmp, i32Type, "tmp_");
            default:
                System.err.println("unaryOp error: " + operator + " 是未定义的单目运算符");
                return super.visitUnaryOpExp(ctx);
        }
    }

    /*
      | exp (MUL | DIV | MOD) exp 		    #MulExp
   | exp (PLUS | MINUS) exp 		    #PlusExp
     */
    @Override
    public LLVMValueRef visitPlusExp(SysYParser.PlusExpContext ctx) {
        List<SysYParser.ExpContext> exps = ctx.exp();
        LLVMValueRef lhs = this.visit(exps.get(0));
        LLVMValueRef rhs = this.visit(exps.get(1));
        LLVMValueRef ret;
        if (ctx.PLUS() != null)
            ret = LLVMBuildAdd(builder, lhs, rhs, "");
        else
            ret = LLVMBuildSub(builder, lhs, rhs, "");
        return ret;
    }

    @Override
    public LLVMValueRef visitMulExp(SysYParser.MulExpContext ctx) {
        LLVMValueRef op1 = visit(ctx.exp(0));
        LLVMValueRef op2 = visit(ctx.exp(1));

        if (ctx.MOD() != null) {
            return LLVMBuildSRem(builder, op1, op2, "114514");
        } else if (ctx.MUL() != null) {
            return LLVMBuildMul(builder, op1, op2, "222");
        } else if (ctx.DIV() != null) {
            return LLVMBuildSDiv(builder, op1, op2, "333");
        }

        return null;
    }



    //----------------------------------------
//这是对所有数字的工具访问函数
    @Override
    public LLVMValueRef visitNumber(SysYParser.NumberContext ctx) {
        int num = baseTrans(ctx.getText());
        LLVMValueRef numRef = LLVMConstInt(i32Type, num, 0);
        return numRef;
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

    @Override
    public LLVMValueRef visitParenExp(SysYParser.ParenExpContext ctx) {
        return this.visit(ctx.exp());
    }
}