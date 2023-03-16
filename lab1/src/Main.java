import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/*
        //System.out.println(input);证明charstream就是charstream，是stream
        //A lexer is recognizer that draws input symbols from a character stream.
        // lexer是一个此法分析器类的实例，lexer是一个此法分析器，能拿在手里用的器

        //A token has properties: text, type, line, character position in the line (so we can ignore tabs), token channel, index, and source from which we obtained this token.

 */
public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("input path is required");
        }
        String source = args[0];
        CharStream input = CharStreams.fromFileName(source);
        SysYLexer lexer = new SysYLexer(input);


        MyErrorListener myErrorListener=new MyErrorListener();
        lexer.removeErrorListeners();
        lexer.addErrorListener(myErrorListener);

        //首先方法返回list，其次这个方法调用时会分析是否合法，如果合法把tokens存进list，不合法要我们自己实现输出lab中的不合法信息&&程停了
        //那么就要看它内部不合法时调了linster哪个函数去修改这个函数
        //找到在consoleerrorlistener中
        List<? extends Token> allTokens=lexer.getAllTokens();
        if(myErrorListener.isError()){
            return;
        }
        else {
            for (Token t:allTokens){
                String name=lexer.getVocabulary().getSymbolicName(t.getType());
                String eof="EOF";
                String text=t.getText();
                if (name!=eof){
                    if (text.charAt(0)=='0'&&text.length()>=2){
                        int num = (text.charAt(0)=='0'&&text.length()>=2 && (text.charAt(1)=='x'||text.charAt(1)=='X'))
                                ? Integer.parseInt(text.substring(2),16)
                                : Integer.parseInt(text,8);
                        System.err.printf("%s %d at Line %d.\n",name,num,t.getLine());
                    }else {
                        System.err.printf("%s %s at Line %d.\n",name,t.getText(),t.getLine());
                    }

                }
            }
        }



    }
}
/*
 //  lexer.getAllTokens().forEach(System.out::println);
课上讲的
CommonTokenStream tokens=new CommonTokenStream(lexer);
        tokens.fill();
        for (Token t:tokens.getTokens()){
            String name=lexer.getVocabulary().getSymbolicName(t.getType());
            String eof="EOF";
            if (name!=eof){
                System.out.printf("%s %s at Line %d.\n",name,t.getText(),t.getLine());
            }
        }
 */

//关于问题25的一个回答：https://chatgptproxy.me/index.html#/questionDetail?code=mFXPsYwrTUBwgWBeUsk5Ge4UVBRq0k4VyJNpih7_x4Bo1m0juoMlhl_WwlvDddfL