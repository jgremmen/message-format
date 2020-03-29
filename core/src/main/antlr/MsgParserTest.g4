parser grammar MsgParserTest;

options {
    tokenVocab = MessageTokenizer;
}

@header {
package de.sayayi.lib.message.parser;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.data.ParameterMap.CompareType;
}


message
        : message0 EOF
        ;

message0
        : ( textPart? parameter)* textPart?
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
          (P_COMMA data=parameterData)?
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

parameterData
        : string           # DataString
        | number=P_NUMBER  # DataNumber
        | map              # DataMap
        ;

map
        : MAP_START mapElements (M_COMMA defaultValue=mapValue)? MAP_END
        ;

mapElements
        : mapElement (M_COMMA mapElement)*
        ;

mapElement
        : key=mapKey M_ARROW value=mapValue
        ;

mapKey
        : relop=relationalOperator? string           # KeyString
        | relop=relationalOperator? number=M_NUMBER  # KeyNumber
        | bool=M_BOOL                                # KeyBool
        | eqop=equalOperator? nil=M_NULL             # KeyNull
        | eqop=equalOperator? empty=M_EMPTY          # KeyEmpty
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
        | M_LTE
        | M_LT
        | M_GT
        | M_GTE
        ;

equalOperator
        : M_EQ
        | M_NE
        ;
