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
parser grammar MessageParser;

options {
    language = Java;
    tokenVocab = MessageLexer;
}


@header {
import de.sayayi.lib.message.*;
import de.sayayi.lib.message.data.*;
import de.sayayi.lib.message.data.map.*;
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
        : SINGLE_QUOTE_START (t=text  { $value = $t.value; } )? SINGLE_QUOTE_END
        | DOUBLE_QUOTE_START (t=text  { $value = $t.value; } )? DOUBLE_QUOTE_END
        ;

forceQuotedMessage returns [Message value]
        : quotedMessage  { $value = $quotedMessage.value; }
        | string  { $value = MessageFactory.parse($string.value); }
        ;

parameter returns [ParameterPart value]
        : PARAM_START
          name=NAME
          (COMMA format=NAME)?
          (COMMA data)?
          PARAM_END
        ;

data returns [Data value]
        : string         { $value = new DataString($string.value); }
        | number=NUMBER  { $value = new DataNumber(Long.parseLong($number.text)); }
        | map            { $value = new DataMap($map.value); }
        ;

map returns [Map<MapKey,MapValue> value]
        @init {
          $value = new LinkedHashMap<MapKey,MapValue>();
        }
        : MAP_START
          mapElements[$value]
          (COMMA forceQuotedMessage  { $value.put(null, new MapValueMessage($forceQuotedMessage.value)); } )?
          MAP_END
        ;

mapElements [Map<MapKey,MapValue> value]
        : mapElement[$value] (COMMA mapElement[$value])*
        ;

mapElement [Map<MapKey,MapValue> value]
        : mapKey ARROW_OR_COLON mapValue  { $value.put($mapKey.key, $mapValue.value); }
        ;

mapKey returns [MapKey key]
        : relop=relationalOperatorOptional string
            { $key = new MapKeyString($relop.cmp, $string.value); }
        | relop=relationalOperatorOptional number=NUMBER
            { $key = new MapKeyNumber($relop.cmp, Long.parseLong($number.text)); }
        | bool=BOOL
            { $key = new MapKeyBool(Boolean.parseBoolean($bool.text)); }
        | eqop=equalOperatorOptional nil=NULL
            { $key = new MapKeyNull($eqop.cmp); }
        | eqop=equalOperatorOptional empty=EMPTY
            { $key = new MapKeyEmpty($eqop.cmp); }
        | name=NAME
            { $key = new MapKeyName($name.text); }
        ;

mapValue returns [MapValue value]
        : string
            { $value = new MapValueString($string.value); }
        | number=NUMBER
            { $value = new MapValueNumber(Long.parseLong($number.text)); }
        | bool=BOOL
            { $value = new MapValueBool(Boolean.parseBoolean($bool.text)); }
        | quotedMessage
            { $value = new MapValueMessage($quotedMessage.value); }
        ;

relationalOperatorOptional returns [MapKey.CompareType cmp]
        @init {
          $cmp = MapKey.CompareType.EQ;
        }
        : (relationalOperator { $cmp = $relationalOperator.cmp; } )?
        ;

relationalOperator returns [MapKey.CompareType cmp]
        : equalOperator  { $cmp = $equalOperator.cmp; }
        | LTE            { $cmp = MapKey.CompareType.LTE; }
        | LT             { $cmp = MapKey.CompareType.LT; }
        | GT             { $cmp = MapKey.CompareType.GT; }
        | GTE            { $cmp = MapKey.CompareType.GTE; }
        ;

equalOperatorOptional returns [MapKey.CompareType cmp]
        @init {
          $cmp = MapKey.CompareType.EQ;
        }
        : (equalOperator { $cmp = $equalOperator.cmp; } )?
        ;

equalOperator returns [MapKey.CompareType cmp]
        : EQ  { $cmp = MapKey.CompareType.EQ; }
        | NE  { $cmp = MapKey.CompareType.NE; }
        ;
