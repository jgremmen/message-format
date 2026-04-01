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
import de.sayayi.lib.message.internal.part.config.MessagePartConfig;
import de.sayayi.lib.message.internal.part.map.MessagePartMap;
import de.sayayi.lib.message.internal.part.map.key.*;
import de.sayayi.lib.message.internal.part.parameter.ParameterPart;
import de.sayayi.lib.message.internal.part.template.TemplatePart;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueBool;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueMessage;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueNumber;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueString;
import de.sayayi.lib.message.part.MessagePart;
import de.sayayi.lib.message.part.normalizer.LRUMessagePartNormalizer;
import org.junit.jupiter.api.*;

import java.util.Map;

import static de.sayayi.lib.message.exception.MessageParserException.Type.MESSAGE;
import static de.sayayi.lib.message.internal.part.config.MessagePartConfig.EMPTY_CONFIG;
import static de.sayayi.lib.message.internal.part.map.MessagePartMap.EMPTY_MAP;
import static de.sayayi.lib.message.part.MapKey.CompareType.*;
import static de.sayayi.lib.message.part.TextPartFactory.*;
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
        COMPILER.compileMessage("%{ test-id }").getMessageParts());
  }



  @Test
  @DisplayName("Parameter format")
  void testParameterFormat()
  {
    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", "bool", false, false, EMPTY_CONFIG, EMPTY_MAP)
        },
        COMPILER.compileMessage("%{ p, format:bool }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", "null", false, false, EMPTY_CONFIG, EMPTY_MAP)
        },
        COMPILER.compileMessage("%{p,format:null}").getMessageParts());
  }



  @Test
  @DisplayName("Parameter with named configuration")
  void testParameterWithNamedConfiguration()
  {
    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", new MessagePartConfig(Map.of("charset", TypedValueBool.FALSE)), EMPTY_MAP)
        },
        COMPILER.compileMessage("%{ p, charset:false }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", new MessagePartConfig(Map.of("charset", TypedValueBool.TRUE)), EMPTY_MAP)
        },
        COMPILER.compileMessage("%{ p, charset:true }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", new MessagePartConfig(Map.of("clip", new TypedValueNumber(-5))), EMPTY_MAP)
        },
        COMPILER.compileMessage("%{ p, clip:-5 }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", new MessagePartConfig(Map.of("default", new TypedValueString("yes"))), EMPTY_MAP)
        },
        COMPILER.compileMessage("%{ p, default:'yes' }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", new MessagePartConfig(Map.of("default", new TypedValueString("no"))), EMPTY_MAP)
        },
        COMPILER.compileMessage("%{ p, default:no }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p",
                new MessagePartConfig(Map.of("msg", new TypedValueMessage(COMPILER.compileMessage(" %{q}")))),
                EMPTY_MAP)
        },
        COMPILER.compileMessage("%{ p, msg:' %{q}' }").getMessageParts());

    var mpe = assertThrowsExactly(
        MessageParserException.class,
        () -> COMPILER.compileMessage("%{ p1, msg:yes, msg:no }").getMessageParts());
    assertEquals("duplicate config name msg for parameter 'p1'", mpe.getErrorMessage());
    assertEquals(MESSAGE, mpe.getType());
  }


  @Test
  @DisplayName("Parameter with 'null' configuration key")
  void testParameterWithNullKey()
  {
    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", EMPTY_CONFIG,
                new MessagePartMap(Map.of(MapKeyNull.EQ, new TypedValueString("msg"))))
        },
        COMPILER.compileMessage("%{ p, =null:msg }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", EMPTY_CONFIG, new MessagePartMap(Map.of(
                MapKeyNull.NE, new TypedValueMessage(COMPILER.compileMessage("msg %{n}"))
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
            new ParameterPart("p", EMPTY_CONFIG, new MessagePartMap(Map.of(
                MapKeyEmpty.EQ, new TypedValueString("msg")
            )))
        },
        COMPILER.compileMessage("%{ p, empty:msg }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", EMPTY_CONFIG, new MessagePartMap(Map.of(
                MapKeyEmpty.NE, new TypedValueMessage(COMPILER.compileMessage("msg %{n}"))
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
            new ParameterPart("p", EMPTY_CONFIG, new MessagePartMap(Map.of(
                MapKeyBool.TRUE, new TypedValueString("msg")
            )))
        },
        COMPILER.compileMessage("%{ p, true:msg }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", EMPTY_CONFIG, new MessagePartMap(Map.of(
                MapKeyBool.FALSE, new TypedValueMessage(COMPILER.compileMessage("msg %{n}"))
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
            new ParameterPart("p", EMPTY_CONFIG, new MessagePartMap(Map.of(
                new MapKeyNumber(EQ, 16), new TypedValueString("msg1")
            )))
        },
        COMPILER.compileMessage("%{ p, 16:msg1 }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", EMPTY_CONFIG, new MessagePartMap(Map.of(
                new MapKeyNumber(EQ, -16), new TypedValueMessage(COMPILER.compileMessage(" msg 2 "))
            )))
        },
        COMPILER.compileMessage("%{ p, =-16:\" msg 2 \" }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", EMPTY_CONFIG, new MessagePartMap(Map.of(
                new MapKeyNumber(LT, 1000), new TypedValueString("msg3")
            )))
        },
        COMPILER.compileMessage("%{ p, < 1000:msg3 }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", EMPTY_CONFIG, new MessagePartMap(Map.of(
                new MapKeyNumber(LTE, 0), new TypedValueString("msg4")
            )))
        },
        COMPILER.compileMessage("%{ p,<=0:msg4 }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", EMPTY_CONFIG, new MessagePartMap(Map.of(
                new MapKeyNumber(NE, 1), new TypedValueString("msg5")
            )))
        },
        COMPILER.compileMessage("%{ p,<>1:msg5 }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", EMPTY_CONFIG, new MessagePartMap(Map.of(
                new MapKeyNumber(GT, 123456789), new TypedValueString("msg6")
            )))
        },
        COMPILER.compileMessage("%{ p,>123456789:msg6 }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", EMPTY_CONFIG, new MessagePartMap(Map.of(
                new MapKeyNumber(GTE, -987654321), new TypedValueMessage(COMPILER.compileMessage(" msg 7"))
            )))
        },
        COMPILER.compileMessage("%{ p, >= -987654321:\" msg 7\" }").getMessageParts());
  }


  @Test
  @DisplayName("Parameter with string configuration key")
  void testParameterWithStringKey()
  {
    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", EMPTY_CONFIG, new MessagePartMap(Map.of(
                new MapKeyString(EQ, "AA"), new TypedValueString("msg1")
            )))
        },
        COMPILER.compileMessage("%{ p, 'AA':msg1 }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", EMPTY_CONFIG, new MessagePartMap(Map.of(
                new MapKeyString(EQ, "B"), new TypedValueMessage(COMPILER.compileMessage(" msg 2 "))
            )))
        },
        COMPILER.compileMessage("%{ p, = 'B':\" msg 2 \" }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", EMPTY_CONFIG, new MessagePartMap(Map.of(
                new MapKeyString(LT, "CC"), new TypedValueString("msg3")
            )))
        },
        COMPILER.compileMessage("%{ p, < 'CC':msg3 }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", EMPTY_CONFIG, new MessagePartMap(Map.of(
                new MapKeyString(LTE, "D"), new TypedValueString("msg4")
            )))
        },
        COMPILER.compileMessage("%{ p,<='D':msg4 }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", EMPTY_CONFIG, new MessagePartMap(Map.of(
                new MapKeyString(NE, "EE"), new TypedValueString("msg5")
            )))
        },
        COMPILER.compileMessage("%{ p,<>'EE':msg5 }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", EMPTY_CONFIG, new MessagePartMap(Map.of(
                new MapKeyString(GT, "FFF"), new TypedValueString("msg6")
            )))
        },
        COMPILER.compileMessage("%{ p,>'FFF':msg6 }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", EMPTY_CONFIG, new MessagePartMap(Map.of(
                new MapKeyString(GTE, "GG"), new TypedValueMessage(COMPILER.compileMessage(" msg 7"))
            )))
        },
        COMPILER.compileMessage("%{ p, >= \"GG\":\" msg 7\" }").getMessageParts());
  }


  @Test
  @DisplayName("Parameter with default map value")
  void testParameterWithDefaultMapValue()
  {
    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", EMPTY_CONFIG, new MessagePartMap(singletonMap(
                null, new TypedValueMessage(COMPILER.compileMessage("test"))
            )))
        },
        COMPILER.compileMessage("%{ p, :test }").getMessageParts());

    assertArrayEquals(
        new MessagePart[] {
            new ParameterPart("p", EMPTY_CONFIG, new MessagePartMap(singletonMap(
                null, new TypedValueMessage(COMPILER.compileMessage(" %{n} items"))
            )))
        },
        COMPILER.compileMessage("%{ p, :' %{n} items' }").getMessageParts());
  }


  @Test
  @DisplayName("Template with name only")
  void testTemplateWithNameOnly()
  {
    assertArrayEquals(
        new MessagePart[] { new TemplatePart("pq", false, true, Map.of(), Map.of()) },
        COMPILER.compileMessage("%[pq] ").getMessageParts());

    var mpe = assertThrowsExactly(
        MessageParserException.class,
        () -> COMPILER.compileMessage("%[ '' ]"));
    assertEquals("missing template name at \"'\"", mpe.getErrorMessage());
    assertEquals(MESSAGE, mpe.getType());
  }


  @Test
  @DisplayName("Template with parameter delegate")
  void testTemplateWithParameterDelegate()
  {
    assertArrayEquals(
        new MessagePart[] { new TemplatePart("pq", false, true,
            Map.of(), Map.of("a", "b", "c", "d")) },
        COMPILER.compileMessage("%[pq,a='b',c='d'] ").getMessageParts());

    var mpe = assertThrowsExactly(
        MessageParserException.class,
        () -> COMPILER.compileMessage("%[ xyz, a->b, a->c ]"));
    assertEquals("duplicate template parameter delegate 'a'", mpe.getErrorMessage());
    assertEquals(MESSAGE, mpe.getType());
  }


  @Test
  @DisplayName("Template with default parameter values")
  void testTemplateWithDefaultParameterValues()
  {
    assertArrayEquals(
        new MessagePart[] { new TemplatePart("pq", false, true,
            Map.of(
                "a", TypedValueBool.TRUE,
                "c", new TypedValueString("C")
            ), Map.of()) },
        COMPILER.compileMessage("%[pq,a=true,c='C'] ").getMessageParts());

    var mpe = assertThrowsExactly(
        MessageParserException.class,
        () -> COMPILER.compileMessage("%[ xyz, a=true, a=false ]"));
    assertEquals("duplicate template default parameter 'a'", mpe.getErrorMessage());
    assertEquals(MESSAGE, mpe.getType());
  }
}
