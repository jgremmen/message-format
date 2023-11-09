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
package de.sayayi.lib.message;

import de.sayayi.lib.message.formatter.DefaultFormatterService;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.DefaultMessageSourceResolvable;

import java.util.Optional;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static java.util.Locale.ENGLISH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * @author Jeroen Gremmen
 * @since 0.9.2
 */
class MessageSupportMessageSourceTest
{
  private MessageSource messageSource;


  @BeforeEach
  void init()
  {
    val messageSupport = MessageSupportFactory
        .create(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE);

    messageSupport.addMessage("spring.message.1",
        "you have %{p1,choice,1:'a single result',:'%{p1} results'}");
    messageSupport.addMessage("spring.message.2",
        "hello%{p1,!empty:' %{p1}',empty:'!'}");
    messageSupport.addMessage("spring.message.3",
        "0=%{p0} 1=%{p1} 2=%{p2} 3=%{p3}");

    messageSource = new MessageSupportMessageSource(messageSupport);
  }


  @Test
  @DisplayName("getMessage without a default message")
  void testGetMessageNoDefault()
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
  void testGetMessageWithDefault()
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
  void testGetMessageWithResolvable()
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
}
