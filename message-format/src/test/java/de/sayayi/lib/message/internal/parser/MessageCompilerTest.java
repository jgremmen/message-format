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
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.util.Map;

import static de.sayayi.lib.message.internal.part.config.MessagePartConfig.EMPTY_CONFIG;
import static de.sayayi.lib.message.internal.part.map.MessagePartMap.EMPTY_MAP;
import static de.sayayi.lib.message.part.TextPartFactory.*;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests {@link MessageCompiler}: compilation of text, parameters, configuration values, map keys and post formatters
 * into message parts.
 *
 * @author Jeroen Gremmen
 * @since 0.20.0
 */
@DisplayName("Message compiler")
@TestMethodOrder(MethodOrderer.DisplayName.class)
@SuppressWarnings("UnnecessaryUnicodeEscape")
final class MessageCompilerTest
{
  private static MessageCompiler COMPILER;
  private static MessageFactory MESSAGE_FACTORY;


  @BeforeAll
  static void init()
  {
    MESSAGE_FACTORY = new MessageFactory(LRUMessagePartNormalizer.create(64));
    COMPILER = new MessageCompiler(MESSAGE_FACTORY);
  }


  @Test
  @DisplayName("Text only")
  void textOnly()
  {
    assertCompiles("", emptyText());
    assertCompiles("a\\u000ab ", addSpaces(noSpaceText("a\nb"), false, true));
    assertCompiles("          ", addSpaces(emptyText(), true, true));
    assertCompiles("  \\'\\\\", addSpaces(noSpaceText("'\\"), true, false));
    assertCompiles("this  is  a  text", noSpaceText("this is a text"));
  }


  @Test
  @DisplayName("Text with lone backslash")
  void textLoneBackslash()
  {
    assertCompiles("C:\\temp \\ x\\", noSpaceText("C:\\temp \\ x\\"));
  }


  @Test
  @DisplayName("Text with supplementary characters")
  void textSupplementaryCharacters()
  {
    assertCompiles("Smile 😀 now", noSpaceText("Smile 😀 now"));
    assertCompiles("😀𝔸𐌀", noSpaceText("😀𝔸𐌀"));
    assertCompiles("  😀   😀  ", addSpaces(noSpaceText("😀 😀"), true, true));
  }


  @Test
  @DisplayName("Text with combining marks, format and private use characters")
  void textCombiningMarksAndFormatCharacters()
  {
    // NFD-normalised Latin
    assertCompiles("Cafe\u0301", noSpaceText("Cafe\u0301"));

    // Indic scripts (virama U+094D, vowel sign U+093E)
    assertCompiles("नमस्ते दुनिया", noSpaceText("नमस्ते दुनिया"));

    // Thai and Arabic with harakat
    assertCompiles("สวัสดี مَرْحَبًا", noSpaceText("สวัสดี مَرْحَبًا"));

    // Emoji with variation selector and ZWJ sequence
    assertCompiles(
        "\u2764\ufe0f \ud83d\udc68\u200d\ud83d\udc69\u200d\ud83d\udc67",
        noSpaceText("\u2764\ufe0f \ud83d\udc68\u200d\ud83d\udc69\u200d\ud83d\udc67"));

    // ZWNJ, bidi marks and private use characters
    assertCompiles("می\u200cخواهم \u200ea\u200f \ue000", noSpaceText("می\u200cخواهم \u200ea\u200f \ue000"));

    // Combining mark following a parameter
    final var parts = COMPILER.compileMessage("%{p}\u0301").getMessageParts();
    assertEquals(2, parts.length);
    assertEquals(noSpaceText("\u0301"), parts[1]);
  }


  @Test
  @DisplayName("Text with C1 control characters")
  void textC1ControlCharacters()
  {
    assertCompiles("a\u007f\u0085\u009fb", noSpaceText("ab"));
    assertCompiles("%{\u0085p\u009f}", new ParameterPart("p", false, false));
  }


  @Test
  @DisplayName("Unquoted values with combining marks and joiners")
  void unquotedValueCombiningMarks()
  {
    for(var value: new String[] { "नमस्ते", "cafe\u0301-au-lait", "می\u200cخواهم" })
    {
      assertCompiles(
          "%{ p, default:" + value + " }",
          new ParameterPart("p", new MessagePartConfig(Map.of("default", new TypedValueString(MESSAGE_FACTORY, value))), EMPTY_MAP));
    }

    assertThrows(MessageParserException.class, () -> COMPILER.compileMessage("%{ p, default:a\u200eb }"));
  }


