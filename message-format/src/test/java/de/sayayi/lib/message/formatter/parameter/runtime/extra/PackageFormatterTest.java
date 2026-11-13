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

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Tests {@link PackageFormatter}: formattable types and formatting of package names.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@DisplayName("Package formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class PackageFormatterTest extends AbstractFormatterTest
{
  private MessageSupport messageSupport;


  @BeforeEach
  void init()
  {
    val formatterService = new DefaultFormatterService();
    formatterService.addFormatter(new PackageFormatter());

    messageSupport = MessageSupportFactory.create(formatterService);
  }


  @Test
  void testFormattableTypes() {
    assertFormatterForType(new PackageFormatter(), Package.class);
  }


  @Test
  @DisplayName("Format package names")
  void testFormat()
  {
    assertEquals("java.lang", messageSupport.message("%{p}").with("p", String.class.getPackage()).format());
    assertEquals("Scanning java.lang, java.util and java.nio.file", messageSupport
        .message("Scanning %{packages,list-sep-last:' and '}")
        .with("packages", List.of(String.class.getPackage(), List.class.getPackage(), Path.class.getPackage()))
        .format());
  }
}
