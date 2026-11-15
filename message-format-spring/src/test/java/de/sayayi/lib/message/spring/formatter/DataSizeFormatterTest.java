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
package de.sayayi.lib.message.spring.formatter;

import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import de.sayayi.lib.message.formatter.parameter.named.extra.FileSizeFormatter;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import lombok.val;
import org.junit.jupiter.api.*;
import org.springframework.util.unit.DataSize;

import static java.util.Locale.US;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;


/**
 * Tests {@link DataSizeFormatter}: delegation of the number of bytes to the {@code filesize} formatter, including its
 * configuration keys and unit labels, the handling of {@code null} and the fallback to number formatting if no
 * {@code filesize} formatter is registered.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@DisplayName("Spring data size formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
@SuppressWarnings("removal")
final class DataSizeFormatterTest extends AbstractFormatterTest
{
  private MessageSupport messageSupport;


  @BeforeEach
  void init() {
    messageSupport = createDefaultMessageSupport(new DataSizeFormatter(), new FileSizeFormatter()).setLocale(US);
  }


  @Test
  @DisplayName("Formattable types")
  void testFormattableTypes() {
    assertFormatterForType(new DataSizeFormatter(), DataSize.class);
  }


  @Test
  @DisplayName("Registered as service")
  void testRegisteredAsService()
  {
    val formatters = new DefaultFormatterService().getFormatters(null, DataSize.class, null);

    assertInstanceOf(DataSizeFormatter.class, formatters[0]);
  }


  @Test
  @DisplayName("Data size is formatted as file size")
  void testFormat()
  {
    assertEquals("0B", messageSupport.message("%{s}").with("s", DataSize.ofBytes(0)).format());
    assertEquals("999B", messageSupport.message("%{s}").with("s", DataSize.ofBytes(999)).format());
    assertEquals("1.2MB", messageSupport.message("%{s}").with("s", DataSize.ofBytes(1234567)).format());
    assertEquals("1.2MB", messageSupport.message("%{s,format:filesize}").with("s", DataSize.ofBytes(1234567)).format());
  }


  @Test
  @DisplayName("Units are based on powers of 1000")
  void testDecimalUnits()
  {
    assertEquals("1.0KB", messageSupport.message("%{s}").with("s", DataSize.ofKilobytes(1)).format());
    assertEquals("524.3KB", messageSupport.message("%{s}").with("s", DataSize.ofKilobytes(512)).format());
    assertEquals("1.1GB", messageSupport.message("%{s}").with("s", DataSize.ofGigabytes(1)).format());
  }


  @Test
  @DisplayName("Configuration keys and unit labels of the filesize formatter apply")
  void testFileSizeConfig()
  {
    val size = DataSize.ofMegabytes(5);

    assertEquals("5 MB", messageSupport.message("%{s,scale:0,space:true}").with("s", size).format());
    assertEquals("5.243MB", messageSupport.message("%{s,scale:3}").with("s", size).format());
    assertEquals("5.2 Megabyte", messageSupport.message("%{s,'MB':' Megabyte'}").with("s", size).format());
  }


  @Test
  @DisplayName("Null is matched by the null key")
  void testNull()
  {
    assertEquals("unknown", messageSupport
        .message("%{s,null:'unknown'}")
        .with("s", null)
        .format());
  }


  @Test
  @DisplayName("Without filesize formatter the number of bytes is formatted")
  void testWithoutFileSizeFormatter()
  {
    val messageSupport = createDefaultMessageSupport(new DataSizeFormatter()).setLocale(US);

    assertEquals("1234567", messageSupport.message("%{s}").with("s", DataSize.ofBytes(1234567)).format());
  }
}