  @Test
  @DisplayName("Parameter with name only")
  void parameterWithNameOnly()
  {
    assertCompiles("%{ p } ", new ParameterPart("p", false, true));
    assertCompiles(" %{ empty } ", new ParameterPart("empty", true, true));
    assertCompiles("%{ test-id }", new ParameterPart("test-id", false, false));
  }



  @Test
  @DisplayName("Parameter format")
  void parameterFormat()
  {
    assertCompiles("%{ p, format:bool }", new ParameterPart("p", "bool", false, false, EMPTY_CONFIG, EMPTY_MAP));
    assertCompiles("%{p,format:null}", new ParameterPart("p", "null", false, false, EMPTY_CONFIG, EMPTY_MAP));
  }



  @Test
  @DisplayName("Parameter with named configuration")
  void parameterWithNamedConfiguration()
  {
    assertCompiles(
        "%{ p, charset:false }",
        new ParameterPart("p", new MessagePartConfig(Map.of("charset", TypedValueBool.FALSE)), EMPTY_MAP));

    assertCompiles(
        "%{ p, charset:true }",
        new ParameterPart("p", new MessagePartConfig(Map.of("charset", TypedValueBool.TRUE)), EMPTY_MAP));

    assertCompiles(
        "%{ p, clip:-5 }",
        new ParameterPart("p", new MessagePartConfig(Map.of("clip", new TypedValueNumber(-5))), EMPTY_MAP));

    assertCompiles(
        "%{ p, default:'yes' }",
        new ParameterPart("p", new MessagePartConfig(Map.of("default", new TypedValueString(MESSAGE_FACTORY, "yes"))), EMPTY_MAP));

    assertCompiles(
        "%{ p, default:no }",
        new ParameterPart("p", new MessagePartConfig(Map.of("default", new TypedValueString(MESSAGE_FACTORY, "no"))), EMPTY_MAP));

    assertCompiles(
        "%{ p, msg:' %{q}' }",
        new ParameterPart("p",
            new MessagePartConfig(Map.of("msg", new TypedValueMessage(COMPILER.compileMessage(" %{q}")))),
            EMPTY_MAP));
  }


  @Test
  @DisplayName("Numeric configuration values accept the full long range")
  void numericConfigurationLongRange()
  {
    assertCompiles(
        "%{p,min:-9223372036854775808,max:9223372036854775807}",
        new ParameterPart("p", new MessagePartConfig(Map.of(
            "min", new TypedValueNumber(Long.MIN_VALUE),
            "max", new TypedValueNumber(Long.MAX_VALUE)
        )), EMPTY_MAP));
  }


  @Test
  @DisplayName("Parameter with 'null' configuration key")
  void parameterWithNullKey()
  {
    assertCompiles(
        "%{ p, =null:msg }",
        MessageBuilder.create().parameter("p").mapNull().message("msg").build().getMessageParts());

    assertCompiles(
        "%{ p, !null:'msg %{n}' }",
        new ParameterPart("p", EMPTY_CONFIG, new MessagePartMap(Map.of(
            MapKeyNull.NE, new TypedValueMessage(COMPILER.compileMessage("msg %{n}"))
        ))));
  }


  @Test
  @DisplayName("Parameter with 'empty' configuration key")
  void parameterWithEmptyKey()
  {
    assertCompiles(
        "%{ p, empty:msg }",
        new ParameterPart("p", EMPTY_CONFIG, new MessagePartMap(Map.of(
            MapKeyEmpty.EQ, new TypedValueMessage(COMPILER.compileMessage("msg"))
        ))));

    assertCompiles(
        "%{ p, <>empty:'msg %{n}' }",
        new ParameterPart("p", EMPTY_CONFIG, new MessagePartMap(Map.of(
            MapKeyEmpty.NE, new TypedValueMessage(COMPILER.compileMessage("msg %{n}"))
        ))));
  }


  @Test
  @DisplayName("Parameter with boolean configuration key")
  void parameterWithBooleanKey()
  {
    assertCompiles(
        "%{ p, true:msg }",
        MessageBuilder.create().parameter("p").mapBool(true).message("msg").build().getMessageParts());

    assertCompiles(
        "%{ p, false:'msg %{n}' }",
        new ParameterPart("p", EMPTY_CONFIG, new MessagePartMap(Map.of(
            MapKeyBool.FALSE, new TypedValueMessage(COMPILER.compileMessage("msg %{n}"))
        ))));
  }


