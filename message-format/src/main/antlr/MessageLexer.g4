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
    SINGLE_QUOTE_START,
    DOUBLE_QUOTE_START
}



// ------------------ Default mode ------------------

PARAM_START
        : ParamStart -> pushMode(PARAMETER)
        ;
CH
        : Character
        ;
CTRL_CHAR
        : CtrlChar+ -> skip
        ;



// ------------------ In single quoted text mode ------------------
mode TEXT1;

PARAM_START1
        : ParamStart -> pushMode(PARAMETER), type(PARAM_START)
        ;
SINGLE_QUOTE_END
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

PARAM_START2
        : ParamStart -> pushMode(PARAMETER), type(PARAM_START)
        ;
DOUBLE_QUOTE_END
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

PARAM_END
        : '}' -> popMode
        ;
COMMA
        : ','
        ;
COLON
        : ':'
        ;
BOOL
        : 'true'
        | 'false'
        ;
NULL
        : 'null'
        ;
EMPTY
        : 'empty'
        ;
NAME
        : DashedName
        ;
NUMBER
        : Number
        ;
P_SQ_START
        : '\'' -> pushMode(TEXT1), type(SINGLE_QUOTE_START)
        ;
P_DQ_START
        : '"' -> pushMode(TEXT2), type(DOUBLE_QUOTE_START)
        ;
P_WS
        : (CtrlChar | ' ')+ -> skip
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



// ------------------ Fragments ------------------

fragment ParamStart
        : '%{'
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
        ;

fragment Number
        : '-'? [0-9]+
        ;

fragment Character
        : ' '+
        | EscapeSequence
        | TextChar
        ;

fragment EscapeSequence
        : '\\u' HexDigit HexDigit HexDigit HexDigit
        | '\\' ["'%{\\]
        ;

fragment HexDigit
        : [0-9a-fA-F]
        ;
