/*
 * Copyright 2019 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import de.sayayi.lib.message.impl.EmptyMessage;
import de.sayayi.lib.message.impl.EmptyMessageWithCode;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Jeroen Gremmen
 */
public class MessageFactoryTest
{
  @Test
  public void testWithCode()
  {
    WithCode msgWithCode1 = MessageFactory.withCode("ABC", EmptyMessage.INSTANCE);
    assertEquals("ABC", msgWithCode1.getCode());
    assertTrue(msgWithCode1 instanceof EmptyMessageWithCode);

    WithCode msgWithCode2 = MessageFactory.withCode("ABC", new EmptyMessageWithCode("DEF"));
    assertEquals("ABC", msgWithCode2.getCode());
    assertTrue(msgWithCode2 instanceof EmptyMessageWithCode);
  }


  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test(expected = MessageParserException.class)
  public void testSyntaxError() {
    MessageFactory.parse("%{x,{true false:1}");
  }
}
