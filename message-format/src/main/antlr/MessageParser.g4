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
import de.sayayi.lib.message.internal.part.TemplatePart;
import de.sayayi.lib.message.internal.part.TextPart;
import de.sayayi.lib.message.part.parameter.ParameterPart;
import de.sayayi.lib.message.part.parameter.key.ConfigKey;
import de.sayayi.lib.message.part.parameter.key.ConfigKeyName;
import de.sayayi.lib.message.part.parameter.value.ConfigValue;
}


message returns [Message.WithSpaces messageWithSpaces]
        : message0 EOF
        ;

message0 returns [Message.WithSpaces messageWithSpaces]
        : (textPart | parameterPart | templatePart)*
        ;

textPart returns [TextPart part]
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

forceQuotedMessage returns [Message.WithSpaces messageWithSpaces]
        : quotedMessage
        | simpleString
        ;

parameterPart returns [ParameterPart part]
        : P_START
          name=simpleString
          (COMMA format=nameOrKeyword)?
          (COMMA parameterConfigElement)*
          (COMMA COLON forceQuotedMessage)?
          P_END
        ;

parameterConfigElement returns [List<ConfigKey> configKeys, ConfigValue configValue]
        : configNamedElement
        | configMapElement
        ;

templatePart returns [TemplatePart part]
        : TPL_START
          name=simpleString
          (COMMA (configNamedElement | templateParameterDelegate))*
          TPL_END
        ;

templateParameterDelegate returns [String parameter, String delegatedParameter]
        : simpleString EQ simpleString
        ;

configMapElement returns [List<ConfigKey> configKeys, ConfigValue configValue]
        : configMapKeys COLON quotedMessage  #configMapMessage
        | configMapKeys COLON simpleString   #configMapString
        ;

configNamedElement returns [ConfigKeyName configKey, ConfigValue configValue]
        : NAME COLON BOOL           #configNamedBool
        | NAME COLON NUMBER         #configNamedNumber
        | NAME COLON simpleString   #configNamedString
        | NAME COLON quotedMessage  #configNamedMessage
        ;

configMapKeys returns [List<ConfigKey> configKeys]
        : configMapKey
        | L_PAREN configMapKey (COMMA configMapKey)+ R_PAREN
        ;

configMapKey returns [ConfigKey configKey]
        : equalOperatorOptional NULL               #configMapKeyNull
        | equalOperatorOptional EMPTY              #configMapKeyEmpty
        | BOOL                                     #configMapKeyBool
        | relationalOperatorOptional NUMBER        #configMapKeyNumber
        | relationalOperatorOptional quotedString  #configMapKeyString
        ;

relationalOperatorOptional returns [ConfigKey.CompareType cmp]
        : relationalOperator?
        ;

relationalOperator returns [ConfigKey.CompareType cmp]
        : equalOperator
        | LTE
        | LT
        | GT
        | GTE
        ;

equalOperatorOptional returns [ConfigKey.CompareType cmp]
        : equalOperator?
        ;

equalOperator returns [ConfigKey.CompareType cmp]
        : EQ
        | NE
        ;

nameOrKeyword returns [String name]
        : NAME
        | BOOL
        | NULL
        | EMPTY
        ;
