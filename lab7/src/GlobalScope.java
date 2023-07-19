
public class GlobalScope extends BaseScope {
    public GlobalScope(Scope enclosingScope) {
        super("GlobalScope", enclosingScope);
//        define(new BaseSymbol("int",new BasicType(SimpleType.INT)));
//        define(new BaseSymbol("void",new BasicType(SimpleType.VOID)));
//        define(new BaseSymbol("double",new BasicType(SimpleType.DOUBLE)));
    }

    @Override
    public String toString() {
        return "GlobalScope";
    }
}