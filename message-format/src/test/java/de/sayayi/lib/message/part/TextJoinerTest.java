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

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static de.sayayi.lib.message.part.TextPartFactory.*;
import static org.junit.jupiter.api.Assertions.*;


@DisplayName("Text joiner")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class TextJoinerTest
{
  @Test
  @DisplayName("Empty joiner produces empty text")
  void testEmpty()
  {
    val joiner = new TextJoiner();

    assertEquals(emptyText(), joiner.asNoSpaceText());
    assertEquals(emptyText(), joiner.asSpacedText());
    assertEquals(joiner.asSpacedText().toString(), joiner.toString());
  }


  @Test
  @DisplayName("Join 'String' instances")
  void testStrings()
  {
    val joiner = new TextJoiner();

    joiner
        .addWithSpace(" Test ")
        .addWithSpace(null)
        .addWithSpace(" ")
        .addWithSpace("1 ");

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
  @DisplayName("addWithSpace collapses consecutive spaces into a single separator")
  void testCollapseSpaces()
  {
    val joiner = new TextJoiner();

    joiner.addWithSpace("a     b   c");

    assertEquals(noSpaceText("a b c"), joiner.asNoSpaceText());
    assertEquals(spacedText("a b c"), joiner.asSpacedText());
  }


  @Test
  @DisplayName("addWithSpace preserves leading and trailing spaces")
  void testAddWithSpacePreservesSurroundingSpaces()
  {
    val joiner = new TextJoiner();

    joiner.addWithSpace("  hello  ");

    assertEquals(spacedText(" hello "), joiner.asSpacedText());
    assertEquals(noSpaceText("hello"), joiner.asNoSpaceText());
  }


  @Test
  @DisplayName("addWithSpace ignores null argument")
  void testAddWithSpaceNull()
  {
    val joiner = new TextJoiner();

    joiner.addWithSpace(null);

    assertEquals(emptyText(), joiner.asSpacedText());
    assertEquals(emptyText(), joiner.asNoSpaceText());
  }


  @Test
  @DisplayName("addWithSpace with blank string only records a pending space")
  void testAddWithSpaceBlank()
  {
    val joiner = new TextJoiner();

    joiner.addWithSpace("   ");

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
  @DisplayName("addNoSpace(String) ignores null argument")
  void testAddNoSpaceStringNull()
  {
    val joiner = new TextJoiner();

    joiner.addNoSpace((String)null);

    assertEquals(emptyText(), joiner.asSpacedText());
    assertEquals(emptyText(), joiner.asNoSpaceText());
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
  @DisplayName("add(char) treats a leading space as a pending leading space")
  void testAddLeadingSpaceChar()
  {
    val joiner = new TextJoiner();

    joiner.add(' ').add('x');

    // the pending leading space is flushed in front of the first content character
    assertEquals(noSpaceText("x"), joiner.asNoSpaceText());
    assertEquals(spacedText(" x"), joiner.asSpacedText());
  }


  @Test
  @DisplayName("add(char) treats a trailing space as a pending trailing space")
  void testAddTrailingSpaceChar()
  {
    val joiner = new TextJoiner();

    joiner.add('x').add(' ');

    assertEquals(noSpaceText("x"), joiner.asNoSpaceText());
    assertEquals(spacedText("x "), joiner.asSpacedText());
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


  @Test
  @DisplayName("asNoSpaceText discards a pending trailing space")
  void testNoSpaceDiscardsTrailingSpace()
  {
    val joiner = new TextJoiner();

    joiner.add(noSpaceText("a")).add(' ');

    assertEquals(noSpaceText("a"), joiner.asNoSpaceText());
    assertFalse(joiner.asNoSpaceText().isSpaceAfter());
  }


  @Test
  @DisplayName("asSpacedText keeps both a leading and a trailing space")
  void testSpacedTextLeadingAndTrailingSpace()
  {
    val joiner = new TextJoiner();

    joiner.add(' ').add('x').add(' ');

    val text = joiner.asSpacedText();

    assertEquals(spacedText(" x "), text);
    assertTrue(text.isSpaceBefore());
    assertTrue(text.isSpaceAfter());
  }


  @Test
  @DisplayName("toString delegates to asSpacedText")
  void testToString()
  {
    val joiner = new TextJoiner();

    joiner.addWithSpace(" a b ");

    // toString delegates to asSpacedText().toString() (the debug representation)
    assertEquals(joiner.asSpacedText().toString(), joiner.toString());
    assertEquals(spacedText(" a b ").toString(), joiner.toString());
  }


  @Test
  @DisplayName("All add methods return the same joiner instance")
  void testFluentReturnsSameInstance()
  {
    val joiner = new TextJoiner();

    assertSame(joiner, joiner.add('a'));
    assertSame(joiner, joiner.add(noSpaceText("b")));
    assertSame(joiner, joiner.addNoSpace("c"));
    assertSame(joiner, joiner.addNoSpace(noSpaceText("d")));
    assertSame(joiner, joiner.addWithSpace("e"));
  }


  @Test
  @DisplayName("Mixing chars, strings and texts joins them consistently")
  void testMixedContent()
  {
    val joiner = new TextJoiner();

    joiner
        .addWithSpace("   one ")
        .add(noSpaceText("two"))
        .add(',')
        .add(' ')
        .addNoSpace("  three  ")
        .add(spacedText(" four "));

    assertEquals(noSpaceText("one two, three four"), joiner.asNoSpaceText());
    assertEquals(spacedText(" one two, three four "), joiner.asSpacedText());
  }
}
