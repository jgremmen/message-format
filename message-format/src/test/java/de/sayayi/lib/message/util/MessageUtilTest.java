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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static de.sayayi.lib.message.util.MessageUtil.*;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Test class for {@link MessageUtil}.
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
@DisplayName("Message utils")
class MessageUtilTest
{
  @Test
  @DisplayName("valid kebab-case names")
  void testIsKebabCaseNameValid()
  {
    // Simple single word
    assertTrue(isKebabCaseName("word"));
    assertTrue(isKebabCaseName("a"));
    assertTrue(isKebabCaseName("test"));

    // With hyphens
    assertTrue(isKebabCaseName("kebab-case"));
    assertTrue(isKebabCaseName("hello-world"));
    assertTrue(isKebabCaseName("my-variable-name"));
    assertTrue(isKebabCaseName("a-b"));
    assertTrue(isKebabCaseName("one-two-three-four"));

    // With digits
    assertTrue(isKebabCaseName("test123"));
    assertTrue(isKebabCaseName("abc123"));
    assertTrue(isKebabCaseName("var-123"));
    assertTrue(isKebabCaseName("my-var-2"));
    assertTrue(isKebabCaseName("test-1-2-3"));
    assertTrue(isKebabCaseName("version-1"));
    assertTrue(isKebabCaseName("v1-alpha"));
    assertTrue(isKebabCaseName("item-42-test"));

    // Complex valid examples
    assertTrue(isKebabCaseName("this-is-a-long-kebab-case-name"));
    assertTrue(isKebabCaseName("api-v2-endpoint"));
    assertTrue(isKebabCaseName("user-id-123"));
    assertTrue(isKebabCaseName("a1-b2-c3"));
  }


  @Test
  @DisplayName("invalid kebab-case names")
  @SuppressWarnings("SpellCheckingInspection")
  void testIsKebabCaseNameInvalid()
  {
    // Empty string
    assertFalse(isKebabCaseName(""));

    // Starting with uppercase
    assertFalse(isKebabCaseName("Word"));
    assertFalse(isKebabCaseName("Kebab-case"));
    assertFalse(isKebabCaseName("Hello-world"));
    assertFalse(isKebabCaseName("Test"));

    // Starting with hyphen
    assertFalse(isKebabCaseName("-word"));
    assertFalse(isKebabCaseName("-kebab-case"));
    assertFalse(isKebabCaseName("-test"));

    // Starting with digit
    assertFalse(isKebabCaseName("1word"));
    assertFalse(isKebabCaseName("123test"));
    assertFalse(isKebabCaseName("9-lives"));

    // Ending with hyphen
    assertFalse(isKebabCaseName("word-"));
    assertFalse(isKebabCaseName("kebab-case-"));
    assertFalse(isKebabCaseName("test-"));
    assertFalse(isKebabCaseName("a-"));

    // Consecutive hyphens
    assertFalse(isKebabCaseName("kebab--case"));
    assertFalse(isKebabCaseName("hello--world"));
    assertFalse(isKebabCaseName("test--123"));
    assertFalse(isKebabCaseName("a--b"));
    assertFalse(isKebabCaseName("one---two"));

    // With uppercase letters
    assertFalse(isKebabCaseName("camelCase"));
    assertFalse(isKebabCaseName("PascalCase"));
    assertFalse(isKebabCaseName("kebab-Case"));
    assertFalse(isKebabCaseName("kebAb-case"));
    assertFalse(isKebabCaseName("UPPER"));
    assertFalse(isKebabCaseName("mixed-Case-name"));

    // With special characters
    assertFalse(isKebabCaseName("test_case"));
    assertFalse(isKebabCaseName("hello world"));
    assertFalse(isKebabCaseName("test.name"));
    assertFalse(isKebabCaseName("test@name"));
    assertFalse(isKebabCaseName("test$name"));
    assertFalse(isKebabCaseName("test#name"));
    assertFalse(isKebabCaseName("test!"));
    assertFalse(isKebabCaseName("test?"));
    assertFalse(isKebabCaseName("test/name"));
    assertFalse(isKebabCaseName("test\\name"));
    assertFalse(isKebabCaseName("test:name"));
    assertFalse(isKebabCaseName("test;name"));
    assertFalse(isKebabCaseName("test,name"));

    // Just hyphens or digits
    assertFalse(isKebabCaseName("-"));
    assertFalse(isKebabCaseName("--"));
    assertFalse(isKebabCaseName("123"));

    // Mixed invalid cases
    assertFalse(isKebabCaseName("Test-"));
    assertFalse(isKebabCaseName("-Test"));
    assertFalse(isKebabCaseName("test--case-"));
    assertFalse(isKebabCaseName("-test-case"));
  }


