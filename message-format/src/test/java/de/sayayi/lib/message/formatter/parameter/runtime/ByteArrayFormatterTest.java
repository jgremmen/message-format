/*
 * Copyright 2023 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter.parameter.runtime;

import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.parameter.named.SizeFormatter;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.IllegalCharsetNameException;
import java.util.Map;
import java.util.stream.Stream;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.params.provider.Arguments.arguments;


/**
 * Tests {@link ByteArrayFormatter}: formattable types, charset and base64 encoding, empty arrays, illegal charsets,
 * delegation and size queries.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@DisplayName("byte[] formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class ByteArrayFormatterTest extends AbstractFormatterTest
{
  private MessageAccessor messageAccessor;


  @BeforeEach
  void createMessageAccessor()
  {
    messageAccessor = MessageSupportFactory
        .create(createFormatterService(new ByteArrayFormatter(), new ArrayFormatter()))
        .setLocale("de-DE")
        .getMessageAccessor();
  }


  @Test
  @DisplayName("Formattable types include byte[]")
  void formattableTypes() {
    assertFormatterForType(new ByteArrayFormatter(), byte[].class);
  }


  @SuppressWarnings("ConvertToBasicLatin")
  static Stream<Arguments> encodedByteArrays()
  {
    return Stream.of(
        arguments("Größe".getBytes(ISO_8859_1), "iso-8859-1", "Größe"),
        arguments("Größe".getBytes(UTF_8), "utf-8", "Größe"),

        // empty or unsupported charset: platform default charset
        arguments("Größe".getBytes(), "", "Größe"),
        arguments("Größe".getBytes(), "AA-bb", "Größe"),

        arguments("Größe".getBytes(), "base64", "R3LDtsOfZQ=="),
        arguments(
            "This appears to be a very long text with a single linefeed!".getBytes(),
            "base64-lf",
            "VGhpcyBhcHBlYXJzIHRvIGJlIGEgdmVyeSBsb25nIHRleHQgd2l0aCBhIHNpbmdsZSBsaW5lZmVl\nZCE="),

        // empty array
        arguments(new byte[0], "utf-8", ""));
  }


  @ParameterizedTest(name = "bytes:{1} -> {2}")
  @DisplayName("Byte arrays are decoded with the configured charset or encoded as base64")
  @MethodSource("encodedByteArrays")
  void encodedByteArray(byte[] bytes, String bytesConfig, String expected) {
    assertEquals(noSpaceText(expected), format(messageAccessor, bytes, "bytes", bytesConfig));
  }


  @Test
  @DisplayName("An illegal charset name fails")
  void illegalCharset()
  {
    assertThrowsExactly(
        IllegalCharsetNameException.class,
        () -> format(messageAccessor, "Größe".getBytes(), "bytes", "XYZ&%"));
  }


  @Test
  @DisplayName("Without bytes configuration, formatting is delegated to the array formatter")
  void delegate()
  {
    assertEquals(
        noSpaceText("71, 114, -61, -74, -61, -97, 101"),
        format(messageAccessor, "Größe".getBytes(UTF_8), Map.of(), Map.of()));
  }


  @Test
  @DisplayName("The size of a byte array is its length")
  void size()
  {
    var messageSupport = MessageSupportFactory.create(
        createFormatterService(
            new SizeFormatter(),
            new ArrayFormatter(),
            new ByteArrayFormatter()));

    assertEquals(
        "2",
        messageSupport
            .message("%{c,format:size}")
            .with("c", new byte[] { 'a', 'b' })
            .format());
  }
}
