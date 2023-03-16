lexer grammar SysYLexer;

//@header{
//    package src;
//}

CONST : 'const';
INT : 'int';
VOID : 'void';
IF : 'if';
ELSE : 'else';
WHILE : 'while';
BREAK : 'break';
CONTINUE : 'continue';
RETURN : 'return';

PLUS : '+';MINUS : '-';MUL : '*';DIV : '/';MOD : '%';

ASSIGN : '=';EQ : '==';NEQ : '!=';LT : '<';GT : '>';LE : '<=';GE : '>=';

NOT : '!';AND : '&&';OR : '||';

L_PAREN : '(';R_PAREN : ')';L_BRACE : '{';R_BRACE : '}';L_BRACKT : '[';R_BRACKT : ']';

COMMA : ',';SEMICOLON : ';';


fragment DIGIT : [0-9];
fragment LETTER : [a-zA-Z];
fragment SHINUM : '0'|([1-9][0-9]*);
fragment BANUM : '0'[0-7]+;
fragment SHILIUNUM : ('0x'|'0X')[0-9a-fA-F];


IDENT : ('_'|LETTER)(DIGIT|LETTER|'_')* ;//以下划线或字母开头，仅包含下划线、英文字母大小写、阿拉伯数字,他们这些前面的0是进制的表示，所以是+

INTEGER_CONST : SHINUM|BANUM|SHILIUNUM;//数字常量，包含十进制数，0开头的八进制数，0x或0X开头的十六进制数


WS : [ \r\n\t]+ ->skip;
LINE_COMMENT : '//' .*? '\n' ->skip;
MULTILINE_COMMENT : '/*' .*? '*/' ->skip;