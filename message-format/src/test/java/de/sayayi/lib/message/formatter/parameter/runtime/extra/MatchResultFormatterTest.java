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
package de.sayayi.lib.message.formatter.parameter.runtime.extra;

import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import org.junit.jupiter.api.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
@DisplayName("Matcher formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class MatchResultFormatterTest extends AbstractFormatterTest
{
  private MessageSupport context;


  @BeforeEach
  void init()
  {
    context = MessageSupportFactory.create(
        createFormatterService(new MatchResultFormatter()), NO_CACHE_INSTANCE);
  }


  @Test
  void testFormattableTypes() {
    assertFormatterForType(new MatchResultFormatter(), Matcher.class);
  }


  @Test
  void testFormatGroupNumber()
  {
    final var pattern = Pattern.compile("[vV]?(?<major>\\d+)(\\.(?<minor>\\d+))?");
    final var matcher = pattern.matcher("v5.12");

    assertEquals("v5.12", context.message("%{m,matcher:0}")
        .with("m", matcher).format());
    assertEquals("5", context.message("%{m,matcher:1}")
        .with("m", matcher).format());
    assertEquals(".12", context.message("%{m,matcher:2}")
        .with("m", matcher).format());
    assertEquals("12", context.message("%{m,matcher:3}")
        .with("m", matcher).format());

    assertEquals("", context.message("%{m,matcher:-1}")
        .with("m", matcher).format());
    assertEquals("", context.message("%{m,matcher:800}")
        .with("m", matcher).format());
  }


  @Test
  void testFormatGroupName()
  {
    final var pattern = Pattern.compile("[vV]?(?<major>\\d+)(\\.(?<minor>\\d+))?");
    final var matcher = pattern.matcher("v5.12");

    assertEquals("5", context.message("%{m,matcher:'major'}")
        .with("m", matcher).format());
    assertEquals("12", context.message("%{m,matcher:'minor'}")
        .with("m", matcher).format());

    assertEquals("", context.message("%{m,matcher:'haha'}")
        .with("m", matcher).format());
    assertEquals("", context.message("%{m,matcher:''}")
        .with("m", matcher).format());
  }


  @Test
  void testFormat()
  {
    final var pattern = Pattern.compile("[vV]?(?<major>\\d+)(\\.(?<minor>\\d+))?");
    final var matcher = pattern.matcher("v4.999");

    assertEquals("v4.999", context.message("%{m}").with("m", matcher).format());
  }
}
