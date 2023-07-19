
import org.bytedeco.llvm.LLVM.LLVMValueRef;

import java.util.Map;

public interface Scope {
    void setName(String name);

    Map<String, LLVMValueRef> getSymbols();

    Scope getEnclosingScope();

    void define(String name, LLVMValueRef symbol, String type);

    String getName();

    LLVMValueRef resolve(String name);

    String getSymbolType(String name);
}
