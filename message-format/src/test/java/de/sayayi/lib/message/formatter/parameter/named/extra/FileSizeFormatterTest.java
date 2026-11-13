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
package de.sayayi.lib.message.formatter.parameter.named.extra;

import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import lombok.val;
import org.junit.jupiter.api.*;

import static java.util.Locale.GERMANY;
import static java.util.Locale.US;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Tests {@link FileSizeFormatter}: unit selection, scale, space and unit messages.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@DisplayName("File size formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
@SuppressWarnings("removal")
final class FileSizeFormatterTest extends AbstractFormatterTest
{
  private MessageSupport messageSupport;


  @BeforeEach
  void init()
  {
    val formatterService = new DefaultFormatterService();
    formatterService.addFormatter(new FileSizeFormatter());

    messageSupport = MessageSupportFactory.create(formatterService);
  }


  @Test
  @DisplayName("Select unit and scale")
  void testFormat()
  {
    assertEquals("0B", messageSupport.message("%{s,format:filesize}").with("s", 0).locale(US).format());
    assertEquals("999B", messageSupport.message("%{s,format:filesize}").with("s", 999).locale(US).format());
    assertEquals("1.0KB", messageSupport.message("%{s,format:filesize}").with("s", 1000).locale(US).format());
    assertEquals("1.2MB", messageSupport.message("%{s,format:filesize}").with("s", 1234567).locale(US).format());
    assertEquals("1.23 MB", messageSupport
        .message("%{s,format:filesize,scale:2,space:true}")
        .with("s", 1234567)
        .locale(US)
        .format());
    assertEquals("999.999KB",
        messageSupport.message("%{s,format:filesize,scale:3}").with("s", 999_999).locale(US).format());
    assertEquals("1.0MB", messageSupport.message("%{s,format:filesize}").with("s", 999_950).locale(US).format());
    assertEquals("1PB", messageSupport
        .message("%{s,format:filesize,scale:0}")
        .with("s", 1_000_000_000_000_000L)
        .locale(US)
        .format());
  }


  @Test
  @DisplayName("Replace unit by map message and match null")
  void testUnitMessageAndNull()
  {
    assertEquals("5 Gigabyte", messageSupport
        .message("%{s,format:filesize,scale:0,'GB':' Gigabyte'}")
        .with("s", 5_000_000_000L)
        .locale(GERMANY)
        .format());
    assertEquals("Size: unknown",
        messageSupport.message("Size: %{s,format:filesize,null:'unknown'}").with("s", null).format());
  }
}