  @Test
  @DisplayName("valid lower camel-case names")
  void testIsLowerCamelCaseNameValid()
  {
    // Simple single word
    assertTrue(isLowerCamelCaseName("word"));
    assertTrue(isLowerCamelCaseName("a"));
    assertTrue(isLowerCamelCaseName("test"));
    assertTrue(isLowerCamelCaseName("variable"));

    // Camel case
    assertTrue(isLowerCamelCaseName("camelCase"));
    assertTrue(isLowerCamelCaseName("helloWorld"));
    assertTrue(isLowerCamelCaseName("myVariableName"));
    assertTrue(isLowerCamelCaseName("aB"));
    assertTrue(isLowerCamelCaseName("oneTwoThreeFour"));

    // With digits
    assertTrue(isLowerCamelCaseName("test123"));
    assertTrue(isLowerCamelCaseName("abc123"));
    assertTrue(isLowerCamelCaseName("var2"));
    assertTrue(isLowerCamelCaseName("myVar2"));
    assertTrue(isLowerCamelCaseName("version1"));
    assertTrue(isLowerCamelCaseName("v1Alpha"));
    assertTrue(isLowerCamelCaseName("item42Test"));
    assertTrue(isLowerCamelCaseName("a1B2C3"));
    assertTrue(isLowerCamelCaseName("test1Test2"));

    // Complex valid examples
    assertTrue(isLowerCamelCaseName("thisIsALongLowerCamelCaseName"));
    assertTrue(isLowerCamelCaseName("apiV2Endpoint"));
    assertTrue(isLowerCamelCaseName("userId123"));
    assertTrue(isLowerCamelCaseName("getHTTPResponse"));
    assertTrue(isLowerCamelCaseName("parseXMLDocument"));

    // All lowercase
    assertTrue(isLowerCamelCaseName("lowercase"));
    assertTrue(isLowerCamelCaseName("alllowercase"));
  }


  @Test
  @DisplayName("invalid lower camel-case names")
  void testIsLowerCamelCaseNameInvalid()
  {
    // Empty string
    assertFalse(isLowerCamelCaseName(""));

    // Starting with uppercase (PascalCase)
    assertFalse(isLowerCamelCaseName("Word"));
    assertFalse(isLowerCamelCaseName("CamelCase"));
    assertFalse(isLowerCamelCaseName("HelloWorld"));
    assertFalse(isLowerCamelCaseName("Test"));
    assertFalse(isLowerCamelCaseName("PascalCase"));

    // Starting with digit
    assertFalse(isLowerCamelCaseName("1word"));
    assertFalse(isLowerCamelCaseName("123test"));
    assertFalse(isLowerCamelCaseName("9lives"));
    assertFalse(isLowerCamelCaseName("42answer"));

    // With hyphens
    assertFalse(isLowerCamelCaseName("kebab-case"));
    assertFalse(isLowerCamelCaseName("hello-world"));
    assertFalse(isLowerCamelCaseName("test-name"));
    assertFalse(isLowerCamelCaseName("a-b"));

    // With underscores
    assertFalse(isLowerCamelCaseName("snake_case"));
    assertFalse(isLowerCamelCaseName("hello_world"));
    assertFalse(isLowerCamelCaseName("test_name"));
    assertFalse(isLowerCamelCaseName("my_variable"));

    // With spaces
    assertFalse(isLowerCamelCaseName("hello world"));
    assertFalse(isLowerCamelCaseName("test name"));
    assertFalse(isLowerCamelCaseName("a b"));

    // With special characters
    assertFalse(isLowerCamelCaseName("test.name"));
    assertFalse(isLowerCamelCaseName("test@name"));
    assertFalse(isLowerCamelCaseName("test$name"));
    assertFalse(isLowerCamelCaseName("test#name"));
    assertFalse(isLowerCamelCaseName("test!"));
    assertFalse(isLowerCamelCaseName("test?"));
    assertFalse(isLowerCamelCaseName("test/name"));
    assertFalse(isLowerCamelCaseName("test\\name"));
    assertFalse(isLowerCamelCaseName("test:name"));
    assertFalse(isLowerCamelCaseName("test;name"));
    assertFalse(isLowerCamelCaseName("test,name"));
    assertFalse(isLowerCamelCaseName("test(name)"));
    assertFalse(isLowerCamelCaseName("test[name]"));
    assertFalse(isLowerCamelCaseName("test{name}"));

    // Just digits or special characters
    assertFalse(isLowerCamelCaseName("123"));
    assertFalse(isLowerCamelCaseName("42"));

    // All uppercase
    assertFalse(isLowerCamelCaseName("UPPERCASE"));
    assertFalse(isLowerCamelCaseName("ALL_CAPS"));

    // Mixed invalid cases
    assertFalse(isLowerCamelCaseName("Test123"));
    assertFalse(isLowerCamelCaseName("test_Case"));
    assertFalse(isLowerCamelCaseName("test-Case"));
  }


