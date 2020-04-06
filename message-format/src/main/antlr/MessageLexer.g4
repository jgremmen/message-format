/*
 * Copyright 2020 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
lexer grammar MessageLexer;

options {
    superClass = AbstractLexer;
}



// ------------------ Default mode ------------------

PARAM_START
        : ParamStart -> pushMode(PARAMETER)
        ;
CH
        : ' '+
        | EscapeSequence
        | TextChar
        ;
CTRL_CHAR
        : CtrlChar+ -> skip
        ;



// ------------------ In single quoted text mode ------------------
mode TEXT1;

PARAM_START1
        : ParamStart -> pushMode(PARAMETER)
        ;
SINGLE_QUOTE_END
        : '\'' -> popMode
        ;
CH1
        : ' '+
        | EscapeSequence
        | TextChar
        ;
CTRL_CHAR1
        : CtrlChar+ -> skip
        ;



// ------------------ In double quoted text mode ------------------
mode TEXT2;

PARAM_START2
        : ParamStart -> pushMode(PARAMETER)
        ;
DOUBLE_QUOTE_END
        : '"' -> popMode
        ;
CH2
        : ' '+
        | EscapeSequence
        | TextChar
        ;
CTRL_CHAR2
        : CtrlChar+ -> skip
        ;


// ------------------ In parameter mode ------------------
mode PARAMETER;

PARAM_END
        : '}' -> popMode
        ;
P_COMMA
        : ','
        ;
P_BOOL
        : BoolLiteral
        ;
P_NAME
        : DashedName
        ;
P_NUMBER
        : Number
        ;
P_SQ_START
        : '\'' -> pushMode(TEXT1)
        ;
P_DQ_START
        : '"' -> pushMode(TEXT2)
        ;
P_WS
        : (CtrlChar | ' ')+ -> skip
        ;
MAP_START
        : '{' -> pushMode(MAP)
        ;



// ------------------ In map mode ------------------
mode MAP;

MAP_END
        : '}' -> popMode
        ;
COLON
        : ':'
        ;
M_WS
        : (CtrlChar | ' ')+ -> skip
        ;
M_COMMA
        : ','
        ;
NULL
        : 'null'
        ;
EMPTY
        : 'empty'
        ;
M_BOOL
        : BoolLiteral
        ;
M_NAME
        : DashedName
        ;
M_NUMBER
        : Number
        ;
M_SQ_START
        : '\'' -> pushMode(TEXT1)
        ;
M_DQ_START
        : '"' -> pushMode(TEXT2)
        ;
EQ
        : '='
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



// ------------------ Nop mode ------------------
// this mode isn't used but only there to define token names
mode NOP;

SINGLE_QUOTE_START : '\'' ;
DOUBLE_QUOTE_START : '"' ;
NAME               : '=' ;
COMMA              : ',' ;
NUMBER             : '>' ;
BOOL               : '<' ;



// ------------------ Fragments ------------------

fragment ParamStart
        : '%{'
        ;

fragment BoolLiteral
        : 'true'
        | 'false'
        ;

fragment CtrlChar
        : [\u0000-\u001f]
        ;

fragment TextChar
        : '\u0020'..'\uffff'
        ;

fragment DashedName
        : Name ('-' Name)*
        ;

fragment Name
        : NameStartChar NameChar*
        ;

fragment NameChar
        : NameStartChar
        | [0-9_]
        | '\u00b7'
        | '\u0300'..'\u036f'
        | '\u203f'..'\u2040'
        ;

fragment NameStartChar
        : [a-zA-Z]
        | ~[\u0000-\u007F\uD800-\uDBFF] // covers all characters above 0x7F which are not a surrogate
        | [\uD800-\uDBFF] [\uDC00-\uDFFF] // covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
        ;

fragment Number
        : '-'? [0-9]+
        ;

fragment EscapeSequence
        : '\\u' HexDigit HexDigit HexDigit HexDigit
        | '\\' ["'%{\\]
        ;

fragment HexDigit
        : [0-9a-fA-F]
        ;
