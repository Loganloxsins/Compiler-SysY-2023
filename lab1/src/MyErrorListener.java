import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

public class MyErrorListener extends BaseErrorListener {
    /*
    结果这是原始代码，也是只输出报错信息
       public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        System.err.println("line " + line + ":" + charPositionInLine + " " + msg);
    }
     */

    private boolean isError=false;
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        isError=true;
        char t=msg.charAt(msg.length()-2);
        //System.err.println(msg);
        System.err.printf("Error type A at Line %d: Mysterious character \"%c\".\n",line,t);
        //System.out.println();


    }

    public boolean isError() {
        return isError;
    }
}
