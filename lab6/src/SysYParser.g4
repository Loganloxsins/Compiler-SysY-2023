parser grammar SysYParser;

options {
    tokenVocab = SysYLexer;
}

program : compUnit;

compUnit : (decl | funcDef)+ EOF;

decl : constDecl | varDecl ;

constDecl : CONST bType constDef (COMMA constDef)* SEMICOLON ;

bType : INT ;

constDef : IDENT (L_BRACKT constExp R_BRACKT)* ASSIGN constInitVal;

constInitVal : constExp
             | L_BRACE (constInitVal (COMMA constInitVal)*)? R_BRACE
             ;

varDecl : bType varDef (COMMA varDef)* SEMICOLON ;

varDef : IDENT (L_BRACKT constExp R_BRACKT)*
       | IDENT (L_BRACKT constExp R_BRACKT)* ASSIGN initVal
       ;

initVal : exp
        | L_BRACE (initVal (COMMA initVal)*)? R_BRACE;

funcDef : funcType IDENT L_PAREN (funcFParams)? R_PAREN block ;

funcType : VOID | INT;

funcFParams : funcFParam (COMMA funcFParam)*;

funcFParam : bType IDENT (L_BRACKT R_BRACKT (L_BRACKT exp R_BRACKT)*)?;

block : L_BRACE (blockItem)* R_BRACE;

blockItem : decl | stmt;

stmt : lVal ASSIGN exp SEMICOLON    #assign
     | (exp)? SEMICOLON             # isExpOrNone
     | block                        # isBlock
     | IF L_PAREN cond R_PAREN stmt (ELSE stmt)?    #if
     | WHILE L_PAREN cond R_PAREN stmt              #while
     | BREAK SEMICOLON  #break
     | CONTINUE SEMICOLON   #continue
     | RETURN (exp)? SEMICOLON  #return
     ;

exp : L_PAREN exp R_PAREN   # recurse
   | lVal                   # leftValue
   | number                 # isNum
   | IDENT L_PAREN funcRParams? R_PAREN     # funCall
   | unaryOp exp                            # unaryOperator
   | exp (MUL | DIV | MOD) exp              # mul
   | exp (PLUS | MINUS) exp                 # plus
   ;

cond: exp                              # isExp
    | cond (LT | GT | LE | GE) cond     # lt
    | cond (EQ | NEQ) cond              # eq
    | cond AND cond                     # and
    | cond OR cond                      # or
    ;

lVal : IDENT (L_BRACKT exp R_BRACKT)*;

number : INTEGR_CONST;

unaryOp : PLUS | MINUS | NOT ;

funcRParams : param (COMMA param)* ;

param : exp;

constExp : exp;

//primaryExp : L_PAREN exp R_PAREN | lVal | number ;
//
//unaryExp : primaryExp | IDENT L_PAREN (funcRParams)? R_PAREN
//         | unaryOp unaryExp;
//
//mulExp : unaryExp | mulExp (MUL|DIV|MOD) unaryExp;
//
//addExp : mulExp | addExp (PLUS|MINUS)mulExp ;
//
//relExp : addExp | relExp (LT|GT|LE|GE) addExp;
//
//eqExp : relExp | eqExp (EQ|NEQ) relExp;
//
//lAndExp : eqExp | lAndExp AND eqExp;
//
//lOrExp : lAndExp | lOrExp OR lAndExp;


