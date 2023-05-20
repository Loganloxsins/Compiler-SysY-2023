import org.bytedeco.llvm.LLVM.*;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.bytedeco.llvm.global.LLVM.*;

public interface Symbols {

    void define(String name, LLVMValueRef valueRef);

   // LLVMValueRef resolve(String name);

    Map<String, LLVMValueRef> getAllSymbols();
    boolean isDefinedSymbol(String name);
}
