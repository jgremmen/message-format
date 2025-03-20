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
package de.sayayi.lib.message.internal.parser;

import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.exception.MessageParserException;
import de.sayayi.lib.message.part.MessagePart;
import de.sayayi.lib.message.part.normalizer.LRUMessagePartNormalizer;
import de.sayayi.lib.message.part.parameter.ParameterConfig;
import de.sayayi.lib.message.part.parameter.ParameterPart;
import de.sayayi.lib.message.part.parameter.key.*;
import de.sayayi.lib.message.part.parameter.value.ConfigValueBool;
import de.sayayi.lib.message.part.parameter.value.ConfigValueMessage;
import de.sayayi.lib.message.part.parameter.value.ConfigValueNumber;
import de.sayayi.lib.message.part.parameter.value.ConfigValueString;
import org.junit.jupiter.api.*;

import java.util.Map;

import static de.sayayi.lib.message.exception.MessageParserException.Type.MESSAGE;
import static de.sayayi.lib.message.part.TextPartFactory.*;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.CompareType.*;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Message compiler test cases.
 *
 * @author Jeroen Gremmen
 * @since 0.20.0
 */
@DisplayName("Message compiler")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class MessageCompilerTest
{
  private static MessageCompiler COMPILER;


  @BeforeAll
  static void init() {
    COMPILER = new MessageCompiler(new MessageFactory(new LRUMessagePartNormalizer(64)));
  }


  @Test
  @DisplayName("Text only")
  void testTextOnly()
  {
    assertArrayEquals(
        new MessagePart[] { emptyText() },
        COMPILER.compileMessage("").getMessageParts());

    assertArrayEquals(
        new MessagePart[] { addSpaces(noSpaceText("a\nb"), false, true) },
        COMPILER.compileMessage("a\\u000ab ").getMessageParts());

    assertArrayEquals(
        new MessagePart[] { addSpaces(emptyText(), true, true) },
        COMPILER.compileMessage("          ").getMessageParts());

    assertArrayEquals(
        new MessagePart[] { addSpaces(noSpaceText("'\\"), true, false) },
        COMPILER.compileMessage("  \\'\\\\").getMessageParts());

    assertArrayEquals(
        new MessagePart[] { noSpaceText("this is a text") },
        COMPILER.compileMessage("this  is  a  text").getMessageParts());
  }


  @Test
  @DisplayName("Parameter with name only")
  void testParameterWithNameOnly()
  {
    assertArrayEquals(
        new MessagePart[] { new ParameterPart("p", false, true) },
        COMPILER.compileMessage("%{ p } ").getMessageParts());

    assertArrayEquals(
        new MessagePart[] { new ParameterPart("empty", true, true) },
        COMPILER.compileMessage(" %{ empty } ").getMessageParts());

    assertArrayEquals(
        new MessagePart[] { new ParameterPart("test-id", false, false) },
        COMPILER.compileMessage("%{ 'test-id' }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] { new ParameterPart(",,,", true, false) },
        COMPILER.compileMessage(" %{ \",,,\" }").getMessageParts());

    var mpe = assertThrowsExactly(
        MessageParserException.class,
        () -> COMPILER.compileMessage("%{ \"\" }"));
    assertEquals("parameter name must not be empty", mpe.getErrorMessage());
    assertEquals(MESSAGE, mpe.getType());
  }



  @Test
  @DisplayName("Parameter format")
  void testParameterFormat()
  {
    var emptyParameterConfig = new ParameterConfig(Map.of());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", "bool", false, false, emptyParameterConfig)
        },
        COMPILER.compileMessage("%{ p, bool }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", "null", false, false, emptyParameterConfig)
        },
        COMPILER.compileMessage("%{p,null}").getMessageParts());
  }



  @Test
  @DisplayName("Parameter with named configuration")
  void testParameterWithNamedConfiguration()
  {
    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", new ParameterConfig(Map.of(
                new ConfigKeyName("charset"), ConfigValueBool.FALSE
            )))
        },
        COMPILER.compileMessage("%{ p, charset:false }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", new ParameterConfig(Map.of(
                new ConfigKeyName("charset"), ConfigValueBool.TRUE
            )))
        },
        COMPILER.compileMessage("%{ p, charset:true }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", new ParameterConfig(Map.of(
                new ConfigKeyName("clip"), new ConfigValueNumber(-5)
            )))
        },
        COMPILER.compileMessage("%{ p, clip:-5 }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", new ParameterConfig(Map.of(
                new ConfigKeyName("default"), new ConfigValueString("yes")
            )))
        },
        COMPILER.compileMessage("%{ p, default:'yes' }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", new ParameterConfig(Map.of(
                new ConfigKeyName("default"), new ConfigValueString("no")
            )))
        },
        COMPILER.compileMessage("%{ p, default:no }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", new ParameterConfig(Map.of(
                new ConfigKeyName("msg"), new ConfigValueMessage(COMPILER.compileMessage(" %{q}"))
            )))
        },
        COMPILER.compileMessage("%{ p, msg:' %{q}' }").getMessageParts());

    var mpe = assertThrowsExactly(
        MessageParserException.class,
        () -> COMPILER.compileMessage("%{ p1, msg:yes, msg:no }").getMessageParts());
    assertEquals("duplicate config element msg for parameter 'p1'", mpe.getErrorMessage());
    assertEquals(MESSAGE, mpe.getType());
  }


  @Test
  @DisplayName("Parameter with 'null' configuration key")
  void testParameterWithNullKey()
  {
    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", new ParameterConfig(Map.of(
                ConfigKeyNull.EQ, new ConfigValueString("msg")
            )))
        },
        COMPILER.compileMessage("%{ p, =null:msg }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", new ParameterConfig(Map.of(
                ConfigKeyNull.NE, new ConfigValueMessage(COMPILER.compileMessage("msg %{n}"))
            )))
        },
        COMPILER.compileMessage("%{ p, !null:'msg %{n}' }").getMessageParts());
  }


  @Test
  @DisplayName("Parameter with 'empty' configuration key")
  void testParameterWithEmptyKey()
  {
    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", new ParameterConfig(Map.of(
                ConfigKeyEmpty.EQ, new ConfigValueString("msg")
            )))
        },
        COMPILER.compileMessage("%{ p, empty:msg }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", new ParameterConfig(Map.of(
                ConfigKeyEmpty.NE, new ConfigValueMessage(COMPILER.compileMessage("msg %{n}"))
            )))
        },
        COMPILER.compileMessage("%{ p, <>empty:'msg %{n}' }").getMessageParts());
  }


  @Test
  @DisplayName("Parameter with boolean configuration key")
  void testParameterWithBooleanKey()
  {
    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", new ParameterConfig(Map.of(
                ConfigKeyBool.TRUE, new ConfigValueString("msg")
            )))
        },
        COMPILER.compileMessage("%{ p, true:msg }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", new ParameterConfig(Map.of(
                ConfigKeyBool.FALSE, new ConfigValueMessage(COMPILER.compileMessage("msg %{n}"))
            )))
        },
        COMPILER.compileMessage("%{ p, false:'msg %{n}' }").getMessageParts());
  }


  @Test
  @DisplayName("Parameter with number configuration key")
  void testParameterWithNumberKey()
  {
    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", new ParameterConfig(Map.of(
                new ConfigKeyNumber(EQ, 16), new ConfigValueString("msg1")
            )))
        },
        COMPILER.compileMessage("%{ p, 16:msg1 }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", new ParameterConfig(Map.of(
                new ConfigKeyNumber(EQ, -16), new ConfigValueMessage(COMPILER.compileMessage(" msg 2 "))
            )))
        },
        COMPILER.compileMessage("%{ p, =-16:\" msg 2 \" }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", new ParameterConfig(Map.of(
                new ConfigKeyNumber(LT, 1000), new ConfigValueString("msg3")
            )))
        },
        COMPILER.compileMessage("%{ p, < 1000:msg3 }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", new ParameterConfig(Map.of(
                new ConfigKeyNumber(LTE, 0), new ConfigValueString("msg4")
            )))
        },
        COMPILER.compileMessage("%{ p,<=0:msg4 }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", new ParameterConfig(Map.of(
                new ConfigKeyNumber(NE, 1), new ConfigValueString("msg5")
            )))
        },
        COMPILER.compileMessage("%{ p,<>1:msg5 }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", new ParameterConfig(Map.of(
                new ConfigKeyNumber(GT, 123456789), new ConfigValueString("msg6")
            )))
        },
        COMPILER.compileMessage("%{ p,>123456789:msg6 }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", new ParameterConfig(Map.of(
                new ConfigKeyNumber(GTE, -987654321), new ConfigValueMessage(COMPILER.compileMessage(" msg 7"))
            )))
        },
        COMPILER.compileMessage("%{ p, >= -987654321:\" msg 7\" }").getMessageParts());
  }


  @Test
  @DisplayName("Parameter with default map value")
  void testParameterWithDefaultMapValue()
  {
    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", new ParameterConfig(singletonMap(
                null, new ConfigValueMessage(COMPILER.compileMessage("test"))
            )))
        },
        COMPILER.compileMessage("%{ p, :test }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", new ParameterConfig(singletonMap(
                null, new ConfigValueMessage(COMPILER.compileMessage(" %{n} items"))
            )))
        },
        COMPILER.compileMessage("%{ p, :' %{n} items' }").getMessageParts());
  }
}
