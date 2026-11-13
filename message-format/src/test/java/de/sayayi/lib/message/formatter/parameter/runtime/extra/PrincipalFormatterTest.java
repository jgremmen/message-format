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

import javax.security.auth.x500.X500Principal;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Tests {@link PrincipalFormatter}: formattable types, formatting of principal names and string key comparison.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@DisplayName("Principal formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class PrincipalFormatterTest extends AbstractFormatterTest
{
  private MessageSupport messageSupport;


  @BeforeEach
  void init()
  {
    val formatterService = new DefaultFormatterService();
    formatterService.addFormatter(new PrincipalFormatter());

    messageSupport = MessageSupportFactory.create(formatterService);
  }


  @Test
  void testFormattableTypes() {
    assertFormatterForType(new PrincipalFormatter(), Principal.class);
  }


  @Test
  @DisplayName("Format principal name")
  void testFormat()
  {
    assertEquals("Signed by CN=Alice,O=Example", messageSupport
        .message("Signed by %{user}")
        .with("user", new X500Principal("CN=Alice,O=Example"))
        .format());
    assertEquals("Logged in as guest",
        messageSupport.message("Logged in as %{user,null:'guest'}").with("user", null).format());
  }


  @Test
  @DisplayName("Compare principal name with string keys")
  void testStringKey()
  {
    final Principal admin = () -> "admin";
    final Principal bob = () -> "bob";
    val message = messageSupport.message("%{user,format:choice,'admin':'Administrator',:'Regular user'}");

    assertEquals("Administrator", message.with("user", admin).format());
    assertEquals("Regular user", message.with("user", bob).format());
  }
}
