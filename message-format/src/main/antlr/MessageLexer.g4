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
    DQ_START
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
        : '\'' -> pushMode(TEXT1), type(SQ_START)
        ;
P_DQ_START
        : '"' -> pushMode(TEXT2), type(DQ_START)
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



// ------------------ Template -------------------
mode TEMPLATE;

TPL_END
        : ']' -> popMode
        ;
T_NAME
        : DashedName -> type(NAME)
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
        | [0-9_]
        | '\u00b7'
        | '\u0300'..'\u036f'
        | '\u203f'..'\u2040'
        ;

fragment NameStartChar
        : [a-zA-Z]
        | ~[\u0000-\u007F\uD800-\uDBFF]  // covers all characters above 0x7F which are not a surrogate
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
