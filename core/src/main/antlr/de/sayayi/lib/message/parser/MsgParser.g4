parser grammar MsgParser;

options {
    language = Java;
    tokenVocab = MessageTokenizer;
}

@header {
package de.sayayi.lib.message.parser;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.data.*;
import de.sayayi.lib.message.data.ParameterMap.CompareType;
import java.util.*;
}


message returns [Message value]
        : message0  { $value = $message0.value; }
          EOF
        ;

message0 returns [Message value] locals [List<MessagePart> parts]
        @init {
          $parts = new ArrayList<MessagePart>();
        }
        : (
            (textPart  { $parts.add($textPart.value); } )?
            parameter  { $parts.add($parameter.value); }
          )*
          (textPart  { $parts.add($textPart.value); } )?
        ;

textPart returns [TextPart value]
        : text
        ;

text returns [String value] locals [StringBuilder sb]
        @init {
          $sb = new StringBuilder();
        }
        @after {
          $value = $sb.toString().replaceAll("\\s+", " ");
        }
        : (CH  { $sb.append($CH.text); })+
        ;

quotedMessage returns [Message value]
        : SINGLE_QUOTE_START message0 SINGLE_QUOTE_END  { $value = $message0.value; }
        | DOUBLE_QUOTE_START message0 DOUBLE_QUOTE_END  { $value = $message0.value; }
        ;

string returns [String value]
        @init {
          $value = "";
        }
        : SINGLE_QUOTE_START (t=text { $value = $t.value; } )? SINGLE_QUOTE_END
        | DOUBLE_QUOTE_START (t=text { $value = $t.value; } )? DOUBLE_QUOTE_END
        ;

parameter returns [ParameterPart value]
        : PARAM_START
          name=NAME
          (P_COMMA format=NAME)?
          (P_COMMA data=parameterData)?
          PARAM_END
        ;

parameterData returns [ParameterData value]
        : string           # DataString
        | number=P_NUMBER  # DataNumber
        | map              # DataMap
        ;

map returns [Map<MapKey,MapValue> value]
        @init {
          $value = new LinkedHashMap<MapKey,MapValue>();
        }
        : MAP_START
          mapElements[$value]
          (M_COMMA mapValue  { $value.put(null, $mapValue.value); }
          )?
          MAP_END
        ;

mapElements [Map<MapKey,MapValue> value]
        : mapElement[$value] (M_COMMA mapElement[$value])*
        ;

mapElement [Map<MapKey,MapValue> value]
        : mapKey M_ARROW mapValue  { $value.put($mapKey.key, $mapValue.value); }
        ;

mapKey returns [MapKey key]
        : relop=relationalOperator? string
            { $key = new MapKeyString($relop.cmp, $string.value); }
        | relop=relationalOperator? number=M_NUMBER
            { $key = new MapKeyNumber($relop.cmp, Integer.parseInt($number.text)); }
        | bool=M_BOOL
            { $key = new MapKeyBool(Boolean.parseBoolean($bool.text)); }
        | eqop=equalOperator? nil=M_NULL
            { $key = new MapKeyNull($eqop.cmp); }
        | eqop=equalOperator? empty=M_EMPTY
            { $key = new MapKeyEmpty($eqop.cmp); }
        | name=NAME
            { $key = new MapKeyName($name.text); }
        ;

mapValue returns [MapValue value]
        : string
            { $value = new MapValueString($string.value); }
        | number=M_NUMBER
            { $value = new MapValueNumber(Integer.parseInt($number.text)); }
        | bool=M_BOOL
            { $value = new MapValueBool(Boolean.parseBoolean($bool.text)); }
        | quotedMessage
            { $value = new MapValueMessage($quotedMessage.value); }
        ;

relationalOperator returns [MapKey.CompareType cmp]
        : equalOperator  { $cmp = $equalOperator.cmp; }
        | M_LTE          { $cmp = MapKey.CompareType.LTE; }
        | M_LT           { $cmp = MapKey.CompareType.LT; }
        | M_GT           { $cmp = MapKey.CompareType.GT; }
        | M_GTE          { $cmp = MapKey.CompareType.GTE; }
        ;

equalOperator returns [MapKey.CompareType cmp]
        : M_EQ  { $cmp = MapKey.CompareType.EQ; }
        | M_NE  { $cmp = MapKey.CompareType.NE; }
        ;
