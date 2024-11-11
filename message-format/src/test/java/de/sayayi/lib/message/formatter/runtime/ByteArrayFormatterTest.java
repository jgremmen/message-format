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
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import de.sayayi.lib.message.formatter.named.SizeFormatter;
import de.sayayi.lib.message.part.TextPart;
import de.sayayi.lib.message.part.parameter.key.ConfigKeyName;
import de.sayayi.lib.message.part.parameter.value.ConfigValueString;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.IllegalCharsetNameException;
import java.util.Map;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.part.MessagePart.Text.EMPTY;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static java.util.Locale.ROOT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@DisplayName("byte[] formatter")
public class ByteArrayFormatterTest extends AbstractFormatterTest
{
  @Test
  void testFormattableTypes() {
    assertFormatterForType(new ByteArrayFormatter(), byte[].class);
  }


  @Test
  @SuppressWarnings("ConvertToBasicLatin")
  void testEncodedByteArray()
  {
    val formatterService = createFormatterService(new ByteArrayFormatter(), new ArrayFormatter());
    val messageAccessor = MessageSupportFactory.create(formatterService, NO_CACHE_INSTANCE)
        .setLocale("de-DE")
        .getMessageAccessor();

    assertEquals(new TextPart("Größe"),
        format(messageAccessor, "Größe".getBytes(ISO_8859_1),
            Map.of(new ConfigKeyName("bytes"), new ConfigValueString("iso-8859-1"))));
    assertEquals(new TextPart("Größe"),
        format(messageAccessor, "Größe".getBytes(UTF_8),
            Map.of(new ConfigKeyName("bytes"), new ConfigValueString("utf-8"))));
    assertEquals(new TextPart("Größe"),
        format(messageAccessor, "Größe".getBytes(),
            Map.of(new ConfigKeyName("bytes"), new ConfigValueString(""))));
    assertEquals(new TextPart("Größe"),
        format(messageAccessor, "Größe".getBytes(),
            Map.of(new ConfigKeyName("bytes"), new ConfigValueString("AA-bb"))));
  }


  @Test
  void testEmptyArray()
  {
    val formatterService = createFormatterService(new ByteArrayFormatter());
    val messageAccessor = MessageSupportFactory.create(formatterService, NO_CACHE_INSTANCE)
        .setLocale(ROOT)
        .getMessageAccessor();

    assertEquals(EMPTY,
        format(messageAccessor, "".getBytes(UTF_8),
            Map.of(new ConfigKeyName("bytes"), new ConfigValueString("utf-8"))));
  }


  @Test
  void testIllegalCharset()
  {
    val formatterService = createFormatterService(new ByteArrayFormatter(), new ArrayFormatter());
    val messageAccessor = MessageSupportFactory.create(formatterService, NO_CACHE_INSTANCE)
        .setLocale("de-DE")
        .getMessageAccessor();

    assertThrowsExactly(IllegalCharsetNameException.class, () ->
        format(messageAccessor, "Größe".getBytes(),
            Map.of(new ConfigKeyName("bytes"), new ConfigValueString("XYZ&%"))));
  }


  @Test
  void testBase64()
  {
    val formatterService = createFormatterService(new ByteArrayFormatter(), new ArrayFormatter());
    val messageAccessor = MessageSupportFactory.create(formatterService, NO_CACHE_INSTANCE)
        .getMessageAccessor();

    assertEquals(new TextPart("R3LDtsOfZQ=="), format(messageAccessor, "Größe".getBytes(),
        Map.of(new ConfigKeyName("bytes"), new ConfigValueString("base64"))));
  }


  @Test
  void testBase64lf()
  {
    val formatterService = createFormatterService(new ByteArrayFormatter(), new ArrayFormatter());
    val messageAccessor = MessageSupportFactory.create(formatterService, NO_CACHE_INSTANCE)
        .getMessageAccessor();

    assertEquals(new TextPart(
        "VGhpcyBhcHBlYXJzIHRvIGJlIGEgdmVyeSBsb25nIHRleHQgd2l0aCBhIHNpbmdsZSBsaW5lZmVl\nZCE="),
        format(messageAccessor,
            "This appears to be a very long text with a single linefeed!".getBytes(),
            Map.of(new ConfigKeyName("bytes"), new ConfigValueString("base64-lf"))));
  }


  @Test
  void testDelegate()
  {
    val formatterService = createFormatterService(
        new ByteArrayFormatter(),
        new ArrayFormatter());
    val messageAccessor = MessageSupportFactory.create(formatterService, NO_CACHE_INSTANCE)
        .setLocale("de-DE")
        .getMessageAccessor();

    assertEquals(new TextPart("71, 114, -61, -74, -61, -97, 101"),
        format(messageAccessor, "Größe".getBytes(UTF_8), emptyMap()));
  }


  @Test
  void testSize()
  {
    val messageSupport = MessageSupportFactory.create(
        createFormatterService(
            new SizeFormatter(),
            new ArrayFormatter(),
            new ByteArrayFormatter()),
        NO_CACHE_INSTANCE);

    assertEquals("2", messageSupport.message("%{c,size}")
        .with("c", new byte[] { 'a', 'b' }).format());
  }
}
