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
package de.sayayi.lib.message.icu.formatter;

import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.GenericFormatterService;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import lombok.val;
import org.junit.jupiter.api.*;

import static java.util.Locale.ENGLISH;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 * @since 0.24.0
 */
@DisplayName("'icu-person' formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class ICUPersonFormatterTest extends AbstractFormatterTest
{
  private static MessageSupport messageSupport;


  @BeforeAll
  static void init()
  {
    val formatterService = new GenericFormatterService();
    formatterService.addFormatter(new ICUPersonFormatter());

    messageSupport = MessageSupportFactory
        .create(formatterService)
        .setLocale(ENGLISH);
  }


  @Test
  @DisplayName("Full name with defaults")
  void testFullNameDefaults()
  {
    val msg = messageSupport
        .message("%{unused,format:icu-person}")
        .with("given-name", "John")
        .with("family-name", "Smith");

    assertEquals("John Smith", msg.format());
  }


  @Test
  @DisplayName("Given name as initial, full family name")
  void testGivenInitialFamilyFull()
  {
    val msg = messageSupport
        .message("%{unused,format:icu-person,given-format:initial,family-format:full}")
        .with("given-name", "John")
        .with("family-name", "Smith");

    assertEquals("J. Smith", msg.format());
  }


  @Test
  @DisplayName("Full given name, family name as initial")
  void testGivenFullFamilyInitial()
  {
    val msg = messageSupport
        .message("%{unused,format:icu-person,given-format:full,family-format:initial}")
        .with("given-name", "John")
        .with("family-name", "Smith");

    assertEquals("John S.", msg.format());
  }


  @Test
  @DisplayName("Surname-first order")
  void testSurnameFirst()
  {
    val msg = messageSupport
        .message("%{unused,format:icu-person,order:surname-first}")
        .with("given-name", "John")
        .with("family-name", "Smith");

    assertEquals("Smith John", msg.format());
  }


  @Test
  @DisplayName("Sorting order")
  void testSortingOrder()
  {
    val msg = messageSupport
        .message("%{unused,format:icu-person,order:sorting}")
        .with("given-name", "John")
        .with("family-name", "Smith");

    assertEquals("Smith, John", msg.format());
  }


  @Test
  @DisplayName("Full name with middle name")
  void testFullNameWithMiddle()
  {
    val msg = messageSupport
        .message("%{unused,format:icu-person,middle-format:full}")
        .with("given-name", "John")
        .with("middle-name", "Michael")
        .with("family-name", "Smith");

    assertEquals("John Michael Smith", msg.format());
  }


  @Test
  @DisplayName("Middle name as initial")
  void testMiddleInitial()
  {
    val msg = messageSupport
        .message("%{unused,format:icu-person,middle-format:initial}")
        .with("given-name", "John")
        .with("middle-name", "Michael")
        .with("family-name", "Smith");

    assertEquals("John M. Smith", msg.format());
  }


  @Test
  @DisplayName("Middle name excluded")
  void testMiddleNone()
  {
    val msg = messageSupport
        .message("%{unused,format:icu-person,middle-format:none}")
        .with("given-name", "John")
        .with("middle-name", "Michael")
        .with("family-name", "Smith");

    assertEquals("John Smith", msg.format());
  }


  @Test
  @DisplayName("With prefix")
  void testWithPrefix()
  {
    val msg = messageSupport
        .message("%{unused,format:icu-person,prefix-format:full}")
        .with("prefix", "Dr.")
        .with("given-name", "John")
        .with("family-name", "Smith");

    assertEquals("Dr. John Smith", msg.format());
  }


  @Test
  @DisplayName("Prefix excluded")
  void testPrefixNone()
  {
    val msg = messageSupport
        .message("%{unused,format:icu-person,prefix-format:none}")
        .with("prefix", "Dr.")
        .with("given-name", "John")
        .with("family-name", "Smith");

    assertEquals("John Smith", msg.format());
  }


  @Test
  @DisplayName("With suffix")
  void testWithSuffix()
  {
    val msg = messageSupport
        .message("%{unused,format:icu-person,suffix-format:full}")
        .with("given-name", "John")
        .with("family-name", "Smith")
        .with("suffix", "Jr.");

    assertEquals("John Smith Jr.", msg.format());
  }


  @Test
  @DisplayName("Suffix excluded")
  void testSuffixNone()
  {
    val msg = messageSupport
        .message("%{unused,format:icu-person,suffix-format:none}")
        .with("given-name", "John")
        .with("family-name", "Smith")
        .with("suffix", "Jr.");

    assertEquals("John Smith", msg.format());
  }


  @Test
  @DisplayName("Given name only")
  void testGivenNameOnly()
  {
    val msg = messageSupport
        .message("%{unused,format:icu-person,family-format:none}")
        .with("given-name", "John")
        .with("family-name", "Smith");

    assertEquals("John", msg.format());
  }


  @Test
  @DisplayName("Family name only")
  void testFamilyNameOnly()
  {
    val msg = messageSupport
        .message("%{unused,format:icu-person,given-format:none}")
        .with("given-name", "John")
        .with("family-name", "Smith");

    assertEquals("Smith", msg.format());
  }


  @Test
  @DisplayName("All initials")
  void testAllInitials()
  {
    val msg = messageSupport
        .message("%{unused,format:icu-person,given-format:initial,middle-format:initial,family-format:initial}")
        .with("given-name", "John")
        .with("middle-name", "Michael")
        .with("family-name", "Smith");

    assertEquals("J. M. S.", msg.format());
  }


  @Test
  @DisplayName("Sorting order with initials")
  void testSortingWithInitials()
  {
    val msg = messageSupport
        .message("%{unused,format:icu-person,order:sorting,given-format:initial,family-format:full}")
        .with("given-name", "John")
        .with("family-name", "Smith");

    assertEquals("Smith, J.", msg.format());
  }


  @Test
  @DisplayName("Null given and family name returns null text")
  void testNullNames()
  {
    val msg = messageSupport
        .message("%{unused,format:icu-person,null:'unknown'}")
        .with("given-name", null)
        .with("family-name", null);

    assertEquals("unknown", msg.format());
  }


  @Test
  @DisplayName("Full name with all parts")
  void testFullNameAllParts()
  {
    val msg = messageSupport
        .message("%{unused,format:icu-person,middle-format:full,prefix-format:full,suffix-format:full}")
        .with("prefix", "Mr.")
        .with("given-name", "James")
        .with("middle-name", "Earl")
        .with("family-name", "Carter")
        .with("suffix", "Jr.");

    assertEquals("Mr. James Earl Carter Jr.", msg.format());
  }


  @Test
  @DisplayName("Surname-first with prefix and suffix")
  void testSurnameFirstWithPrefixSuffix()
  {
    val msg = messageSupport
        .message("%{unused,format:icu-person,order:surname-first,middle-format:initial,prefix-format:full,suffix-format:full}")
        .with("prefix", "Dr.")
        .with("given-name", "Jane")
        .with("middle-name", "Marie")
        .with("family-name", "Doe")
        .with("suffix", "Ph.D.");

    assertEquals("Doe Dr. Jane M. Ph.D.", msg.format());
  }
}
