lexer grammar SysYLexer;

CONST : 'const';

INT : 'int';

VOID : 'void';

IF : 'if';

ELSE : 'else';

WHILE : 'while';

BREAK : 'break';

CONTINUE : 'continue';

RETURN : 'return';

PLUS : '+';

MINUS : '-';

MUL : '*';

DIV : '/';

MOD : '%';

ASSIGN : '=';

EQ : '==';

NEQ : '!=';

LT : '<';

GT : '>';

LE : '<=';

GE : '>=';

NOT : '!';

AND : '&&';

OR : '||';

L_PAREN : '(';

R_PAREN : ')';

L_BRACE : '{';

R_BRACE : '}';

L_BRACKT : '[';

R_BRACKT : ']';

COMMA : ',';

SEMICOLON : ';';

IDENT : ('_' | LETTER) ('_' | LETTER | DIGIT)* ;

INTEGR_CONST : '0' | ([1-9] DIGIT*)
             | ('0' [0-7]*)
             | ('0x' | '0X') (DIGIT | [a-fA-F])*
             ;

WS : [ \r\n\t]+ -> skip ;

LINE_COMMENT : '//' .*? '\n' -> skip ;

MULTILINE_COMMENT : '/*' .*? '*/' -> skip ;

LETTER : [a-zA-Z];
DIGIT: [0-9];