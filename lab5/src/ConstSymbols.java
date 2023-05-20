import org.bytedeco.llvm.LLVM.*;

import java.util.LinkedHashMap;
import java.util.Map;
public class ConstSymbols {

    public Map<String,Integer> constRefMap ;
    public ConstSymbols() {
        constRefMap = new LinkedHashMap<>();
    }


    public void define(String name, int i) {
        constRefMap.put(name, i);
    }

//    public int resolve(String name) {
//        return constRefMap.get(name);
//    }

    public Map<String, Integer> getAllSymbols() {
        return constRefMap;
    }

    public boolean isDefinedSymbol(String name) {
        return constRefMap.containsKey(name);
    }
}
