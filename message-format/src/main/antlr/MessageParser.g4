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
import de.sayayi.lib.message.parameter.ParamConfig;
import de.sayayi.lib.message.parameter.key.*;
import de.sayayi.lib.message.parameter.value.*;
import de.sayayi.lib.message.part.*;
import java.util.Map;
}


message returns [Message.WithSpaces value]
        : message0 EOF
        ;

message0 returns [Message.WithSpaces value]
        : (textPart | parameterPart | templatePart)*
        ;

textPart returns [TextPart value]
        : text
        ;

text returns [String value]
        : CH+
        ;

quotedMessage returns [Message.WithSpaces value]
        : SQ_START message0 SQ_END
        | DQ_START message0 DQ_END
        ;

string returns [String value]
        : SQ_START text? SQ_END
        | DQ_START text? DQ_END
        ;

forceQuotedMessage returns [Message.WithSpaces value]
        : quotedMessage
        | string
        ;

parameterPart returns [ParameterPart value]
        : P_START
          name=nameOrKeyword
          (COMMA format=nameOrKeyword)?
          (COMMA configElement)*
          (COMMA COLON forceQuotedMessage)?
          P_END
        ;

templatePart returns [TemplatePart value]
        : TPL_START
          nameOrKeyword
          TPL_END
        ;

configElement returns [ConfigKey key, ConfigValue value]
        : configKey COLON configValue
        ;

configKey returns [ConfigKey key]
        : relationalOperatorOptional string  #configKeyString
        | relationalOperatorOptional NUMBER  #configKeyNumber
        | BOOL                               #configKeyBool
        | equalOperatorOptional NULL         #configKeyNull
        | equalOperatorOptional EMPTY        #configKeyEmpty
        | NAME                               #configKeyName
        ;

configValue returns [ConfigValue value]
        : BOOL           #configValueBool
        | NUMBER         #configValueNumber
        | string         #configValueString
        | nameOrKeyword  #configValueString
        | quotedMessage  #configValueMessage
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
