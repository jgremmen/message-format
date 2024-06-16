/*
 * Copyright 2020 Jeroen Gremmen
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
lexer grammar MessageLexer;


tokens {
    SQ_START,
    DQ_START,
    COMMA,
    COLON,
    BOOL,
    NUMBER,
    NAME,
    EQ,
    L_PAREN,
    R_PAREN
}



// ------------------ Default mode ------------------

P_START
        : ParamStart -> pushMode(PARAMETER)
        ;
TPL_START
        : TemplateStart -> pushMode(TEMPLATE)
        ;
CH
        : Character
        ;
CTRL_CHAR
        : CtrlChar+ -> skip
        ;



// ------------------ In single quoted text mode ------------------
mode TEXT1;

P_START1
        : ParamStart -> pushMode(PARAMETER), type(P_START)
        ;
TPL_START1
        : TemplateStart -> pushMode(TEMPLATE), type(TPL_START)
        ;
SQ_END
        : '\'' -> popMode
        ;
CH1
        : Character -> type(CH)
        ;
CTRL_CHAR1
        : CtrlChar+ -> skip
        ;



// ------------------ In double quoted text mode ------------------
mode TEXT2;

P_START2
        : ParamStart -> pushMode(PARAMETER), type(P_START)
        ;
TPL_START2
        : TemplateStart -> pushMode(TEMPLATE), type(TPL_START)
        ;
DQ_END
        : '"' -> popMode
        ;
CH2
        : Character -> type(CH)
        ;
CTRL_CHAR2
        : CtrlChar+ -> skip
        ;



// ------------------ In parameter mode ------------------
mode PARAMETER;

P_END
        : '}' -> popMode
        ;
P_COMMA
        : ',' -> type(COMMA)
        ;
P_COLON
        : ':' -> type(COLON)
        ;
P_BOOL
        : ('true' | 'false') -> type(BOOL)
        ;
NULL
        : 'null'
        ;
EMPTY
        : 'empty'
        ;
P_NAME
        : DashedName -> type(NAME)
        ;
P_NUMBER
        : Number -> type(NUMBER)
        ;
P_SQ_START
        : '\'' -> pushMode(TEXT1), type(SQ_START)
        ;
P_DQ_START
        : '"' -> pushMode(TEXT2), type(DQ_START)
        ;
P_WS
        : (CtrlChar | ' ')+ -> skip
        ;
P_EQ
        : '=' -> type(EQ)
        ;
NE
        : '<>' | '!'
        ;
LT
        : '<'
        ;
LTE
        : '<='
        ;
GT
        : '>'
        ;
GTE
        : '>='
        ;
P_L_PAREN
        : '(' -> type(L_PAREN)
        ;
P_R_PAREN
        : ')' -> type(R_PAREN)
        ;



// ------------------ In template mode ------------------
mode TEMPLATE;

TPL_END
        : ']' -> popMode
        ;
T_COMMA
        : ',' -> type(COMMA)
        ;
T_COLON
        : ':' -> type(COLON)
        ;
T_EQ
        : '=' -> type(EQ)
        ;
T_BOOL
        : ('true' | 'false') -> type(BOOL)
        ;
T_NUMBER
        : Number -> type(NUMBER)
        ;
T_NAME
        : DashedName -> type(NAME)
        ;
T_SQ_START
        : '\'' -> pushMode(TEXT1), type(SQ_START)
        ;
T_DQ_START
        : '"' -> pushMode(TEXT2), type(DQ_START)
        ;
T_WS
        : (CtrlChar | ' ')+ -> skip
        ;



// ------------------ Fragments ------------------

fragment ParamStart
        : '%{'
        ;

fragment TemplateStart
        : '%['
        ;

fragment CtrlChar
        : [\u0000-\u001f]
        ;

fragment DashedName
        : Name ('-' Name)*
        ;

fragment Name
        : NameStartChar NameChar*
        ;

fragment NameChar
        : NameStartChar
        | [\p{N}]  // Unicode N (number)
        | '_'
        ;

fragment NameStartChar
        : [\p{L}]  // Unicode L (letter)
        ;

fragment Number
        : '-'? [0-9]+
        ;

fragment Character
        : EscapeSequence
        | [\p{Zs}]+  // Unicode Zs (whitespace)
        | [\p{L}]    // Unicode L (letter)
        | [\p{N}]    // Unicode N (number)
        | [\p{P}]    // Unicode P (punctuation)
        | [\p{S}]    // Unicode S (symbol)
        ;

fragment EscapeSequence
        : '\\u' HexDigit HexDigit HexDigit HexDigit
        | '\\' ["'%{\\\u005b]
        ;

fragment HexDigit
        : [0-9a-fA-F]
        ;
