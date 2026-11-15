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
package de.sayayi.lib.message.util;

import de.sayayi.lib.message.FormatStringSerializer;
import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageBuilder;
import de.sayayi.lib.message.MessageSupportFactory;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static de.sayayi.lib.message.util.AssertionTestSupport.assertNullRejected;
import static de.sayayi.lib.message.util.MessageUtil.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newOutputStream;
import static java.nio.file.Files.writeString;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests {@link MessageUtil}: validation of kebab case and lower camel case names, trimming and collapsing of spaces,
 * quoting of text in format strings and detection of pack files.
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
@DisplayName("Message utils")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class MessageUtilTest
{
  @ParameterizedTest(name = "\"{0}\"")
  @DisplayName("Name predicates")
  @CsvSource(delimiter = '|', textBlock = """
      # name                           | kebab | camel | kebab or camel | name

      # kebab and lower camel case
      word                             | true  | true  | true           | true
      a                                | true  | true  | true           | true
      test                             | true  | true  | true           | true
      test123                          | true  | true  | true           | true
      abc123                           | true  | true  | true           | true
      variable                         | true  | true  | true           | true
      var2                             | true  | true  | true           | true
      version1                         | true  | true  | true           | true
      lowercase                        | true  | true  | true           | true
      alllowercase                     | true  | true  | true           | true
      name                             | true  | true  | true           | true
      ä1                               | true  | true  | true           | true

      # kebab case only
      kebab-case                       | true  | false | true           | true
      hello-world                      | true  | false | true           | true
      my-variable-name                 | true  | false | true           | true
      a-b                              | true  | false | true           | true
      one-two-three-four               | true  | false | true           | true
      var-123                          | true  | false | true           | true
      my-var-2                         | true  | false | true           | true
      test-1-2-3                       | true  | false | true           | true
      version-1                        | true  | false | true           | true
      v1-alpha                         | true  | false | true           | true
      item-42-test                     | true  | false | true           | true
      this-is-a-long-kebab-case-name   | true  | false | true           | true
      api-v2-endpoint                  | true  | false | true           | true
      user-id-123                      | true  | false | true           | true
      a1-b2-c3                         | true  | false | true           | true
      test-name                        | true  | false | true           | true
      test-123                         | true  | false | true           | true
      var-2                            | true  | false | true           | true
      test-id                          | true  | false | true           | true

      # lower camel case only
      camelCase                        | false | true  | true           | true
      helloWorld                       | false | true  | true           | true
      myVariableName                   | false | true  | true           | true
      aB                               | false | true  | true           | true
      oneTwoThreeFour                  | false | true  | true           | true
      myVar2                           | false | true  | true           | true
      v1Alpha                          | false | true  | true           | true
      item42Test                       | false | true  | true           | true
      a1B2C3                           | false | true  | true           | true
      test1Test2                       | false | true  | true           | true
      thisIsALongLowerCamelCaseName    | false | true  | true           | true
      apiV2Endpoint                    | false | true  | true           | true
      userId123                        | false | true  | true           | true
      getHTTPResponse                  | false | true  | true           | true
      parseXMLDocument                 | false | true  | true           | true

      # names only
      Word                             | false | false | false          | true
      Kebab-case                       | false | false | false          | true
      Hello-world                      | false | false | false          | true
      Test                             | false | false | false          | true
      PascalCase                       | false | false | false          | true
      kebab-Case                       | false | false | false          | true
      kebAb-case                       | false | false | false          | true
      UPPER                            | false | false | false          | true
      mixed-Case-name                  | false | false | false          | true
      test_case                        | false | false | false          | true
      CamelCase                        | false | false | false          | true
      HelloWorld                       | false | false | false          | true
      snake_case                       | false | false | false          | true
      hello_world                      | false | false | false          | true
      test_name                        | false | false | false          | true
      my_variable                      | false | false | false          | true
      UPPERCASE                        | false | false | false          | true
      ALL_CAPS                         | false | false | false          | true
      Test123                          | false | false | false          | true
      test_Case                        | false | false | false          | true
      test-Case                        | false | false | false          | true
      test-Name                        | false | false | false          | true
      snake_case_2                     | false | false | false          | true
      नाम                              | false | false | false          | true
      cafe\u0301                       | false | false | false          | true
      a-\u0301                         | false | false | false          | true
      می\u200cخواهم                    | false | false | false          | true
      a\u200db                         | false | false | false          | true

      # invalid for all predicates
      ''                               | false | false | false          | false
      -word                            | false | false | false          | false
      -kebab-case                      | false | false | false          | false
      -test                            | false | false | false          | false
      1word                            | false | false | false          | false
      123test                          | false | false | false          | false
      9-lives                          | false | false | false          | false
      word-                            | false | false | false          | false
      kebab-case-                      | false | false | false          | false
      test-                            | false | false | false          | false
      a-                               | false | false | false          | false
      kebab--case                      | false | false | false          | false
      hello--world                     | false | false | false          | false
      test--123                        | false | false | false          | false
      a--b                             | false | false | false          | false
      one---two                        | false | false | false          | false
      hello world                      | false | false | false          | false
      test.name                        | false | false | false          | false
      test@name                        | false | false | false          | false
      test$name                        | false | false | false          | false
      test#name                        | false | false | false          | false
      test!                            | false | false | false          | false
      test?                            | false | false | false          | false
      test/name                        | false | false | false          | false
      test\\name                       | false | false | false          | false
      test:name                        | false | false | false          | false
      test;name                        | false | false | false          | false
      test,name                        | false | false | false          | false
      -                                | false | false | false          | false
      --                               | false | false | false          | false
      123                              | false | false | false          | false
      Test-                            | false | false | false          | false
      -Test                            | false | false | false          | false
      test--case-                      | false | false | false          | false
      -test-case                       | false | false | false          | false
      9lives                           | false | false | false          | false
      42answer                         | false | false | false          | false
      test name                        | false | false | false          | false
      a b                              | false | false | false          | false
      test(name)                       | false | false | false          | false
      test[name]                       | false | false | false          | false
      test{name}                       | false | false | false          | false
      42                               | false | false | false          | false
      1a                               | false | false | false          | false
      -a                               | false | false | false          | false
      a.b                              | false | false | false          | false
      \u0301a                          | false | false | false          | false
      \u200da                          | false | false | false          | false
      a\u200eb                         | false | false | false          | false
      a\u202eb                         | false | false | false          | false
      a\u00adb                         | false | false | false          | false
      """)
  void namePredicates(String name, boolean kebab, boolean camel, boolean kebabOrCamel, boolean isName)
  {
    assertAll(
        () -> assertEquals(kebab, isKebabCaseName(name), "isKebabCaseName"),
        () -> assertEquals(camel, isLowerCamelCaseName(name), "isLowerCamelCaseName"),
        () -> assertEquals(kebabOrCamel, isKebabOrLowerCamelCaseName(name), "isKebabOrLowerCamelCaseName"),
        () -> assertEquals(isName, isName(name), "isName"));
  }


  @Test
  @DisplayName("Detect message format pack files")
  void detectMessageFormatPackFiles(@TempDir Path tempDir) throws IOException
  {
    var messageSupport = MessageSupportFactory.createDefault().addMessage("MSG", "Hello %{name}");

    for(var compress: new boolean[] { true, false })
    {
      final var packFile = tempDir.resolve("messages-" + compress + ".mfp");

      try(var stream = newOutputStream(packFile)) {
        messageSupport.exportMessages(stream, compress, null, null);
      }

      assertTrue(isMessageFormatPack(packFile), "compress=" + compress);
    }

    final var textFile = writeString(tempDir.resolve("messages.txt"), "MSG=Hello %{name}");

    assertFalse(isMessageFormatPack(textFile));
    assertFalse(isMessageFormatPack(writeString(tempDir.resolve("empty.mfp"), "")));
    assertFalse(isMessageFormatPack(tempDir.resolve("missing.mfp")));
    assertFalse(isMessageFormatPack(tempDir));
  }


  @Test
  @DisplayName("importMessages() rejects a null message factory and still closes the pack stream")
  @SuppressWarnings("DataFlowIssue")
  void importMessagesNullMessageFactory() throws IOException
  {
    var packBytes = new ByteArrayOutputStream();
    MessageSupportFactory
        .createDefault()
        .addMessage("MSG", "Hello")
        .exportMessages(packBytes);

    var closed = new AtomicBoolean();
    var packStream = new FilterInputStream(new ByteArrayInputStream(packBytes.toByteArray())) {
      @Override
      public void close() throws IOException
      {
        closed.set(true);
        super.close();
      }
    };

    assertNullRejected(
        "messageFactory must not be null",
        () -> importMessages(null, packStream, null, null));
    assertTrue(closed.get());
  }




  @Nested
  @DisplayName("trimAndNormalizeSpaces")
  class TrimAndNormalizeSpacesTest
  {
    @Test
    @DisplayName("null and empty inputs")
    void nullAndEmpty()
    {
      // null returns null
      //noinspection ConstantValue
      assertNull(trimAndNormalizeSpaces(null));

      // empty string returns empty
      assertEquals("", trimAndNormalizeSpaces(""));

      // spaces-only strings return empty
      assertEquals("", trimAndNormalizeSpaces(" "));
      assertEquals("", trimAndNormalizeSpaces("   "));
      assertEquals("", trimAndNormalizeSpaces("\u00a0"));
      assertEquals("", trimAndNormalizeSpaces(" \u00a0 "));
    }


    @Test
    @DisplayName("returns same instance when unchanged")
    void sameInstance()
    {
      // no spaces at all → same instance
      var s = "hello";
      assertSame(s, trimAndNormalizeSpaces(s));

      s = "a";
      assertSame(s, trimAndNormalizeSpaces(s));

      // single space between words, no leading/trailing → same instance
      s = "hello there";
      assertSame(s, trimAndNormalizeSpaces(s));

      s = "a b c";
      assertSame(s, trimAndNormalizeSpaces(s));
    }


    @Test
    @DisplayName("trims leading and trailing spaces")
    void trim()
    {
      assertEquals("hello", trimAndNormalizeSpaces(" hello"));
      assertEquals("hello", trimAndNormalizeSpaces("hello "));
      assertEquals("hello", trimAndNormalizeSpaces(" hello "));
      assertEquals("hello", trimAndNormalizeSpaces("   hello   "));
      assertEquals("hello", trimAndNormalizeSpaces("\u00a0hello\u00a0"));
      assertEquals("hello there", trimAndNormalizeSpaces("  hello there  "));
    }


    @Test
    @DisplayName("collapses consecutive spaces")
    void collapse()
    {
      assertEquals("hello there", trimAndNormalizeSpaces("hello  there"));
      assertEquals("hello there", trimAndNormalizeSpaces("hello     there"));
      assertEquals("a b c", trimAndNormalizeSpaces("a  b  c"));
      assertEquals("a b c d", trimAndNormalizeSpaces("a   b   c   d"));
    }


    @Test
    @DisplayName("trims and collapses combined")
    void trimAndCollapse()
    {
      assertEquals("hello there", trimAndNormalizeSpaces("  hello     there  "));
      assertEquals("a b c", trimAndNormalizeSpaces("   a   b   c   "));
      assertEquals("hello world foo bar", trimAndNormalizeSpaces("  hello  world  foo  bar  "));
    }


    @Test
    @DisplayName("unicode space characters")
    void unicode()
    {
      // non-breaking space (U+00A0) is a SPACE_SEPARATOR
      assertEquals("hello there", trimAndNormalizeSpaces("hello\u00a0\u00a0there"));
      assertEquals("hello there", trimAndNormalizeSpaces("\u00a0hello\u00a0\u00a0there\u00a0"));

      // mixed regular space and non-breaking space
      assertEquals("hello there", trimAndNormalizeSpaces("hello \u00a0 there"));
      assertEquals("a b", trimAndNormalizeSpaces(" \u00a0a\u00a0 \u00a0b \u00a0"));
    }


    @Test
    @DisplayName("single internal non-ASCII space character is normalized (no collapsing fast-path)")
    void singleInternalNonAsciiSpaceChar()
    {
      // single internal NBSP, no adjacent spaces to collapse: same result length as input,
      // must still be normalized to a regular ASCII space rather than left untouched
      assertEquals("hello there", trimAndNormalizeSpaces("hello\u00a0there"));

      // single internal tab, same result length as input
      assertEquals("a b", trimAndNormalizeSpaces("a\tb"));

      // single internal carriage return, same result length as input
      assertEquals("a b", trimAndNormalizeSpaces("a\rb"));

      // multiple, non-adjacent single non-ASCII space chars: still same result length as input
      assertEquals("a b c", trimAndNormalizeSpaces("a\u00a0b\u00a0c"));
    }


    @Test
    @DisplayName("preserves newlines")
    void preservesNewlines()
    {
      assertEquals("hello\nthere", trimAndNormalizeSpaces("hello\nthere"));
      assertEquals("hello\nthere", trimAndNormalizeSpaces(" hello\nthere "));
      assertEquals("hello\n\nthere", trimAndNormalizeSpaces("hello\n\nthere"));
      assertEquals("hello\nthere", trimAndNormalizeSpaces("  hello\nthere  "));
    }


    @Test
    @DisplayName("single characters")
    void singleCharacters()
    {
      assertEquals("a", trimAndNormalizeSpaces("a"));
      assertEquals("a", trimAndNormalizeSpaces(" a "));
      assertEquals("a", trimAndNormalizeSpaces("   a   "));
    }
  }




  @Nested
  @DisplayName("serializeMessage")
  class SerializeMessageTest
  {
    @Test
    @DisplayName("force quoted regular text message with letters only")
    void regularTextLettersOnlyForceQuoted()
    {
      final var message = MessageBuilder
          .create()
          .text("hello")
          .build();
      assertEquals("'hello'", serialize(message, true));
    }


    @Test
    @DisplayName("regular text message with letters only serializes unquoted")
    void regularTextLettersOnly()
    {
      final var message = MessageBuilder
          .create()
          .text("hello")
          .build();
      assertEquals("hello", serialize(message, false));
    }


    @Test
    @DisplayName("text message with single quotes uses double-quote wrapper")
    void textWithSingleQuotes()
    {
      final var message = MessageBuilder
          .create()
          .text("it's good")
          .build();
      assertEquals("\"it's good\"", serialize(message, false));
    }


    @Test
    @DisplayName("text message with double quotes uses single-quote wrapper")
    void textWithDoubleQuotes()
    {
      final var message = MessageBuilder
          .create()
          .text("say \"hi\"")
          .build();
      assertEquals("'say \"hi\"'", serialize(message, false));
    }


    @Test
    @DisplayName("text message with more single quotes than double quotes uses double-quote wrapper")
    void textWithMoreSingleQuotesThanDoubleQuotes()
    {
      final var message = MessageBuilder
          .create()
          .text("it's 'nice' \"ok\"")
          .build();
      assertEquals("\"it's 'nice' \\\"ok\\\"\"", serialize(message, false));
    }


    @Test
    @DisplayName("text message with more double quotes than single quotes uses single-quote wrapper")
    void textWithMoreDoubleQuotesThanSingleQuotes()
    {
      final var message = MessageBuilder
          .create()
          .text("\"a\" \"b\" 'c'")
          .build();
      assertEquals("'\"a\" \"b\" \\'c\\''", serialize(message, false));
    }


    @Test
    @DisplayName("compound message with text part containing single quote after parameter uses double-quote wrapper")
    void compoundMessageWithSingleQuoteAfterParameter()
    {
      final var message = MessageBuilder
          .create()
          .parameter("x")
          .text("it's").spaceBefore()
          .build();
      assertEquals("\"%{x} it's\"", serialize(message, false));
    }


    @Contract(pure = true)
    private @NotNull String serialize(@NotNull Message message, boolean forceQuoted)
    {
      final var context = new FormatStringSerializer.Context(UTF_8);

      serializeMessage(context, message, forceQuoted);

      return context
          .textJoiner()
          .asSpacedText()
          .getTextNotNull();
    }
  }
}
