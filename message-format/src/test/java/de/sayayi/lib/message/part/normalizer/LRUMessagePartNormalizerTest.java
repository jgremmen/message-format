/*
 * Copyright 2021 Jeroen Gremmen
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
package de.sayayi.lib.message.part.normalizer;

import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.internal.CompoundMessage;
import de.sayayi.lib.message.internal.part.TextPart;
import de.sayayi.lib.message.part.MessagePart;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.platform.commons.util.ReflectionUtils.tryToReadFieldValue;


/**
 * @author Jeroen Gremmen
 */
@DisplayName("LRU message part normalizer")
class LRUMessagePartNormalizerTest
{
  @Test
  @DisplayName("normalize without eviction")
  void testNoEviction()
  {
    val cache = new LRUMessagePartNormalizer(4);
    val mp1 = new TextPart("mp1");
    val mp2 = new TextPart("mp2");
    val mp3 = new TextPart("mp3");

    assertSame(mp1, cache.normalize(mp1));
    assertSame(mp2, cache.normalize(mp2));
    cache.normalize(mp3);

    assertSame(mp2, cache.normalize(new TextPart("mp2")));
    assertSame(mp1, cache.normalize(new TextPart("mp1")));
    assertSame(mp3, cache.normalize(new TextPart("mp3")));
  }


  @Test
  @DisplayName("normalize with eviction")
  void testWithEviction()
  {
    val cache = new LRUMessagePartNormalizer(4);
    val mp1 = new TextPart("mp1");
    val mp2 = new TextPart("mp2");
    val mp3 = new TextPart("mp3");
    val mp4 = new TextPart("mp4");
    val mp5 = new TextPart("mp5");
    val mp6 = new TextPart("mp6");

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
  @DisplayName("validate normalization on message parsing")
  void testCache() throws Exception
  {
    val resolver = new LRUMessagePartNormalizer(10);
    val msg = new MessageFactory(resolver).parseMessage("this is %{a,number} and %{b}this is %{b}");
    val parts = (MessagePart[])
        tryToReadFieldValue(CompoundMessage.class, "messageParts", (CompoundMessage)msg).get();

    assertEquals(6, parts.length);

    assertSame(parts[0], parts[4]);
    assertSame(parts[3], parts[5]);
  }
}
