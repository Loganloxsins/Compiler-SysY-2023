
import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef;

public class WhileBlock {
    private final LLVMBasicBlockRef whileCond;
    private final LLVMBasicBlockRef whileComplete;
    private final WhileBlock enclosingWhileBlock;

    public WhileBlock(LLVMBasicBlockRef whileCond, LLVMBasicBlockRef whileComplete, WhileBlock enclosingWhileBlock) {
        this.whileCond = whileCond;
        this.whileComplete = whileComplete;
        this.enclosingWhileBlock = enclosingWhileBlock;
    }

    public LLVMBasicBlockRef getWhileCond() {
        return whileCond;
    }

    public LLVMBasicBlockRef getWhileComplete() {
        return whileComplete;
    }

    public WhileBlock getEnclosingWhileBlock() {
        return enclosingWhileBlock;
    }
}
