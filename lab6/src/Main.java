import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.bytedeco.llvm.LLVM.LLVMValueRef;

import java.io.IOException;


public class Main{
    // 词法分析器
    private static SysYLexer sysYLexer;
    // 语法分析器
    private static SysYParser sysYParser;
    // 词法错误监听器
    private static MyListener myLexerListener;
    // 语法错误监听器
    private static MyListener myParserListener;

    public static void main(String[] args) throws IOException {
        // 读取文件，创建词法分析器
        if (args.length < 1) {
            System.err.println("input path is required");
        }
//        String s = args[0];
//        String s = "tests/test1.sysy";
//        lineNum = Integer.parseInt(args[1]);
////        column = Integer.parseInt(args[2]);
////        changeName = args[3];
//        String s = "tests/test1.sysy";
//        String t = "src/test.ll";
        String s = args[0];
        String t = args[1];
        CharStream input = CharStreams.fromFileName(s);
        sysYLexer = new SysYLexer(input);
        // 生成语法分析器
        CommonTokenStream tokens = new CommonTokenStream(sysYLexer);
        sysYParser = new SysYParser(tokens);
        // 添加listener
        myLexerListener = new MyListener();
        myParserListener = new MyListener();
        sysYLexer.removeErrorListeners();
        sysYParser.removeErrorListeners();
        sysYLexer.addErrorListener(myLexerListener);
        sysYParser.addErrorListener(myParserListener);

        ParseTree tree = sysYParser.program();
//        // 获取报错
//        ArrayList<Integer> lexerErrorLines = myLexerListener.getLineNums();
//        ArrayList<Integer> parserErrorLines = myParserListener.getLineNums();
//
//        if (!lexerErrorLines.isEmpty()){
//            for (Integer line :lexerErrorLines){
//                System.err.println("Error type A at Line "+ line + ": lexer error!");
//            }
//            return;
//        }
//        if (!parserErrorLines.isEmpty()){
//            for (Integer line :parserErrorLines){
//                System.err.println("Error type B at Line "+ line + ": parser error!");
//            }
//            return;
//        }
//
//        String[] lexerRuleNames = sysYLexer.getRuleNames();
//        String[] parserRuleNames = sysYParser.getRuleNames();
//        RuleNames ruleNames = new RuleNames(lexerRuleNames,parserRuleNames);
//        // 表驱动，输出颜色
//        Map<String, String> colorsMap = new LinkedHashMap<>();
//        colorsMap.put("CONST", "orange");
//        colorsMap.put("INT", "orange");
//        colorsMap.put("VOID", "orange");
//        colorsMap.put("IF", "orange");
//        colorsMap.put("ELSE", "orange");
//        colorsMap.put("WHILE", "orange");
//        colorsMap.put("BREAK", "orange");
//        colorsMap.put("CONTINUE", "orange");
//        colorsMap.put("RETURN", "orange");
//
//        colorsMap.put("PLUS", "blue");
//        colorsMap.put("MINUS", "blue");
//        colorsMap.put("MUL", "blue");
//        colorsMap.put("DIV", "blue");
//        colorsMap.put("MOD", "blue");
//        colorsMap.put("ASSIGN", "blue");
//        colorsMap.put("EQ", "blue");
//        colorsMap.put("NEQ", "blue");
//        colorsMap.put("LT", "blue");
//        colorsMap.put("GT", "blue");
//        colorsMap.put("LE", "blue");
//        colorsMap.put("GE", "blue");
//        colorsMap.put("NOT", "blue");
//        colorsMap.put("AND", "blue");
//        colorsMap.put("OR", "blue");
//
//        colorsMap.put("L_PAREN", "null");
//        colorsMap.put("R_PAREN", "null");
//        colorsMap.put("L_BRACE", "null");
//        colorsMap.put("R_BRACE", "null");
//        colorsMap.put("L_BRACKT", "null");
//        colorsMap.put("R_BRACKT", "null");
//        colorsMap.put("COMMA", "null");
//        colorsMap.put("SEMICOLON", "null");
//
//        colorsMap.put("IDENT", "red");
//        colorsMap.put("INTEGR_CONST", "green");
//
//
//        MyVisitor myVisitor = new MyVisitor(ruleNames);
//        myVisitor.visit(tree);
//        // 若有错误就输出错误
//        ArrayList<String> errorList = myVisitor.getErrorList();
//        if (errorList.size()>0){
//            for (String string: errorList){
//                System.err.println(string);
//            }
//            return;
//        }
//        // 若没错误按规则输出内容
//        ArrayList<Position> positions = myVisitor.getPositions();
//        // 获取指定位置行列数
//        ArrayList<ArrayList<Integer>> allPositions = new ArrayList<>();
//        for (Position p : positions){
//            for (ArrayList<Integer> ps : p.positions){
//                if (ps.get(0) == lineNum && ps.get(1) == column){
//                    allPositions = p.positions;
//                    break;
//                }
//            }
//        }
//        // 遍历输出语法树，传入所有需要重命名的变量位置
//        Iterator iterator = new Iterator(ruleNames,allPositions);
//        iterator.visit(tree);
//        ArrayList<Node> nodeArrayList = iterator.getNodes();
//
//        for (Node node : nodeArrayList){
//            // 词法节点输出
//            if (node.isLexer){
//                if (node.lexerName.equals("EOF")) continue;
//                String color = colorsMap.get(node.lexerName);
//                if (color.equals("null")) continue;
//                for (int i=1; i<node.depth; i++){
//                    System.err.print("  ");
//                }
//                if (node.change)
//                    System.err.println(changeName + " " + node.lexerName + "[" + color + "]");
//                else System.err.println(node.lexerValue + " " + node.lexerName + "[" + color +"]");
//            }
//            else {
//                for (int i=1;i< node.depth;i++){
//                    System.err.print("  ");
//                }
//                System.err.println(node.parserName.substring(0,1).toUpperCase(Locale.ROOT) + node.parserName.substring(1));
//            }
//        }
        RuleNames ruleNames = new RuleNames(sysYLexer.getRuleNames(), sysYParser.getRuleNames());
        LLVMVisitor llvmVisitor = new LLVMVisitor(ruleNames);
        llvmVisitor.visit(tree);

//        llvmVisitor.console();
        llvmVisitor.save(t);
    }
}