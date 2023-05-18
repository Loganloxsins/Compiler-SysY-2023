import org.antlr.v4.runtime.tree.ParseTree;
import org.bytedeco.llvm.LLVM.*;

import static org.bytedeco.llvm.global.LLVM.*;

import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;

import java.util.List;

import static org.bytedeco.llvm.global.LLVM.*;

public class MyVisitor extends SysYParserBaseVisitor<LLVMValueRef> {
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
        //所有起点，因为main，只执行一次
        this.visitCompUnit(ctx.compUnit());
        //输出到文件
        if (LLVMPrintModuleToFile(module, llPath, error) != 0) {
            LLVMDisposeMessage(error);
        }
        return null;
    }

    @Override
    public LLVMValueRef visitFuncDef(SysYParser.FuncDefContext ctx) {
        if ("main".equals(ctx.IDENT().getText())) {
            //要分析的main没有参数
            PointerPointer<Pointer> argumentTypes =new PointerPointer<>(0);
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
        LLVMValueRef ret = this.visit(ctx.exp());
        LLVMBuildRet(builder, ret);
        return ret;
    }

    //-----------------------------------------------------------------------------------------------------------------------

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

    //---------------------------------------------------------------------------

    // 定义：unaryOp exp   #UnaryOpExp
    @Override
    public LLVMValueRef visitUnaryOpExp(SysYParser.UnaryOpExpContext ctx) {
        String operator = ctx.unaryOp().getText();
        LLVMValueRef expValue = visit(ctx.exp());
        switch (operator) {
            case "+": {
                return expValue;
            }
            case "-": {
                return LLVMBuildNeg(builder, expValue, "tmp_");
            }
            case "!": {
                long numValue = LLVMConstIntGetZExtValue(expValue);
                if (numValue == 0) {
                    return LLVMConstInt(i32Type, 1, 1);
                } else {
                    return LLVMConstInt(i32Type, 0, 1);
                }
            }
            default: {
                System.err.println("unaryOp error:" + operator+"是未定义的单目运算符");
            }
        }
        return super.visitUnaryOpExp(ctx);
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
            return LLVMBuildSRem(builder, op1, op2, "temp");
        } else if (ctx.MUL() != null) {
            return LLVMBuildMul(builder, op1, op2, "temp");
        } else if (ctx.DIV() != null) {
            return LLVMBuildSDiv(builder, op1, op2, "temp");
        }

        return null;
    }

}
