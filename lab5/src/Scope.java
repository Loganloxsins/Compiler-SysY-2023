import org.bytedeco.llvm.LLVM.LLVMValueRef;

public interface Scope {
    String getName();

    void setName(String name);

    Scope getEnclosingScope();

    Scope getSubScope(String name);
    IntSymbols getIntSymbols();
    ConstSymbols getConstSymbols();
    LLVMValueRef resolveInt(String name);

    int resolveConst(String name);

//    BaseSymbols createSymbols();    //this is valueRefs hashmap
//
//    //TODO: private Map<String,Integer> consts = new HashMap<>();
}