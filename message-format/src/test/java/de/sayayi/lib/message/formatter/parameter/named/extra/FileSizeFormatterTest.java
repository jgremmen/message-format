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
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static java.util.Locale.GERMANY;
import static java.util.Locale.US;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


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
  private final MessageSupport messageSupport = createDefaultMessageSupport(new FileSizeFormatter());


  static Stream<Arguments> unitAndScale()
  {
    return Stream.of(
        arguments("%{s,format:filesize}", 0, "0B"),
        arguments("%{s,format:filesize}", 999, "999B"),
        arguments("%{s,format:filesize}", 1000, "1.0KB"),
        arguments("%{s,format:filesize}", 1234567, "1.2MB"),
        arguments("%{s,format:filesize,scale:2,space:true}", 1234567, "1.23 MB"),
        arguments("%{s,format:filesize,scale:3}", 999_999, "999.999KB"),
        arguments("%{s,format:filesize}", 999_950, "1.0MB"),
        arguments("%{s,format:filesize,scale:0}", 1_000_000_000_000_000L, "1PB"));
  }


  @ParameterizedTest(name = "{1} with {0} -> {2}")
  @DisplayName("Select unit and scale")
  @MethodSource("unitAndScale")
  void selectUnitAndScale(@Language("MessageFormat") String formatString, Number size, String expected)
  {
    assertEquals(expected, messageSupport
        .message(formatString)
        .with("s", size)
        .locale(US)
        .format());
  }


  @Test
  @DisplayName("Replace unit by map message and match null")
  void unitMessageAndNull()
  {
    assertEquals(
        "5 Gigabyte",
        messageSupport
            .message("%{s,format:filesize,scale:0,'GB':' Gigabyte'}")
            .with("s", 5_000_000_000L)
            .locale(GERMANY)
            .format());
    assertEquals(
        "Size: unknown",
        messageSupport
            .message("Size: %{s,format:filesize,null:'unknown'}")
            .with("s", null)
            .format());
  }
}
