import org.bytedeco.llvm.LLVM.LLVMValueRef;

import java.util.LinkedHashMap;
import java.util.Map;

public class BaseScope implements Scope {

    String name;
    Scope enclosingScope;

    IntSymbols valueRefs;
    ConstSymbols constRefs;

    Map<String, Scope> subScopes = new LinkedHashMap<>();

    public BaseScope(String name, Scope enclosingScope) {
        this.name = name;
        this.enclosingScope = enclosingScope;
        this.valueRefs = new IntSymbols();
        this.constRefs = new ConstSymbols();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Scope getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public Scope getSubScope(String name) {
        return subScopes.get(name);
    }

    @Override
    public IntSymbols getIntSymbols() {
        return valueRefs;
    }

    @Override
    public ConstSymbols getConstSymbols() {
        return constRefs;
    }

    @Override
    //方桌圆桌台灯还要知道方圆属性，放一起是唯一办法
    public LLVMValueRef resolveInt(String name) {
        LLVMValueRef ret = valueRefs.valueRefMap.get(name);
        if (ret != null)
            return ret;
        if (enclosingScope != null)
            return enclosingScope.resolveInt(name);

        System.err.println("No such symbol: " + name);
        return null;

    }
    @Override
    public int resolveConst(String name) {
        if (constRefs.constRefMap.containsKey(name))
            return constRefs.constRefMap.get(name);
//        int ret = constRefs.constRefMap.get(name);
//        if (ret != null)
//            return ret;
        if (enclosingScope != null)
            return enclosingScope.resolveConst(name);

        System.err.println("No such symbol: " + name);
        return -1;

    }

}
