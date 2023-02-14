/*
 * Copyright 2019 Jeroen Gremmen
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

import de.sayayi.lib.message.Message.WithCode;
import de.sayayi.lib.message.exception.MessageParserException;
import de.sayayi.lib.message.internal.EmptyMessage;
import de.sayayi.lib.message.internal.EmptyMessageWithCode;
import de.sayayi.lib.message.internal.ParameterizedMessage;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.*;


/**
 * @author Jeroen Gremmen
 */
public class MessageFactoryTest
{
  @Test
  public void testParseString()
  {
    Message.WithSpaces msg = MessageFactory.NO_CACHE_INSTANCE.parse("this is %{test}");
    assertEquals(singleton("test"), msg.getParameterNames());
    assertTrue(msg instanceof ParameterizedMessage);
    assertTrue(msg.hasParameters());
  }


  @Test
  public void testWithCode()
  {
    WithCode msgWithCode1 = MessageFactory.NO_CACHE_INSTANCE.withCode("ABC", EmptyMessage.INSTANCE);
    assertEquals("ABC", msgWithCode1.getCode());
    assertTrue(msgWithCode1 instanceof EmptyMessageWithCode);

    WithCode msgWithCode2 = MessageFactory.NO_CACHE_INSTANCE.withCode("ABC", new EmptyMessageWithCode("DEF"));
    assertEquals("ABC", msgWithCode2.getCode());
    assertTrue(msgWithCode2 instanceof EmptyMessageWithCode);
  }


  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test
  public void testSyntaxError() {
    assertThrows(MessageParserException.class, () -> MessageFactory.NO_CACHE_INSTANCE.parse("%{x,{true false:1}"));
  }


  @Test
  public void testForLanguageTag()
  {
    assertEquals(Locale.ROOT, MessageFactory.forLanguageTag(""));
    assertEquals(Locale.GERMANY, MessageFactory.forLanguageTag("de_DE"));
  }
}
