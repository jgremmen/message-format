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
import de.sayayi.lib.message.internal.message.CompoundMessage;
import de.sayayi.lib.message.internal.part.TextPart;
import de.sayayi.lib.message.part.MessagePart;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.function.IntConsumer;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.platform.commons.util.ReflectionUtils.tryToReadFieldValue;


/**
 * @author Jeroen Gremmen
 */
@DisplayName("LRU message part normalizer")
@TestMethodOrder(MethodOrderer.DisplayName.class)
@SuppressWarnings("SameParameterValue")
final class LRUMessagePartNormalizerTest
{
  @Test
  @DisplayName("normalize without eviction")
  void testNoEviction()
  {
    val cache = LRUMessagePartNormalizer.create(4);
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
    val cache = LRUMessagePartNormalizer.create(4);
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
  @DisplayName("normalize identical instance not at head")
  void testIdenticalInstanceNotAtHead()
  {
    for(val maxSize: new int[] { 4, 100 })
    {
      val cache = LRUMessagePartNormalizer.create(maxSize);
      val mp1 = new TextPart("mp1");
      val mp2 = new TextPart("mp2");
      val mp3 = new TextPart("mp3");

      cache.normalize(mp1);
      cache.normalize(mp2);
      cache.normalize(mp3);

      assertSame(mp1, cache.normalize(mp1));
      assertSame(mp2, cache.normalize(mp2));
      assertSame(mp3, cache.normalize(new TextPart("mp3")));
      assertSame(mp1, cache.normalize(new TextPart("mp1")));
    }
  }


  @Test
  @DisplayName("validate normalization on message parsing")
  void testCache() throws Exception
  {
    val resolver = LRUMessagePartNormalizer.create(10);
    val msg = new MessageFactory(resolver)
        .parseMessage("this is %{a,format:number} and %{b}this is %{b}");
    val parts = (MessagePart[])
        tryToReadFieldValue(CompoundMessage.class, "messageParts", (CompoundMessage)msg).get();

    assertEquals(6, parts.length);

    assertSame(parts[0], parts[4]);
    assertSame(parts[3], parts[5]);
  }


  @ParameterizedTest(name = "max size {0}")
  @DisplayName("concurrent normalization of overlapping parts without eviction")
  @ValueSource(ints = { 16, 200 })
  void testConcurrentNoEviction(int maxSize) throws Exception
  {
    val cache = LRUMessagePartNormalizer.create(maxSize);
    val normalized = new ConcurrentHashMap<String,MessagePart>();

    // all threads normalize the same maxSize values, each starting at a different offset
    runConcurrently(8, thread -> {
      for(var n = 0; n < 50 * maxSize; n++)
      {
        val text = "mp" + ((thread * 7 + n) % maxSize);
        val part = cache.normalize(new TextPart(text));

        assertEquals(text, part.getText());
        assertSame(normalized.computeIfAbsent(text, t -> part), part);
      }
    });

    assertEquals(maxSize, normalized.size());

    for(val entry: normalized.entrySet())
      assertSame(entry.getValue(), cache.normalize(new TextPart(entry.getKey())));
  }


  @ParameterizedTest(name = "max size {0}")
  @DisplayName("concurrent normalization with eviction keeps the cache bounded and consistent")
  @ValueSource(ints = { 16, 200 })
  void testConcurrentWithEviction(int maxSize) throws Exception
  {
    val cache = LRUMessagePartNormalizer.create(maxSize);

    runConcurrently(8, thread -> {
      val random = ThreadLocalRandom.current();

      for(var n = 0; n < 50 * maxSize; n++)
      {
        val text = "mp" + random.nextInt(4 * maxSize);
        assertEquals(text, cache.normalize(new TextPart(text)).getText());
      }
    });

    // fill the cache with maxSize new parts, which must all be retained
    val parts = new TextPart[maxSize];
    for(var n = 0; n < maxSize; n++)
      assertSame(parts[n] = new TextPart("new" + n), cache.normalize(parts[n]));

    for(var n = 0; n < maxSize; n++)
      assertSame(parts[n], cache.normalize(new TextPart("new" + n)));

    // one more part exceeds the bound and evicts the least recently used part
    cache.normalize(new TextPart("extra"));

    assertNotSame(parts[0], cache.normalize(new TextPart("new0")));
    assertSame(parts[maxSize - 1], cache.normalize(new TextPart("new" + (maxSize - 1))));
  }


  private static void runConcurrently(int threads, @NotNull IntConsumer task) throws Exception
  {
    val start = new CountDownLatch(1);
    val tasks = new ArrayList<Callable<Void>>();

    for(var t = 0; t < threads; t++)
    {
      val thread = t;

      tasks.add(() -> {
        start.await();
        task.accept(thread);
        return null;
      });
    }

    try(val executor = Executors.newFixedThreadPool(threads)) {
      val futures = tasks.stream().map(executor::submit).toList();
      start.countDown();

      for(val future: futures)
        future.get(30, SECONDS);
    }
  }
}
