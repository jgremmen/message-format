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

import de.sayayi.lib.message.MessageBuilder;
import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.exception.MessageParserException;
import de.sayayi.lib.message.internal.part.config.MessagePartConfig;
import de.sayayi.lib.message.internal.part.map.MessagePartMap;
import de.sayayi.lib.message.internal.part.map.key.MapKeyBool;
import de.sayayi.lib.message.internal.part.map.key.MapKeyEmpty;
import de.sayayi.lib.message.internal.part.map.key.MapKeyNull;
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
import static de.sayayi.lib.message.part.TextPartFactory.*;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Message compiler test cases.
 *
 * @author Jeroen Gremmen
 * @since 0.20.0
 */
@DisplayName("Message compiler")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class MessageCompilerTest
{
  private static MessageCompiler COMPILER;


  @BeforeAll
  static void init() {
    COMPILER = new MessageCompiler(new MessageFactory(LRUMessagePartNormalizer.create(64)));
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
    assertEquals("duplicate config name 'msg' for parameter 'p1'", mpe.getErrorMessage());
    assertEquals(MESSAGE, mpe.getType());
  }


  @Test
  @DisplayName("Parameter with 'null' configuration key")
  void testParameterWithNullKey()
  {
    assertArrayEquals(
        MessageBuilder.create().parameter("p").mapNull().message("msg").build().getMessageParts(),
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
                MapKeyEmpty.EQ, new TypedValueMessage(COMPILER.compileMessage("msg"))
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
        MessageBuilder.create().parameter("p").mapBool(true).message("msg").build().getMessageParts(),
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
        MessageBuilder.create().parameter("p").mapNumber(16).message("msg1").build().getMessageParts(),
        COMPILER.compileMessage("%{ p, 16:msg1 }").getMessageParts());

    assertArrayEquals(
        MessageBuilder.create().parameter("p").mapNumber(-16).message(" msg 2 ").build().getMessageParts(),
        COMPILER.compileMessage("%{ p, =-16:\" msg 2 \" }").getMessageParts());

    assertArrayEquals(
        MessageBuilder.create().parameter("p").mapNumber(1000).lt().message("msg3").build().getMessageParts(),
        COMPILER.compileMessage("%{ p, < 1000:msg3 }").getMessageParts());

    assertArrayEquals(
        MessageBuilder.create().parameter("p").mapNumber(0).lte().message("msg4").build().getMessageParts(),
        COMPILER.compileMessage("%{ p,<=0:msg4 }").getMessageParts());

    assertArrayEquals(
        MessageBuilder.create().parameter("p").mapNumber(1).ne().message("msg5").build().getMessageParts(),
        COMPILER.compileMessage("%{ p,<>1:msg5 }").getMessageParts());

    assertArrayEquals(
        MessageBuilder.create().parameter("p").mapNumber(123456789).gt().message("msg6").build().getMessageParts(),
        COMPILER.compileMessage("%{ p,>123456789:msg6 }").getMessageParts());

    assertArrayEquals(
        MessageBuilder.create().parameter("p").mapNumber(-987654321).gte().message(" msg 7").build().getMessageParts(),
        COMPILER.compileMessage("%{ p, >= -987654321:\" msg 7\" }").getMessageParts());
  }


  @Test
  @DisplayName("Parameter with string configuration key")
  void testParameterWithStringKey()
  {
    assertArrayEquals(
        MessageBuilder.create().parameter("p").mapString("AA").message("msg1").build().getMessageParts(),
        COMPILER.compileMessage("%{ p, 'AA':msg1 }").getMessageParts());

    assertArrayEquals(
        MessageBuilder.create().parameter("p").mapString("B").message(" msg 2 ").build().getMessageParts(),
        COMPILER.compileMessage("%{ p, = 'B':\" msg 2 \" }").getMessageParts());

    assertArrayEquals(
        MessageBuilder.create().parameter("p").mapString("CC").lt().message("msg3").build().getMessageParts(),
        COMPILER.compileMessage("%{ p, < 'CC':msg3 }").getMessageParts());

    assertArrayEquals(
        MessageBuilder.create().parameter("p").mapString("D").lte().message("msg4").build().getMessageParts(),
        COMPILER.compileMessage("%{ p,<='D':msg4 }").getMessageParts());

    assertArrayEquals(
        MessageBuilder.create().parameter("p").mapString("EE").ne().message("msg5").build().getMessageParts(),
        COMPILER.compileMessage("%{ p,<>'EE':msg5 }").getMessageParts());

    assertArrayEquals(
        MessageBuilder.create().parameter("p").mapString("FFF").gt().message("msg6").build().getMessageParts(),
        COMPILER.compileMessage("%{ p,>'FFF':msg6 }").getMessageParts());

    assertArrayEquals(
        MessageBuilder.create().parameter("p").mapString("GG").gte().message(" msg 7").build().getMessageParts(),
        COMPILER.compileMessage("%{ p, >= \"GG\":\" msg 7\" }").getMessageParts());
  }


  @Test
  @DisplayName("Parameter with default map value")
  void testParameterWithDefaultMapValue()
  {
    assertArrayEquals(
        MessageBuilder
            .create()
            .parameter("p").mapNumber(4).message("four").mapDefault().message("test")
            .build()
            .getMessageParts(),
        COMPILER.compileMessage("%{ p, 4:four, :test }").getMessageParts());

    var mpe = assertThrowsExactly(
        MessageParserException.class,
        () -> COMPILER.compileMessage("%{ p, :' %{n} items' }"));
    assertEquals("default map entry can only be used in combination with other map entries", mpe.getErrorMessage());
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
