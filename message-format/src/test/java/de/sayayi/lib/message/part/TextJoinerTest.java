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
import lombok.val;
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
  void testPendingSpaces(String name, Consumer<TextJoiner> operations, Text noSpace, Text spaced)
  {
    val joiner = new TextJoiner();

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
  void testStrings()
  {
    val joiner = new TextJoiner();

    joiner
        .add(" Test ")
        .add((String)null)
        .add(" ")
        .add("1 ");

    assertEquals(noSpaceText("Test 1"), joiner.asNoSpaceText());
    assertEquals(spacedText(" Test 1 "), joiner.asSpacedText());
  }


  @Test
  @DisplayName("Join 'Text' instances")
  void testTexts()
  {
    val joiner = new TextJoiner();

    joiner
        .add(noSpaceText("e1"))
        .add(spacedText(", "))
        .add(noSpaceText("e2"));

    assertEquals(noSpaceText("e1, e2"), joiner.asNoSpaceText());
  }


  @Test
  @DisplayName("add(String) collapses consecutive spaces into a single separator")
  void testCollapseSpaces()
  {
    val joiner = new TextJoiner();

    joiner.add("a     b   c");

    assertEquals(noSpaceText("a b c"), joiner.asNoSpaceText());
    assertEquals(spacedText("a b c"), joiner.asSpacedText());
  }


  @Test
  @DisplayName("add(String) preserves leading and trailing spaces")
  void testAddPreservesSurroundingSpaces()
  {
    val joiner = new TextJoiner();

    joiner.add("  hello  ");

    assertEquals(spacedText(" hello "), joiner.asSpacedText());
    assertEquals(noSpaceText("hello"), joiner.asNoSpaceText());
  }


  @Test
  @DisplayName("add(String) with blank string only records a pending space")
  void testAddBlank()
  {
    val joiner = new TextJoiner();

    joiner.add("   ");

    // pending trailing space is emitted by asSpacedText, discarded by asNoSpaceText
    assertEquals(setSpaces(emptyText(), false, true), joiner.asSpacedText());
    assertEquals(emptyText(), joiner.asNoSpaceText());
  }


  @Test
  @DisplayName("addNoSpace(String) strips leading and trailing spaces")
  void testAddNoSpaceString()
  {
    val joiner = new TextJoiner();

    joiner
        .addNoSpace("  a  ")
        .addNoSpace("  b  ");

    // no space was preserved between the two additions
    assertEquals(noSpaceText("ab"), joiner.asNoSpaceText());
    assertEquals(spacedText("ab"), joiner.asSpacedText());
  }


  @Test
  @DisplayName("addNoSpace(Text) strips the surrounding spaces of the text")
  void testAddNoSpaceText()
  {
    val joiner = new TextJoiner();

    joiner
        .addNoSpace(spacedText("  a  "))
        .add(noSpaceText("b"));

    // the trailing space of the first text was stripped, so 'a' and 'b' stick together
    assertEquals(noSpaceText("ab"), joiner.asNoSpaceText());
  }


  @Test
  @DisplayName("add(char[]) adds all characters and collapses spaces")
  void testAddCharArray()
  {
    val joiner = new TextJoiner();

    joiner.add("a   b  ".toCharArray());

    assertEquals(noSpaceText("a b"), joiner.asNoSpaceText());
    assertEquals(spacedText("a b "), joiner.asSpacedText());
  }


  @Test
  @DisplayName("add(char) with a non-space character appends it directly")
  void testAddChar()
  {
    val joiner = new TextJoiner();

    joiner.add('a').add('b').add('c');

    assertEquals(noSpaceText("abc"), joiner.asNoSpaceText());
    assertEquals(spacedText("abc"), joiner.asSpacedText());
  }


  @Test
  @DisplayName("add(char) with a space character records a pending separator space")
  void testAddSpaceChar()
  {
    val joiner = new TextJoiner();

    joiner.add('a').add(' ').add('b');

    assertEquals(noSpaceText("a b"), joiner.asNoSpaceText());
    assertEquals(spacedText("a b"), joiner.asSpacedText());
  }


  @Test
  @DisplayName("add(char) collapses multiple space characters into one")
  void testAddMultipleSpaceChars()
  {
    val joiner = new TextJoiner();

    joiner.add('a').add(' ').add(' ').add(' ').add('b');

    assertEquals(noSpaceText("a b"), joiner.asNoSpaceText());
  }


  @Test
  @DisplayName("add(char) treats unicode space separators as spaces")
  void testAddUnicodeSpaceChar()
  {
    val joiner = new TextJoiner();

    // non-breaking space, tab and carriage return are all space characters
    joiner.add('a').add('\u00a0').add('\t').add('\r').add('b');

    assertEquals(noSpaceText("a b"), joiner.asNoSpaceText());
    assertEquals(spacedText("a b"), joiner.asSpacedText());
  }


  @Test
  @DisplayName("add(char) keeps a newline as a literal character")
  void testAddNewlineChar()
  {
    val joiner = new TextJoiner();

    joiner.add('a').add('\n').add('b');

    assertEquals(noSpaceText("a\nb"), joiner.asNoSpaceText());
  }


  @Test
  @DisplayName("add(Text) honours the leading space of the first text")
  void testAddTextLeadingSpace()
  {
    val joiner = new TextJoiner();

    joiner.add(spacedText(" x"));

    assertEquals(spacedText(" x"), joiner.asSpacedText());
    assertEquals(noSpaceText("x"), joiner.asNoSpaceText());
  }


  @Test
  @DisplayName("add(Text) honours the trailing space of the last text")
  void testAddTextTrailingSpace()
  {
    val joiner = new TextJoiner();

    joiner.add(spacedText("x "));

    assertEquals(spacedText("x "), joiner.asSpacedText());
    assertEquals(noSpaceText("x"), joiner.asNoSpaceText());
  }


  @Test
  @DisplayName("add(Text) inserts a single space between space-adjacent texts")
  void testAddTextAdjacentSpaces()
  {
    val joiner = new TextJoiner();

    joiner
        .add(spacedText("a "))
        .add(spacedText(" b"));

    assertEquals(noSpaceText("a b"), joiner.asNoSpaceText());
    assertEquals(spacedText("a b"), joiner.asSpacedText());
  }


  @Test
  @DisplayName("add(Text) with empty text only accumulates the pending space state")
  void testAddEmptyText()
  {
    val joiner = new TextJoiner();

    joiner
        .add(noSpaceText("a"))
        .add(spacedText(" "))
        .add(noSpaceText("b"));

    assertEquals(noSpaceText("a b"), joiner.asNoSpaceText());
    assertEquals(spacedText("a b"), joiner.asSpacedText());
  }


  @Test
  @DisplayName("add(Text) with null text only accumulates the pending space state")
  void testAddNullText()
  {
    val joiner = new TextJoiner();

    joiner
        .add(noSpaceText("a"))
        .add(setSpaces(nullText(), false, true))
        .add(noSpaceText("b"));

    assertEquals(noSpaceText("a b"), joiner.asNoSpaceText());
  }


  @Test
  @DisplayName("add(Text) with a space-around empty text carries the pending space")
  void testAddSpaceAroundEmptyText()
  {
    val joiner = new TextJoiner();

    joiner
        .add(noSpaceText("a"))
        .add(setSpaces(emptyText(), true, true))
        .add(noSpaceText("b"));

    assertEquals(noSpaceText("a b"), joiner.asNoSpaceText());
    assertEquals(spacedText("a b"), joiner.asSpacedText());
  }
}
