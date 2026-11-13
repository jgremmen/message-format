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
package de.sayayi.lib.message.formatter.parameter.runtime.extra;

import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import lombok.val;
import org.junit.jupiter.api.*;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Tests {@link KeyFormatter}: formattable types and formatting of the algorithm, format and encoded form of a key.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@DisplayName("Key formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class KeyFormatterTest extends AbstractFormatterTest
{
  private MessageSupport messageSupport;


  @BeforeEach
  void init()
  {
    val formatterService = new DefaultFormatterService();
    formatterService.addFormatter(new KeyFormatter());

    messageSupport = MessageSupportFactory.create(formatterService);
  }


  @Test
  void testFormattableTypes() {
    assertFormatterForType(new KeyFormatter(), Key.class);
  }


  @Test
  @DisplayName("Format algorithm, format and encoded key")
  void testFormat()
  {
    val key = new SecretKeySpec(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 }, "AES");

    assertEquals("Using AES key in RAW format", messageSupport
        .message("Using %{k,key:algorithm} key in %{k,key:format} format")
        .with("k", key)
        .format());
    assertEquals("AQIDBAUGBwgJCgsMDQ4PEA==",
        messageSupport.message("%{k,key:encoded,bytes:base64}").with("k", key).format());
  }


  @Test
  @DisplayName("Missing key configuration delegates to the next formatter")
  void testNoConfig()
  {
    val key = new SecretKeySpec(new byte[] { 1, 2, 3, 4 }, "AES");

    assertEquals(key.toString(), messageSupport.message("%{k}").with("k", key).format());
  }
}
