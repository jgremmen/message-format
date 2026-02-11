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
import org.junit.jupiter.api.Test;

import static de.sayayi.lib.message.util.MessageUtil.isKebabCaseName;
import static de.sayayi.lib.message.util.MessageUtil.isLowerCamelCaseName;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Test class for {@link MessageUtil}.
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
class MessageUtilTest
{
  @Test
  @DisplayName("Valid kebab-case names")
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
  @DisplayName("Invalid kebab-case names")
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
  @DisplayName("Valid lower camel-case names")
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
  @DisplayName("Invalid lower camel-case names")
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
}