  @Test
  @DisplayName("Parameter with number configuration key")
  void parameterWithNumberKey()
  {
    assertCompiles(
        "%{ p, 16:msg1 }",
        MessageBuilder.create().parameter("p").mapNumber(16).message("msg1").build().getMessageParts());

    assertCompiles(
        "%{ p, =-16:\" msg 2 \" }",
        MessageBuilder.create().parameter("p").mapNumber(-16).message(" msg 2 ").build().getMessageParts());

    assertCompiles(
        "%{ p, < 1000:msg3 }",
        MessageBuilder.create().parameter("p").mapNumber(1000).lt().message("msg3").build().getMessageParts());

    assertCompiles(
        "%{ p,<=0:msg4 }",
        MessageBuilder.create().parameter("p").mapNumber(0).lte().message("msg4").build().getMessageParts());

    assertCompiles(
        "%{ p,<>1:msg5 }",
        MessageBuilder.create().parameter("p").mapNumber(1).ne().message("msg5").build().getMessageParts());

    assertCompiles(
        "%{ p,>123456789:msg6 }",
        MessageBuilder.create().parameter("p").mapNumber(123456789).gt().message("msg6").build().getMessageParts());

    assertCompiles(
        "%{ p, >= -987654321:\" msg 7\" }",
        MessageBuilder.create().parameter("p").mapNumber(-987654321).gte().message(" msg 7").build().getMessageParts());
  }


  @Test
  @DisplayName("Parameter with string configuration key")
  void parameterWithStringKey()
  {
    assertCompiles(
        "%{ p, 'AA':msg1 }",
        MessageBuilder.create().parameter("p").mapString("AA").message("msg1").build().getMessageParts());

    assertCompiles(
        "%{ p, = 'B':\" msg 2 \" }",
        MessageBuilder.create().parameter("p").mapString("B").message(" msg 2 ").build().getMessageParts());

    assertCompiles(
        "%{ p, < 'CC':msg3 }",
        MessageBuilder.create().parameter("p").mapString("CC").lt().message("msg3").build().getMessageParts());

    assertCompiles(
        "%{ p,<='D':msg4 }",
        MessageBuilder.create().parameter("p").mapString("D").lte().message("msg4").build().getMessageParts());

    assertCompiles(
        "%{ p,<>'EE':msg5 }",
        MessageBuilder.create().parameter("p").mapString("EE").ne().message("msg5").build().getMessageParts());

    assertCompiles(
        "%{ p,>'FFF':msg6 }",
        MessageBuilder.create().parameter("p").mapString("FFF").gt().message("msg6").build().getMessageParts());

    assertCompiles(
        "%{ p, >= \"GG\":\" msg 7\" }",
        MessageBuilder.create().parameter("p").mapString("GG").gte().message(" msg 7").build().getMessageParts());
  }


  @Test
  @DisplayName("Parameter with default map value")
  void parameterWithDefaultMapValue()
  {
    assertCompiles(
        "%{ p, 4:four, :test }",
        MessageBuilder
            .create()
            .parameter("p").mapNumber(4).message("four").mapDefault().message("test")
            .build()
            .getMessageParts());
  }


  @Test
  @DisplayName("Template with name only")
  void templateWithNameOnly()
  {
    assertCompiles("%[pq] ", new TemplatePart("pq", false, true, Map.of(), Map.of()));
  }


  @Test
  @DisplayName("Template with parameter delegate")
  void templateWithParameterDelegate()
  {
    assertCompiles(
        "%[pq,a->b,c->d] ",
        new MessagePart[] { new TemplatePart("pq", false, true,
            Map.of(), Map.of("a", "b", "c", "d")) });
  }


  @Test
  @DisplayName("Template with default parameter values")
  void templateWithDefaultParameterValues()
  {
    assertCompiles(
        "%[pq,a=true,c='C'] ",
        new MessagePart[] { new TemplatePart("pq", false, true,
            Map.of(
                "a", TypedValueBool.TRUE,
                "c", new TypedValueString(MESSAGE_FACTORY, "C")
            ), Map.of()) });
  }


  private static void assertCompiles(@NotNull String formatString, @NotNull MessagePart... expectedParts) {
    assertArrayEquals(expectedParts, COMPILER.compileMessage(formatString).getMessageParts());
  }
}
