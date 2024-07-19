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
import de.sayayi.lib.message.internal.CompoundMessage;
import de.sayayi.lib.message.internal.EmptyMessage;
import de.sayayi.lib.message.internal.EmptyMessageWithCode;
import org.junit.jupiter.api.Test;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static org.junit.jupiter.api.Assertions.*;


/**
 * @author Jeroen Gremmen
 */
public class MessageFactoryTest
{
  @Test
  public void testParseString()
  {
    Message.WithSpaces msg = NO_CACHE_INSTANCE.parseMessage("this is %{test}");
    assertInstanceOf(CompoundMessage.class, msg);
  }


  @Test
  public void testWithCode()
  {
    WithCode msgWithCode1 = NO_CACHE_INSTANCE.withCode("ABC", EmptyMessage.INSTANCE);
    assertEquals("ABC", msgWithCode1.getCode());
    assertInstanceOf(EmptyMessageWithCode.class, msgWithCode1);

    WithCode msgWithCode2 = NO_CACHE_INSTANCE.withCode("ABC", new EmptyMessageWithCode("DEF"));
    assertEquals("ABC", msgWithCode2.getCode());
    assertInstanceOf(EmptyMessageWithCode.class, msgWithCode2);
  }


  @Test
  public void testSyntaxError()
  {
    // lexer error
    assertThrows(MessageParserException.class,
        () -> NO_CACHE_INSTANCE.parseMessage("%{x,{true false:1}"));

    // parser error
    assertThrows(MessageParserException.class,
        () -> NO_CACHE_INSTANCE.parseMessage("%{x,true false:1}"));
  }
}