  @Test
  @DisplayName("valid kebab- or lower camel-case names")
  void testIsKebabOrLowerCamelCaseNameValid()
  {
    // Simple single word (valid for both)
    assertTrue(isKebabOrLowerCamelCaseName("word"));
    assertTrue(isKebabOrLowerCamelCaseName("a"));
    assertTrue(isKebabOrLowerCamelCaseName("test"));
    assertTrue(isKebabOrLowerCamelCaseName("variable"));

    // Valid kebab-case names
    assertTrue(isKebabOrLowerCamelCaseName("kebab-case"));
    assertTrue(isKebabOrLowerCamelCaseName("hello-world"));
    assertTrue(isKebabOrLowerCamelCaseName("my-variable-name"));
    assertTrue(isKebabOrLowerCamelCaseName("a-b"));
    assertTrue(isKebabOrLowerCamelCaseName("one-two-three-four"));
    assertTrue(isKebabOrLowerCamelCaseName("test-123"));
    assertTrue(isKebabOrLowerCamelCaseName("var-2"));
    assertTrue(isKebabOrLowerCamelCaseName("a1-b2-c3"));
    assertTrue(isKebabOrLowerCamelCaseName("api-v2-endpoint"));

    // Valid lower camel-case names
    assertTrue(isKebabOrLowerCamelCaseName("camelCase"));
    assertTrue(isKebabOrLowerCamelCaseName("helloWorld"));
    assertTrue(isKebabOrLowerCamelCaseName("myVariableName"));
    assertTrue(isKebabOrLowerCamelCaseName("aB"));
    assertTrue(isKebabOrLowerCamelCaseName("oneTwoThreeFour"));
    assertTrue(isKebabOrLowerCamelCaseName("test123"));
    assertTrue(isKebabOrLowerCamelCaseName("var2"));
    assertTrue(isKebabOrLowerCamelCaseName("a1B2C3"));
    assertTrue(isKebabOrLowerCamelCaseName("apiV2Endpoint"));

    // With digits (valid for both)
    assertTrue(isKebabOrLowerCamelCaseName("test123"));
    assertTrue(isKebabOrLowerCamelCaseName("abc123"));
    assertTrue(isKebabOrLowerCamelCaseName("version1"));
    assertTrue(isKebabOrLowerCamelCaseName("v1Alpha"));
    assertTrue(isKebabOrLowerCamelCaseName("v1-alpha"));

    // Complex valid examples
    assertTrue(isKebabOrLowerCamelCaseName("thisIsALongLowerCamelCaseName"));
    assertTrue(isKebabOrLowerCamelCaseName("this-is-a-long-kebab-case-name"));
    assertTrue(isKebabOrLowerCamelCaseName("userId123"));
    assertTrue(isKebabOrLowerCamelCaseName("user-id-123"));
  }


