import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.HashMap;

public class Visitor extends SysYParserBaseVisitor<Void> {
    //index
    //typename
    //colour
    // 本来想用二维数组存，但是序号不是连续的

    private String[] ruleNames;
    private Vocabulary vocabulary;

    private HashMap<String, String> tcMap;

    public Visitor(String[] ruleNames, Vocabulary vocabulary) {
        this.ruleNames = ruleNames;
        this.vocabulary = vocabulary;
        HashMap<String, String> tcMap = new HashMap<>();
        this.tcMap = tcMap;
        tcMap.put("CONST", "orange");
        tcMap.put("INT", "orange");
        tcMap.put("VOID", "orange");
        tcMap.put("IF", "orange");
        tcMap.put("ELSE", "orange");
        tcMap.put("WHILE", "orange");
        tcMap.put("BREAK", "orange");
        tcMap.put("CONTINUE", "orange");
        tcMap.put("RETURN", "orange");
        tcMap.put("PLUS", "blue");
        tcMap.put("MINUS", "blue");
        tcMap.put("MUL", "blue");
        tcMap.put("DIV", "blue");
        tcMap.put("MOD", "blue");
        tcMap.put("ASSIGN", "blue");
        tcMap.put("EQ", "blue");
        tcMap.put("NEQ", "blue");
        tcMap.put("LT", "blue");
        tcMap.put("GT", "blue");
        tcMap.put("LE", "blue");
        tcMap.put("GE", "blue");
        tcMap.put("NOT", "blue");
        tcMap.put("AND", "blue");
        tcMap.put("OR", "blue");
        tcMap.put("IDENT", "red");
        tcMap.put("INTEGER_CONST", "green");
    }

    @Override
    public Void visitChildren(RuleNode node) {
        String ruleName = ruleNames[node.getRuleContext().getRuleIndex()];
        PrintSpace(node.getRuleContext().depth() - 1);
        ruleName = CapWord(ruleName);
        System.err.println(ruleName);
        return super.visitChildren(node);
    }

    @Override
    public Void visitTerminal(TerminalNode node) {
        String type = vocabulary.getSymbolicName(node.getSymbol().getType());
        if (tcMap.containsKey(type)) {
            RuleNode parent;
            if (node.getParent() instanceof RuleNode) {
                parent = (RuleNode) node.getParent();
                PrintSpace(parent.getRuleContext().depth());
            }
            System.err.print(ToSjNum(node.getText()) + " ");
            System.err.print(type);
            System.err.println("[" + tcMap.get(type) + "]");
        } else {
//            System.err.print(node.getText() + " ");
//            System.err.println(index);
        }
        return super.visitTerminal(node);
    }

    //---------------------工具函数--------------------------
    //print space
    public void PrintSpace(int depth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            sb.append("  ");
        }
        System.err.print(sb);
    }

    //没有单词首字母大写函数Capitalize a word
    public String CapWord(String word) {
        String t = word.substring(0, 1).toUpperCase() + word.substring(1);
        return t;
    }


    //to shijin num
    public String ToSjNum(String text) {
        if (text.charAt(0) == '0' && text.length() >= 2) {
            int num = (text.charAt(0) == '0' && text.length() >= 2 && (text.charAt(1) == 'x' || text.charAt(1) == 'X'))
                    ? Integer.parseInt(text.substring(2), 16)
                    : Integer.parseInt(text, 8);
            return String.valueOf(num);
        }
        return text;
    }
}
//如你所见，int type = node.getSymbol().getType();返回vocabulary
