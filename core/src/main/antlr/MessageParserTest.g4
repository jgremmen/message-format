parser grammar MessageParserTest;

options {
    tokenVocab = MessageLexer;
}


message
        : message0 EOF
        ;

message0
        : ( textPart? parameter )* textPart?
        ;

textPart
        : text
        ;

text
        : (character) +
        ;

character
        : CH
        | CH1
        | CH2
        ;

quotedMessage
        : singleQuoteStart message0 singleQuoteEnd
        | doubleQuoteStart message0 doubleQuoteEnd
        ;

string
        : singleQuoteStart text? singleQuoteEnd
        | doubleQuoteStart text? doubleQuoteEnd
        ;

forceQuotedMessage
        : string
        | quotedMessage
        ;

singleQuoteStart
        : P_SQ_START
        | M_SQ_START
        ;

singleQuoteEnd
        : SINGLE_QUOTE_END
        ;

doubleQuoteEnd
        : DOUBLE_QUOTE_END
        ;

doubleQuoteStart
        : P_DQ_START
        | M_DQ_START
        ;

name
        : P_NAME
        | M_NAME
        ;

parameter
        : parameterStart
          name
          (P_COMMA format=name)?
          (P_COMMA data)?
          parameterEnd
        ;

parameterStart
        : PARAM_START
        | PARAM_START1
        | PARAM_START2
        ;

parameterEnd
        : PARAM_END
        ;

data
        : string           # DataString
        | number=P_NUMBER  # DataNumber
        | map              # DataMap
        ;

map
        : MAP_START mapElements (M_COMMA forceQuotedMessage)? MAP_END
        ;

mapElements
        : mapElement (M_COMMA mapElement)*
        ;

mapElement
        : key=mapKey ARROW_OR_COLON value=mapValue
        ;

mapKey
        : relop=relationalOperator? string           # KeyString
        | relop=relationalOperator? number=M_NUMBER  # KeyNumber
        | bool=M_BOOL                                # KeyBool
        | eqop=equalOperator? nil=NULL               # KeyNull
        | eqop=equalOperator? empty=EMPTY            # KeyEmpty
        | name                                       # KeyName
        ;

mapValue
        : string           # ValueString
        | number=M_NUMBER  # ValueNumber
        | bool=M_BOOL      # ValueBool
        | quotedMessage    # ValueMessage
        ;

relationalOperator
        : equalOperator
        | LTE
        | LT
        | GT
        | GTE
        ;

equalOperator
        : EQ
        | NE
        ;
