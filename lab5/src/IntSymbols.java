import org.bytedeco.llvm.LLVM.*;

import java.util.LinkedHashMap;
import java.util.Map;

public class IntSymbols implements Symbols{
    public Map<String, LLVMValueRef> valueRefMap;

    public IntSymbols() {
        valueRefMap = new LinkedHashMap<>();
    }
    @Override
    public void define(String name, LLVMValueRef valueRef) {
        valueRefMap.put(name, valueRef);
    }



    @Override
    public Map<String, LLVMValueRef> getAllSymbols() {
        return valueRefMap;
    }

    @Override
    public boolean isDefinedSymbol(String name) {
        return valueRefMap.containsKey(name);
    }
}
