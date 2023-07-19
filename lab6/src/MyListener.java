import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.ArrayList;

public class MyListener extends BaseErrorListener {
    ArrayList<Integer> lineNums = new ArrayList<>();
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        // super.syntaxError(recognizer, offendingSymbol, line, charPositionInLine, msg, e);
        this.lineNums.add(line);
    }

    public ArrayList<Integer> getLineNums() {
        return this.lineNums;
    }
}