  @Test
  @DisplayName("invalid kebab- or lower camel-case names")
  @SuppressWarnings("SpellCheckingInspection")
  void testIsKebabOrLowerCamelCaseNameInvalid()
  {
    // Empty string
    assertFalse(isKebabOrLowerCamelCaseName(""));

    // Starting with uppercase (PascalCase)
    assertFalse(isKebabOrLowerCamelCaseName("Word"));
    assertFalse(isKebabOrLowerCamelCaseName("CamelCase"));
    assertFalse(isKebabOrLowerCamelCaseName("HelloWorld"));
    assertFalse(isKebabOrLowerCamelCaseName("Kebab-case"));
    assertFalse(isKebabOrLowerCamelCaseName("Hello-world"));
    assertFalse(isKebabOrLowerCamelCaseName("PascalCase"));

    // Starting with hyphen
    assertFalse(isKebabOrLowerCamelCaseName("-word"));
    assertFalse(isKebabOrLowerCamelCaseName("-kebab-case"));
    assertFalse(isKebabOrLowerCamelCaseName("-test"));

    // Starting with digit
    assertFalse(isKebabOrLowerCamelCaseName("1word"));
    assertFalse(isKebabOrLowerCamelCaseName("123test"));
    assertFalse(isKebabOrLowerCamelCaseName("9-lives"));
    assertFalse(isKebabOrLowerCamelCaseName("9lives"));
    assertFalse(isKebabOrLowerCamelCaseName("42answer"));

    // Ending with hyphen (kebab-case specific)
    assertFalse(isKebabOrLowerCamelCaseName("word-"));
    assertFalse(isKebabOrLowerCamelCaseName("kebab-case-"));
    assertFalse(isKebabOrLowerCamelCaseName("test-"));
    assertFalse(isKebabOrLowerCamelCaseName("a-"));

    // Consecutive hyphens (kebab-case specific)
    assertFalse(isKebabOrLowerCamelCaseName("kebab--case"));
    assertFalse(isKebabOrLowerCamelCaseName("hello--world"));
    assertFalse(isKebabOrLowerCamelCaseName("test--123"));
    assertFalse(isKebabOrLowerCamelCaseName("a--b"));
    assertFalse(isKebabOrLowerCamelCaseName("one---two"));

    // Uppercase in kebab-case (mixing formats)
    assertFalse(isKebabOrLowerCamelCaseName("kebab-Case"));
    assertFalse(isKebabOrLowerCamelCaseName("kebAb-case"));
    assertFalse(isKebabOrLowerCamelCaseName("mixed-Case-name"));
    assertFalse(isKebabOrLowerCamelCaseName("test-Name"));

    // With underscores
    assertFalse(isKebabOrLowerCamelCaseName("snake_case"));
    assertFalse(isKebabOrLowerCamelCaseName("hello_world"));
    assertFalse(isKebabOrLowerCamelCaseName("test_name"));
    assertFalse(isKebabOrLowerCamelCaseName("test_Case"));

    // With spaces
    assertFalse(isKebabOrLowerCamelCaseName("hello world"));
    assertFalse(isKebabOrLowerCamelCaseName("test name"));
    assertFalse(isKebabOrLowerCamelCaseName("a b"));

    // With special characters
    assertFalse(isKebabOrLowerCamelCaseName("test.name"));
    assertFalse(isKebabOrLowerCamelCaseName("test@name"));
    assertFalse(isKebabOrLowerCamelCaseName("test$name"));
    assertFalse(isKebabOrLowerCamelCaseName("test#name"));
    assertFalse(isKebabOrLowerCamelCaseName("test!"));
    assertFalse(isKebabOrLowerCamelCaseName("test?"));
    assertFalse(isKebabOrLowerCamelCaseName("test/name"));
    assertFalse(isKebabOrLowerCamelCaseName("test\\name"));
    assertFalse(isKebabOrLowerCamelCaseName("test:name"));
    assertFalse(isKebabOrLowerCamelCaseName("test;name"));
    assertFalse(isKebabOrLowerCamelCaseName("test,name"));

    // Just hyphens or digits
    assertFalse(isKebabOrLowerCamelCaseName("-"));
    assertFalse(isKebabOrLowerCamelCaseName("--"));
    assertFalse(isKebabOrLowerCamelCaseName("123"));
    assertFalse(isKebabOrLowerCamelCaseName("42"));

    // All uppercase
    assertFalse(isKebabOrLowerCamelCaseName("UPPERCASE"));
    assertFalse(isKebabOrLowerCamelCaseName("UPPER"));

    // Mixed invalid cases
    assertFalse(isKebabOrLowerCamelCaseName("Test-"));
    assertFalse(isKebabOrLowerCamelCaseName("-Test"));
    assertFalse(isKebabOrLowerCamelCaseName("test--case-"));
    assertFalse(isKebabOrLowerCamelCaseName("-test-case"));
    assertFalse(isKebabOrLowerCamelCaseName("Test123"));
    assertFalse(isKebabOrLowerCamelCaseName("test-Case"));
  }


