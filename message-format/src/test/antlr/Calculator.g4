/*
 * Copyright 2026 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Combined lexer/parser grammar for a simple integer calculator.
 * Supports +, -, *, / operators with operator precedence and parentheses.
 * Numbers are integral and can be negative (unary minus).
 */
grammar Calculator;


// Parser rules

calc returns [int result]
    : expr EOF
    ;

expr returns [int result]
    : MINUS expr                   # UnaryMinus
    | expr op=(STAR | SLASH) expr  # MulDiv
    | expr op=(PLUS | MINUS) expr  # AddSub
    | LPAREN expr RPAREN           # Parens
    | NUMBER                       # Num
    ;


// Lexer rules

NUMBER  : [0-9]+ ;
PLUS    : '+' ;
MINUS   : '-' ;
STAR    : '*' ;
SLASH   : '/' ;
LPAREN  : '(' ;
RPAREN  : ')' ;

WS      : [ \t\r\n]+ -> skip ;
