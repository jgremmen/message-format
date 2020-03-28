parser grammar MsgParser;

options {
    language = Java;
    tokenVocab = MessageTokenizer;
}

@header {
package de.sayayi.lib.message.parser;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.data.ParameterMap.CompareType;
}


message returns [Message value] locals [List<MessagePart> parts]
        @init {
          $parts = new ArrayList<MessagePart>();
        }
        : (
            (textPart { $parts.add($textPart.value); } )?
            parameter { $parts.add($parameter.value); }
          )*
          (textPart { $parts.add($textPart.value); } )?
        ;

textPart returns [TextPart value]
        : text
        ;

text returns [String value] locals [StringBuilder sb]
        @init {
          $sb = new StringBuilder();
        }
        @after {
          $value = $sb.toString();
        }
        : (CH { $sb.append($CH.text); })+
        ;

quotedMessage returns [Message value]
        : SINGLE_QUOTE_START message SINGLE_QUOTE_END  { $value = $message.value; }
        | DOUBLE_QUOTE_START message DOUBLE_QUOTE_END  { $value = $message.value; }
        ;

string returns [String value]
        @init {
          $value = "";
        }
        : SINGLE_QUOTE_START t=text? SINGLE_QUOTE_END  { $value = $t.value; }
        | DOUBLE_QUOTE_START t=text? DOUBLE_QUOTE_END  { $value = $t.value; }
        ;

parameter returns [ParameterPart value]
        : PARAM_START
          name=NAME
          (P_COMMA format=NAME)?
          (P_COMMA data=parameterData)?
          PARAM_END
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
        : key=mapKey arrow=M_ARROW value=mapValue
        ;

mapKey
        : relop=relationalOperator? string           # KeyString
        | relop=relationalOperator? number=M_NUMBER  # KeyNumber
        | bool=M_BOOL                                # KeyBool
        | eqop=equalOperator? nil=M_NULL             # KeyNull
        | eqop=equalOperator? empty=M_EMPTY          # KeyEmpty
        | name=NAME                                  # KeyName
        ;

mapValue
        : string         # ValueString
        | quotedMessage  # ValueMessage
        ;

relationalOperator returns [CompareType cmp]
        : equalOperator  { $cmp = $equalOperator.cmp; }
        | M_LTE  { $cmp = CompareType.LTE; }
        | M_LT   { $cmp = CompareType.LT; }
        | M_GT   { $cmp = CompareType.GT; }
        | M_GTE  { $cmp = CompareType.GTE; }
        ;

equalOperator returns [CompareType cmp]
        : M_EQ  { $cmp = CompareType.EQ; }
        | M_NE  { $cmp = CompareType.NE; }
        ;
