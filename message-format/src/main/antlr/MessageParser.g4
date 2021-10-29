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
import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.internal.part.*;
import de.sayayi.lib.message.data.Data;
import de.sayayi.lib.message.data.map.*;
import java.util.Map;
}


message returns [Message.WithSpaces value]
        : message0 EOF
        ;

message0 returns [Message.WithSpaces value]
        : (textPart? parameter)* textPart?
        ;

textPart returns [TextPart value]
        : text
        ;

text returns [String value]
        : CH+
        ;

quotedMessage returns [Message.WithSpaces value]
        : SINGLE_QUOTE_START message0 SINGLE_QUOTE_END
        | DOUBLE_QUOTE_START message0 DOUBLE_QUOTE_END
        ;

string returns [String value]
        : SINGLE_QUOTE_START text? SINGLE_QUOTE_END
        | DOUBLE_QUOTE_START text? DOUBLE_QUOTE_END
        ;

forceQuotedMessage returns [Message.WithSpaces value]
        : quotedMessage
        | string
        ;

parameter returns [ParameterPart value]
        : PARAM_START name=NAME (COMMA format=NAME)? (COMMA data)? PARAM_END
        ;

data returns [Data value]
        : string  #dataString
        | NUMBER  #dataNumber
        | map     #dataMap
        ;

map returns [Map<MapKey,MapValue> value]
        : MAP_START mapElements (COMMA forceQuotedMessage)? MAP_END
        ;

mapElements returns [Map<MapKey,MapValue> value]
        : mapElement (COMMA mapElement)*
        ;

mapElement returns [MapKey key, MapValue value]
        : mapKey COLON mapValue
        ;

mapKey returns [MapKey key]
        : relationalOperatorOptional string  #mapKeyString
        | relationalOperatorOptional NUMBER  #mapKeyNumber
        | BOOL                               #mapKeyBool
        | equalOperatorOptional NULL         #mapKeyNull
        | equalOperatorOptional EMPTY        #mapKeyEmpty
        | NAME                               #mapKeyName
        ;

mapValue returns [MapValue value]
        : string         #mapValueString
        | NUMBER         #mapValueNumber
        | BOOL           #mapValueBool
        | quotedMessage  #mapValueMessage
        ;

relationalOperatorOptional returns [MapKey.CompareType cmp]
        : relationalOperator?
        ;

relationalOperator returns [MapKey.CompareType cmp]
        : equalOperator
        | LTE
        | LT
        | GT
        | GTE
        ;

equalOperatorOptional returns [MapKey.CompareType cmp]
        : equalOperator?
        ;

equalOperator returns [MapKey.CompareType cmp]
        : EQ
        | NE
        ;
