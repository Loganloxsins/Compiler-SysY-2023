
import org.bytedeco.llvm.LLVM.LLVMValueRef;
import org.bytedeco.llvm.global.LLVM;

import java.util.LinkedHashMap;
import java.util.Map;

public class BaseScope implements Scope {

    private final Scope enclosingScope;

    private final Map<String, LLVMValueRef> symbols = new LinkedHashMap<String, LLVMValueRef>();

    private final Map<String, String> symbolTypes = new LinkedHashMap<>();

    private String name;
    public BaseScope(String name, Scope enclosingScope) {
        this.name = name;
        this.enclosingScope = enclosingScope;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Map<String, LLVMValueRef> getSymbols() {
        return symbols;
    }

    @Override
    public Scope getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public void define(String name, LLVMValueRef symbol, String type) {
        symbols.put(name,symbol);
        symbolTypes.put(name,type);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public LLVMValueRef resolve(String name) {
        LLVMValueRef symbol = symbols.get(name);
        if (symbol != null){
            return symbol;
        }
        if (enclosingScope != null){
            return enclosingScope.resolve(name);
        }
        return null;
    }

    @Override
    public String getSymbolType(String name) {
        String type = symbolTypes.get(name);
        if (type != null) {
            return type;
        }
        if (enclosingScope != null){
            return enclosingScope.getSymbolType(name);
        }
        return null;
    }
}