  @Test
  @DisplayName("normalize spaces with null and empty inputs")
  void testTrimAndNormalizeSpacesNullAndEmpty()
  {
    // null returns null
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
  @DisplayName("normalize spaces returns same instance when unchanged")
  void testTrimAndNormalizeSpacesSameInstance()
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
  @DisplayName("normalize spaces trims leading and trailing spaces")
  void testTrimAndNormalizeSpacesTrim()
  {
    assertEquals("hello", trimAndNormalizeSpaces(" hello"));
    assertEquals("hello", trimAndNormalizeSpaces("hello "));
    assertEquals("hello", trimAndNormalizeSpaces(" hello "));
    assertEquals("hello", trimAndNormalizeSpaces("   hello   "));
    assertEquals("hello", trimAndNormalizeSpaces("\u00a0hello\u00a0"));
    assertEquals("hello there", trimAndNormalizeSpaces("  hello there  "));
  }


  @Test
  @DisplayName("normalize spaces collapses consecutive spaces")
  void testTrimAndNormalizeSpacesCollapse()
  {
    assertEquals("hello there", trimAndNormalizeSpaces("hello  there"));
    assertEquals("hello there", trimAndNormalizeSpaces("hello     there"));
    assertEquals("a b c", trimAndNormalizeSpaces("a  b  c"));
    assertEquals("a b c d", trimAndNormalizeSpaces("a   b   c   d"));
  }


  @Test
  @DisplayName("normalize spaces trims and collapses combined")
  void testTrimAndNormalizeSpacesTrimAndCollapse()
  {
    assertEquals("hello there", trimAndNormalizeSpaces("  hello     there  "));
    assertEquals("a b c", trimAndNormalizeSpaces("   a   b   c   "));
    assertEquals("hello world foo bar", trimAndNormalizeSpaces("  hello  world  foo  bar  "));
  }


  @Test
  @DisplayName("normalize spaces with unicode space characters")
  void testTrimAndNormalizeSpacesUnicode()
  {
    // non-breaking space (U+00A0) is a SPACE_SEPARATOR
    assertEquals("hello there", trimAndNormalizeSpaces("hello\u00a0\u00a0there"));
    assertEquals("hello there", trimAndNormalizeSpaces("\u00a0hello\u00a0\u00a0there\u00a0"));

    // mixed regular space and non-breaking space
    assertEquals("hello there", trimAndNormalizeSpaces("hello \u00a0 there"));
    assertEquals("a b", trimAndNormalizeSpaces(" \u00a0a\u00a0 \u00a0b \u00a0"));
  }


  @Test
  @DisplayName("normalize spaces preserves newlines")
  void testTrimAndNormalizeSpacesPreservesNewlines()
  {
    assertEquals("hello\nthere", trimAndNormalizeSpaces("hello\nthere"));
    assertEquals("hello\nthere", trimAndNormalizeSpaces(" hello\nthere "));
    assertEquals("hello\n\nthere", trimAndNormalizeSpaces("hello\n\nthere"));
    assertEquals("hello\nthere", trimAndNormalizeSpaces("  hello\nthere  "));
  }


  @Test
  @DisplayName("normalize spaces with single characters")
  void testTrimAndNormalizeSpacesSingleCharacters()
  {
    assertEquals("a", trimAndNormalizeSpaces("a"));
    assertEquals("a", trimAndNormalizeSpaces(" a "));
    assertEquals("a", trimAndNormalizeSpaces("   a   "));
  }
}
