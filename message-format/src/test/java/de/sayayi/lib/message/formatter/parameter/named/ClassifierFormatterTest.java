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
package de.sayayi.lib.message.formatter.parameter.named;

import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import lombok.val;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Tests {@link ClassifierFormatter}: message selection by classifier, precedence and the default map entry.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@DisplayName("Classifier formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class ClassifierFormatterTest extends AbstractFormatterTest
{
  private MessageSupport messageSupport;


  @BeforeEach
  void init()
  {
    val formatterService = new DefaultFormatterService();
    formatterService.addFormatter(new ClassifierFormatter());

    messageSupport = MessageSupportFactory.create(formatterService);
  }


  @Test
  @DisplayName("Select message by classifier")
  void testFormat()
  {
    val message = messageSupport.message("Received %{v,format:classifier,'null':'nothing','bool':'flag %{v}'," +
        "'number':'number %{v}','list':'%{v,format:size} elements',:'text %{v}'}");

    assertEquals("Received number 42", message.with("v", 42).format());
    assertEquals("Received flag true", message.with("v", true).format());
    assertEquals("Received 2 elements", message.with("v", List.of("a", "b")).format());
    assertEquals("Received nothing", message.with("v", null).format());
    assertEquals("Received text abc", message.with("v", "abc").format());
  }


  @Test
  @DisplayName("Specific classifier takes precedence")
  void testPrecedence()
  {
    assertEquals("a language setting", messageSupport
        .message("%{v,format:classifier,'string':'some text','locale':'a language setting'}")
        .with("v", Locale.GERMANY)
        .format());
  }


  @Test
  @DisplayName("No matching classifier and no default formats null")
  void testNoMatch() {
    assertEquals("", messageSupport.message("%{v,format:classifier,'list':'a list'}").with("v", 42).format());
  }
}
