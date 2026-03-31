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
parser grammar MessageParser;


options {
    language = Java;
    tokenVocab = MessageLexer;
}


@header {
import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.part.MessagePart.*;
import de.sayayi.lib.message.part.MapKey;
import de.sayayi.lib.message.part.TypedValue;
}


message returns [Message.WithSpaces messageWithSpaces]
        : message0 EOF
        ;

message0 returns [Message.WithSpaces messageWithSpaces]
        : (textPart | parameterPart | templatePart | postFormatPart)*
        ;

textPart returns [Text part]
        : text
        ;

text returns [String characters]
        : CH+
        ;

quotedMessage returns [Message.WithSpaces messageWithSpaces]
        : SQ_START message0 SQ_END
        | DQ_START message0 DQ_END
        ;

quotedString returns [String string]
        : SQ_START text? SQ_END
        | DQ_START text? DQ_END
        ;

simpleString returns [String string]
        : nameOrKeyword
        | quotedString
        ;

parameterPart returns [Parameter part]
        : P_START
          parameterName
          (COMMA (configDefinition | mapEntry | parameterFormat))*
          (COMMA mapEntryDefault)?
          P_END
        ;

parameterName returns [String name]
        : nameOrKeyword  // kebab- or lower camel-case format
        ;

parameterFormat returns [String format]
        : FORMAT COLON nameOrKeyword  // kebab-case format
        ;

templatePart returns [Template part]
        : TPL_START
          templateName
          (COMMA (configDefinition | templateParameterDelegate))*
          TPL_END
        ;

templateName returns [String name]
        : nameOrKeyword  // kebab-case format
        ;

templateParameterDelegate returns [String parameter, String delegatedParameter]
        : nameOrKeyword EQ nameOrKeyword  // both in kebab- or lower camel-case format
        ;

postFormatPart returns [PostFormat part]
        : PF_START
          postFormatName
          COMMA quotedMessage
          (COMMA configDefinition)*
          PF_END
        ;

postFormatName returns [String name]
        : nameOrKeyword  // kebab-case format
        ;

configDefinition returns [String name, TypedValue<?> value]
        : NAME COLON BOOL           #configDefinitionBool
        | NAME COLON NUMBER         #configDefinitionNumber
        | NAME COLON simpleString   #configDefinitionString
        | NAME COLON quotedMessage  #configDefinitionMessage
        ;

mapEntry returns [List<MapKey> keys, TypedValue<?> value]
        : mapKeys COLON quotedMessage  #mapEntryMessage
        | mapKeys COLON simpleString   #mapEntryString
        ;

mapEntryDefault returns [Message.WithSpaces messageWithSpaces]
        : COLON quotedMessage
        | COLON simpleString
        ;

mapKeys returns [List<MapKey> keys]
        : mapKey
        | L_PAREN mapKey (COMMA mapKey)+ R_PAREN
        ;

mapKey returns [MapKey key]
        : equalOperator? NULL               #mapKeyNull
        | equalOperator? EMPTY              #mapKeyEmpty
        | BOOL                              #mapKeyBool
        | relationalOperator? NUMBER        #mapKeyNumber
        | relationalOperator? quotedString  #mapKeyString
        ;

relationalOperator returns [MapKey.CompareType cmp]
        : equalOperator
        | LTE
        | LT
        | GT
        | GTE
        ;

equalOperator returns [MapKey.CompareType cmp]
        : EQ
        | NE
        ;

nameOrKeyword returns [String name]
        : NAME
        | BOOL
        | NULL
        | EMPTY
        | FORMAT
        ;
