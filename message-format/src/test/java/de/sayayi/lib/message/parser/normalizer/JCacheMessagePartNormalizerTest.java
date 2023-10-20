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
package de.sayayi.lib.message.parser.normalizer;

import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.internal.CompoundMessage;
import de.sayayi.lib.message.part.MessagePart;
import lombok.val;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.platform.commons.util.ReflectionUtils.tryToReadFieldValue;


/**
 * @author Jeroen Gremmen
 */
@DisplayName("JCache message part normalizer")
class JCacheMessagePartNormalizerTest
{
  private static MessagePartNormalizer resolver;


  @BeforeAll
  static void init()
  {
    val cachingProvider = Caching.getCachingProvider();
    val cacheManager = cachingProvider.getCacheManager();
    val config = new MutableConfiguration<MessagePart,MessagePart>();

    config.setTypes(MessagePart.class, MessagePart.class);
    config.setStoreByValue(false);

    val cache = cacheManager.createCache("message-part-cache", config);

    resolver = new JCacheMessagePartNormalizer(cache);
  }


  @Test
  void testCache() throws Exception
  {
    val msg = new MessageFactory(resolver).parseMessage("this is %{a,number} and %{b}this is %{b}");
    val parts = (MessagePart[])
        tryToReadFieldValue(CompoundMessage.class, "messageParts", (CompoundMessage)msg).get();

    assertEquals(6, parts.length);

    assertSame(parts[0], parts[4]);
    assertSame(parts[3], parts[5]);
  }
}
