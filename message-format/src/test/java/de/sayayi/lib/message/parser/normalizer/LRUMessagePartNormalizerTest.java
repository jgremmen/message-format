/*
 * Copyright 2021 Jeroen Gremmen
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
package de.sayayi.lib.message.parser.normalizer;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.internal.ParameterizedMessage;
import de.sayayi.lib.message.internal.part.MessagePart;
import de.sayayi.lib.message.internal.part.TextPart;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.platform.commons.util.ReflectionUtils.tryToReadFieldValue;


/**
 * @author Jeroen Gremmen
 */
public class LRUMessagePartNormalizerTest
{
  @Test
  public void testNoEviction()
  {
    final LRUMessagePartNormalizer cache = new LRUMessagePartNormalizer(4);
    final MessagePart mp1 = new TextPart("mp1");
    final MessagePart mp2 = new TextPart("mp2");
    final MessagePart mp3 = new TextPart("mp3");

    assertSame(mp1, cache.normalize(mp1));
    assertSame(mp2, cache.normalize(mp2));
    cache.normalize(mp3);

    assertSame(mp2, cache.normalize(new TextPart("mp2")));
    assertSame(mp1, cache.normalize(new TextPart("mp1")));
    assertSame(mp3, cache.normalize(new TextPart("mp3")));
  }


  @Test
  public void testWithEviction()
  {
    final LRUMessagePartNormalizer cache = new LRUMessagePartNormalizer(4);
    final MessagePart mp1 = new TextPart("mp1");
    final MessagePart mp2 = new TextPart("mp2");
    final MessagePart mp3 = new TextPart("mp3");
    final MessagePart mp4 = new TextPart("mp4");
    final MessagePart mp5 = new TextPart("mp5");
    final MessagePart mp6 = new TextPart("mp6");

    cache.normalize(mp1);
    cache.normalize(mp2);
    cache.normalize(mp3);
    cache.normalize(mp4);
    cache.normalize(mp5);  // evicts mp1
    cache.normalize(mp6);  // evicts mp2

    assertNotSame(mp2, cache.normalize(new TextPart("mp2")));  // evicts mp3
    assertSame(mp4, cache.normalize(new TextPart("mp4")));
    assertSame(mp6, cache.normalize(new TextPart("mp6")));
  }


  @Test
  public void testCache() throws Exception
  {
    final LRUMessagePartNormalizer resolver = new LRUMessagePartNormalizer(10);
    final Message.WithSpaces msg = new MessageFactory(resolver).parse("this is %{a,number} and %{b}this is %{b}");
    final MessagePart[] parts = (MessagePart[])
        tryToReadFieldValue(ParameterizedMessage.class, "parts", (ParameterizedMessage)msg).get();

    assertEquals(6, parts.length);

    assertSame(parts[0], parts[4]);
    assertSame(parts[3], parts[5]);
  }
}
