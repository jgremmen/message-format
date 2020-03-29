lexer grammar MessageTokenizer;

options {
    superClass = AbstractMessageLexer;
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
        : [\u0000-\u001f]+ -> skip
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
        : [\u0000-\u001f]+ -> skip
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
        : [\u0000-\u001f]+ -> skip
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
        : NameStartChar NameChar*
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
        : [\u0000-\u0020]+ -> skip
        ;
MAP_START
        : '{' -> pushMode(MAP)
        ;



// ------------------ In map mode ------------------
mode MAP;

MAP_END
        : '}' -> popMode
        ;
M_ARROW
        : '->' | ':'
        ;
M_WS
        : [\u0000-\u0020]+ -> skip
        ;
M_COMMA
        : ','
        ;
M_NULL
        : 'null'
        ;
M_EMPTY
        : 'empty'
        ;
M_BOOL
        : BoolLiteral
        ;
M_NAME
        : NameStartChar NameChar*
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
M_EQ
        : '='
        ;
M_NE
        : '<>' | '!'
        ;
M_LT
        : '<'
        ;
M_LTE
        : '<='
        ;
M_GT
        : '>'
        ;
M_GTE
        : '>='
        ;



// ------------------ Nop mode ------------------
// this mode isn't used but only there to define token names
mode NOP;

SINGLE_QUOTE_START : '\'' ;
DOUBLE_QUOTE_START : '"' ;
NAME               : ',' ;



// ------------------ Fragments ------------------

fragment ParamStart
        : '%{'
        ;

fragment BoolLiteral
        : 'true'
        | 'false'
        ;

fragment TextChar
        : '\u0020'..'\uffff'
        ;

fragment NameChar
        : NameStartChar
        | [0-9_]
        | '\u00b7'
        | '\u0300'..'\u036f'
        | '\u203f'..'\u2040'
        ;

fragment NameStartChar
        : [a-zA-Z] // these are the name letters below 0x7F
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
