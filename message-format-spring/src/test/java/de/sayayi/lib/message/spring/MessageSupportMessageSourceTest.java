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
package de.sayayi.lib.message.spring;

import de.sayayi.lib.message.MessageSupportFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.context.support.StaticMessageSource;

import java.util.Locale;
import java.util.Optional;

import static de.sayayi.lib.message.util.AssertionTestSupport.assertNullRejected;
import static java.util.Locale.ENGLISH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * Tests {@link MessageSupportMessageSource}: message lookup with and without a default message, resolution of
 * {@code MessageSourceResolvable} instances, including their arguments, and validation of the parameter prefix.
 *
 * @author Jeroen Gremmen
 * @since 0.10.0
 */
@DisplayName("Spring MessageSource backed by MessageSupport")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class MessageSupportMessageSourceTest
{
  private MessageSource messageSource;


  @BeforeEach
  void init()
  {
    var messageSupport = MessageSupportFactory.createDefault();

    messageSupport.addMessage("spring.message.1",
        "you have %{p1,format:choice,1:'a single result',:'%{p1} results'}");
    messageSupport.addMessage("spring.message.2",
        "hello%{p1,!empty:' %{p1}',empty:'!'}");
    messageSupport.addMessage("spring.message.3",
        "0=%{p0} 1=%{p1} 2=%{p2} 3=%{p3}");
    messageSupport.addMessage("spring.field.name", "user name");

    messageSource = new MessageSupportMessageSource(messageSupport);
  }


  @Test
  @DisplayName("getMessage without a default message")
  void getMessageNoDefault()
  {
    assertEquals("0= 1=false 2=3.14 3=Hello",
        messageSource.getMessage("spring.message.3",
            new Object[] { false, 3.14f, Optional.of("Hello") }, ENGLISH));

    assertThrows(NoSuchMessageException.class, () ->
        messageSource.getMessage("spring.no-message",
            new Object[] { false, 3.14f, Optional.of("Hello") }, ENGLISH));
  }


  @Test
  @DisplayName("getMessage with a default message")
  void getMessageWithDefault()
  {
    assertEquals("you have a single result",
        messageSource.getMessage("spring.message.1",
            new Object[] { 1 }, "Default text", ENGLISH));

    assertEquals("Default text",
        messageSource.getMessage("spring.message.4",
            new Object[] { false, 3.14f, Optional.of("Hello") },
            "Default text", ENGLISH));
  }


  @Test
  @DisplayName("getMessage with resolvable")
  void getMessageWithResolvable()
  {
    assertEquals("hello jack", messageSource.getMessage(
        new DefaultMessageSourceResolvable(
            new String[] { "spring.message.12", "spring.message.2" },
            new Object[] { "jack" }), ENGLISH));

    assertEquals("Default text", messageSource.getMessage(
        new DefaultMessageSourceResolvable(
            new String[] { "spring.message.12", "spring.message.11" },
            new Object[] { "jack" },
            "Default text"), ENGLISH));
  }


  @Test
  @DisplayName("getMessage with resolvable arguments")
  void getMessageWithResolvableArguments()
  {
    assertEquals("hello user name", messageSource.getMessage("spring.message.2",
        new Object[] { new DefaultMessageSourceResolvable("spring.field.name") }, ENGLISH));

    // p2 is not used by the message, so the unresolvable argument is never resolved
    assertEquals("hello jack", messageSource.getMessage("spring.message.2",
        new Object[] { "jack", new DefaultMessageSourceResolvable("spring.no-field") }, ENGLISH));

    assertThrows(NoSuchMessageException.class, () -> messageSource.getMessage("spring.message.2",
        new Object[] { new DefaultMessageSourceResolvable("spring.no-field") }, ENGLISH));
  }


  @Test
  @DisplayName("Codes unknown to the message support are resolved by the parent message source")
  void getMessageFromParent()
  {
    var parentMessageSource = new StaticMessageSource();
    parentMessageSource.addMessage("spring.parent", ENGLISH, "parent {0}");
    ((MessageSupportMessageSource)messageSource).setParentMessageSource(parentMessageSource);

    assertEquals("parent jack", messageSource.getMessage("spring.parent", new Object[] { "jack" }, ENGLISH));
    assertEquals("hello jack", messageSource.getMessage("spring.message.2", new Object[] { "jack" }, ENGLISH));

    assertThrows(NoSuchMessageException.class, () -> messageSource.getMessage("spring.no-message", null, ENGLISH));
    assertThrows(NoSuchMessageException.class, () -> messageSource.getMessage(" ", null, ENGLISH));
  }


  @Test
  @DisplayName("Unknown codes without a locale report the default locale")
  void getMessageUnknownWithoutLocale()
  {
    final var expectedMessage =
        "No message found under code 'spring.no-message' for locale '" + Locale.getDefault() + "'.";

    assertEquals(expectedMessage, assertThrows(NoSuchMessageException.class,
        () -> messageSource.getMessage("spring.no-message", null, null)).getMessage());
    assertEquals(expectedMessage, assertThrows(NoSuchMessageException.class,
        () -> messageSource.getMessage(new DefaultMessageSourceResolvable("spring.no-message"), null)).getMessage());
  }


  @ParameterizedTest(name = "[{index}] {0}")
  @DisplayName("Parameter prefixes forming valid parameter names are accepted")
  @ValueSource(strings = { "p", "arg", "myArg", "my-arg-", "ä-" })
  void validParameterPrefix(String parameterPrefix)
  {
    var messageSupport = MessageSupportFactory.createDefault();
    messageSupport.addMessage("msg", "hello %{" + parameterPrefix + "1}");

    assertEquals(
        "hello jack",
        new MessageSupportMessageSource(parameterPrefix, messageSupport)
            .getMessage("msg", new Object[] { "jack" }, ENGLISH));
  }


  @ParameterizedTest(name = "[{index}] '{0}'")
  @DisplayName("Parameter prefixes not forming valid parameter names are rejected")
  @ValueSource(strings = { "", " ", "1x", "P", "Arg", "p_", "my_arg", "my--arg-" })
  void invalidParameterPrefix(String parameterPrefix)
  {
    var messageSupport = MessageSupportFactory.createDefault();

    var ex = assertThrows(
        IllegalArgumentException.class,
        () -> new MessageSupportMessageSource(parameterPrefix, messageSupport));

    assertEquals(
        "parameterPrefix '" + parameterPrefix + "' must form kebab-case or lower camel-case parameter names",
        ex.getMessage());
  }


  @Test
  @DisplayName("Message support must not be null")
  @SuppressWarnings("DataFlowIssue")
  void nullMessageSupport()
  {
    assertNullRejected(
        "messageSupport must not be null",
        () -> new MessageSupportMessageSource("p", null));
  }
}
