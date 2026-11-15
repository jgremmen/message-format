/*
 * Copyright 2026 Jeroen Gremmen
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
package de.sayayi.lib.message.part;

import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Consumer;
import java.util.stream.Stream;

import static de.sayayi.lib.message.part.TextPartFactory.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


/**
 * Tests {@link TextJoiner}: joining of strings, texts and characters, with the handling of spaces, {@code null} values
 * and empty text.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@DisplayName("Text joiner")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class TextJoinerTest
{
  static Stream<Arguments> pendingSpaces()
  {
    return Stream.of(
        joined("empty joiner", joiner -> {}, emptyText(), emptyText()),
        joined("add(String) ignores null", joiner -> joiner.add((String)null), emptyText(), emptyText()),
        joined("addNoSpace(String) ignores null", joiner -> joiner.addNoSpace((String)null), emptyText(), emptyText()),
        joined("add(char) leading space", joiner -> joiner.add(' ').add('x'), noSpaceText("x"), spacedText(" x")),
        joined("add(char) trailing space", joiner -> joiner.add('x').add(' '), noSpaceText("x"), spacedText("x ")),
        joined("add(char) leading and trailing space", joiner -> joiner.add(' ').add('x').add(' '),
            noSpaceText("x"), spacedText(" x ")),
        joined("add(Text) followed by add(char) space", joiner -> joiner.add(noSpaceText("a")).add(' '),
            noSpaceText("a"), spacedText("a ")));
  }


  @ParameterizedTest(name = "{0}")
  @DisplayName("Pending spaces and ignored values")
  @MethodSource("pendingSpaces")
  void pendingSpacesAndIgnoredValues(String name, Consumer<TextJoiner> operations, Text noSpace, Text spaced)
  {
    var joiner = new TextJoiner();

    operations.accept(joiner);

    assertEquals(noSpace, joiner.asNoSpaceText());
    assertEquals(spaced, joiner.asSpacedText());
    assertEquals(spaced.toString(), joiner.toString());
  }


  private static Arguments joined(String name, Consumer<TextJoiner> operations, Text noSpace, Text spaced) {
    return arguments(name, operations, noSpace, spaced);
  }


  @Test
  @DisplayName("Join 'String' instances")
  void strings()
  {
    var joiner = new TextJoiner();

    joiner
        .add(" Test ")
        .add((String)null)
        .add(" ")
        .add("1 ");

    assertJoined(joiner, "Test 1", " Test 1 ");
  }


  @Test
  @DisplayName("Join 'Text' instances")
  void texts()
  {
    var joiner = new TextJoiner();

    joiner
        .add(noSpaceText("e1"))
        .add(spacedText(", "))
        .add(noSpaceText("e2"));

    assertEquals(noSpaceText("e1, e2"), joiner.asNoSpaceText());
  }


  @Test
  @DisplayName("add(String) collapses consecutive spaces into a single separator")
  void collapseSpaces()
  {
    var joiner = new TextJoiner();

    joiner.add("a     b   c");

    assertJoined(joiner, "a b c", "a b c");
  }


  @Test
  @DisplayName("add(String) preserves leading and trailing spaces")
  void addPreservesSurroundingSpaces()
  {
    var joiner = new TextJoiner();

    joiner.add("  hello  ");

    assertJoined(joiner, "hello", " hello ");
  }


  @Test
  @DisplayName("add(String) with blank string only records a pending space")
  void addBlank()
  {
    var joiner = new TextJoiner();

    joiner.add("   ");

    // pending trailing space is emitted by asSpacedText, discarded by asNoSpaceText
    assertEquals(setSpaces(emptyText(), false, true), joiner.asSpacedText());
    assertEquals(emptyText(), joiner.asNoSpaceText());
  }


  @Test
  @DisplayName("addNoSpace(String) strips leading and trailing spaces")
  void addNoSpaceString()
  {
    var joiner = new TextJoiner();

    joiner
        .addNoSpace("  a  ")
        .addNoSpace("  b  ");

    // no space was preserved between the two additions
    assertJoined(joiner, "ab", "ab");
  }


  @Test
  @DisplayName("addNoSpace(Text) strips the surrounding spaces of the text")
  void addNoSpaceText()
  {
    var joiner = new TextJoiner();

    joiner
        .addNoSpace(spacedText("  a  "))
        .add(noSpaceText("b"));

    // the trailing space of the first text was stripped, so 'a' and 'b' stick together
    assertEquals(noSpaceText("ab"), joiner.asNoSpaceText());
  }


  @Test
  @DisplayName("add(char[]) adds all characters and collapses spaces")
  void addCharArray()
  {
    var joiner = new TextJoiner();

    joiner.add("a   b  ".toCharArray());

    assertJoined(joiner, "a b", "a b ");
  }


  @Test
  @DisplayName("add(char) with a non-space character appends it directly")
  void addChar()
  {
    var joiner = new TextJoiner();

    joiner.add('a').add('b').add('c');

    assertJoined(joiner, "abc", "abc");
  }


  @Test
  @DisplayName("add(char) with a space character records a pending separator space")
  void addSpaceChar()
  {
    var joiner = new TextJoiner();

    joiner.add('a').add(' ').add('b');

    assertJoined(joiner, "a b", "a b");
  }


  @Test
  @DisplayName("add(char) collapses multiple space characters into one")
  void addMultipleSpaceChars()
  {
    var joiner = new TextJoiner();

    joiner.add('a').add(' ').add(' ').add(' ').add('b');

    assertEquals(noSpaceText("a b"), joiner.asNoSpaceText());
  }


  @Test
  @DisplayName("add(char) treats unicode space separators as spaces")
  void addUnicodeSpaceChar()
  {
    var joiner = new TextJoiner();

    // non-breaking space, tab and carriage return are all space characters
    joiner.add('a').add('\u00a0').add('\t').add('\r').add('b');

    assertJoined(joiner, "a b", "a b");
  }


  @Test
  @DisplayName("add(char) keeps a newline as a literal character")
  void addNewlineChar()
  {
    var joiner = new TextJoiner();

    joiner.add('a').add('\n').add('b');

    assertEquals(noSpaceText("a\nb"), joiner.asNoSpaceText());
  }


  @Test
  @DisplayName("add(Text) honours the leading space of the first text")
  void addTextLeadingSpace()
  {
    var joiner = new TextJoiner();

    joiner.add(spacedText(" x"));

    assertJoined(joiner, "x", " x");
  }


  @Test
  @DisplayName("add(Text) honours the trailing space of the last text")
  void addTextTrailingSpace()
  {
    var joiner = new TextJoiner();

    joiner.add(spacedText("x "));

    assertJoined(joiner, "x", "x ");
  }


  @Test
  @DisplayName("add(Text) inserts a single space between space-adjacent texts")
  void addTextAdjacentSpaces()
  {
    var joiner = new TextJoiner();

    joiner
        .add(spacedText("a "))
        .add(spacedText(" b"));

    assertJoined(joiner, "a b", "a b");
  }


  @Test
  @DisplayName("add(Text) with empty text only accumulates the pending space state")
  void addEmptyText()
  {
    var joiner = new TextJoiner();

    joiner
        .add(noSpaceText("a"))
        .add(spacedText(" "))
        .add(noSpaceText("b"));

    assertJoined(joiner, "a b", "a b");
  }


  @Test
  @DisplayName("add(Text) with null text only accumulates the pending space state")
  void addNullText()
  {
    var joiner = new TextJoiner();

    joiner
        .add(noSpaceText("a"))
        .add(setSpaces(nullText(), false, true))
        .add(noSpaceText("b"));

    assertEquals(noSpaceText("a b"), joiner.asNoSpaceText());
  }


  @Test
  @DisplayName("add(Text) with a space-around empty text carries the pending space")
  void addSpaceAroundEmptyText()
  {
    var joiner = new TextJoiner();

    joiner
        .add(noSpaceText("a"))
        .add(setSpaces(emptyText(), true, true))
        .add(noSpaceText("b"));

    assertJoined(joiner, "a b", "a b");
  }


  private static void assertJoined(@NotNull TextJoiner joiner, @NotNull String noSpace, @NotNull String spaced)
  {
    assertEquals(noSpaceText(noSpace), joiner.asNoSpaceText());
    assertEquals(spacedText(spaced), joiner.asSpacedText());
  }
}
