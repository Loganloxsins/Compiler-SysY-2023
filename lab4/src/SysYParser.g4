parser grammar SysYParser;
options {
    tokenVocab = SysYLexer;
}

program: compUnit;

compUnit:  (decl| funcDef)+ EOF;

decl: constDecl | varDecl ;

    constDecl: CONST bType constDef (COMMA constDef)* SEMICOLON;

        bType : INT;

        constDef : IDENT (L_BRACKT constExp R_BRACKT)* ASSIGN constInitVal;

        constInitVal: constExp| L_BRACE (constInitVal (COMMA constInitVal)*)? R_BRACE;

    varDecl: bType varDef (COMMA varDef)* SEMICOLON ;

        varDef: IDENT (L_BRACKT  constExp R_BRACKT)*
                | IDENT (L_BRACKT  constExp R_BRACKT)* ASSIGN initVal;

                initVal: exp | L_BRACE (initVal (COMMA initVal)* )?  R_BRACE;

funcDef: funcType IDENT L_PAREN (funcFParams)? R_PAREN block;

    funcType: VOID | INT;

    funcFParams: funcFParam (COMMA funcFParam)*;

        funcFParam: bType IDENT (L_BRACKT R_BRACKT (L_BRACKT exp R_BRACKT)*)?;

    block : L_BRACE (blockItem)*R_BRACE;

        blockItem : decl| stmt;

 stmt: lVal ASSIGN exp SEMICOLON 			#AssignStmt
       | (exp)? SEMICOLON				    #ExpStmt
       | block						        #BlockStmt
       | IF L_PAREN cond R_PAREN stmt (ELSE stmt)?	#IfStmt
       | WHILE L_PAREN cond R_PAREN stmt			    #WhileStmt
       | BREAK SEMICOLON					            #BreakStmt
       | CONTINUE SEMICOLON				            #ContinueStmt
       | RETURN (exp)? SEMICOLON 			        #ReturnStmt;


exp : L_PAREN exp R_PAREN  		        #ParenExp
   | lVal 		   		                #LvalExp
   | number 				            #NumberExp
   | IDENT L_PAREN funcRParams? R_PAREN #CallFuncExp
   | unaryOp exp 			            #UnaryOpExp
   | exp (MUL | DIV | MOD) exp 		    #MulExp
   | exp (PLUS | MINUS) exp 		    #PlusExp
   ;

cond
   : exp                            #expCond
   | cond (LT | GT | LE | GE) cond  #ltCond
   | cond (EQ | NEQ) cond           #eqCond
   | cond AND cond                  #andCond
   | cond OR cond                   #orCond
   ;


lVal
   : IDENT (L_BRACKT exp R_BRACKT)*
   ;

number
   : INTEGER_CONST
   ;

unaryOp
   : PLUS
   | MINUS
   | NOT
   ;

funcRParams
   : param (COMMA param)*
   ;

param
   : exp
   ;

constExp
   : exp
   ;
