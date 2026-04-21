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

import de.sayayi.lib.message.exception.MessageParserException;
import de.sayayi.lib.message.internal.CompoundMessage;
import de.sayayi.lib.message.internal.EmptyMessage;
import de.sayayi.lib.message.internal.EmptyMessageWithCode;
import de.sayayi.lib.message.part.normalizer.LRUMessagePartNormalizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.ArrayList;
import java.util.concurrent.CyclicBarrier;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.part.normalizer.MessagePartNormalizer.PASS_THROUGH;
import static org.junit.jupiter.api.Assertions.*;


/**
 * @author Jeroen Gremmen
 */
@DisplayName("MessageFactory")
@TestMethodOrder(MethodOrderer.DisplayName.class)
@SuppressWarnings("ResultOfMethodCallIgnored")
final class MessageFactoryTest
{
  @Test
  @DisplayName("Parse message format string into CompoundMessage")
  void testParseString() {
    assertInstanceOf(CompoundMessage.class, NO_CACHE_INSTANCE.parseMessage("this is %{test}"));
  }


  @Test
  @DisplayName("Wrap message with a different code using withCode()")
  void testWithCode()
  {
    final var msgWithCode1 = NO_CACHE_INSTANCE.withCode("ABC", EmptyMessage.INSTANCE);
    assertEquals("ABC", msgWithCode1.getCode());
    assertInstanceOf(EmptyMessageWithCode.class, msgWithCode1);

    final var msgWithCode2 = NO_CACHE_INSTANCE.withCode("ABC", new EmptyMessageWithCode("DEF"));
    assertEquals("ABC", msgWithCode2.getCode());
    assertInstanceOf(EmptyMessageWithCode.class, msgWithCode2);
  }


  @Test
  @DisplayName("Throw MessageParserException on syntax errors")
  void testSyntaxError()
  {
    // lexer error
    assertThrows(MessageParserException.class,
        () -> NO_CACHE_INSTANCE.parseMessage("%{x,{true false:1}"));

    // parser error
    assertThrows(MessageParserException.class,
        () -> NO_CACHE_INSTANCE.parseMessage("%{x,true false:1}"));
  }


  @Test
  @DisplayName("Cache returns same instance for identical format string")
  void testParseMessageCacheHit()
  {
    final var factory = new MessageFactory(PASS_THROUGH, 8);

    final var msg1 = factory.parseMessage("hello %{name}");
    final var msg2 = factory.parseMessage("hello %{name}");

    assertSame(msg1, msg2, "cached message should be the same instance");
  }


  @Test
  @DisplayName("No-cache factory returns different instances for identical format string")
  void testParseMessageNoCacheReturnsDifferentInstances()
  {
    final var msg1 = NO_CACHE_INSTANCE.parseMessage("hello %{name}");
    final var msg2 = NO_CACHE_INSTANCE.parseMessage("hello %{name}");

    assertNotSame(msg1, msg2);
  }


  @Test
  @DisplayName("Cache evicts least recently used entry when full")
  void testParseMessageCacheEvictsLRU()
  {
    final var factory = new MessageFactory(PASS_THROUGH, 2);

    final var messageA = factory.parseMessage("a");
    factory.parseMessage("b");
    factory.parseMessage("c"); // should evict "a"

    assertNotSame(messageA, factory.parseMessage("a"), "evicted message should be re-parsed");
  }


  @Test
  @DisplayName("Cache preserves recently accessed entry over older entries")
  void testParseMessageCacheLRUAccessOrder()
  {
    var factory = new MessageFactory(PASS_THROUGH, 2);

    final var messageA = factory.parseMessage("a");

    factory.parseMessage("b");
    factory.parseMessage("a"); // access "a" so "b" becomes LRU
    factory.parseMessage("c"); // should evict "b", not "a"

    assertSame(messageA, factory.parseMessage("a"), "recently accessed message should survive eviction");
  }


  @Test
  @DisplayName("Cache is thread-safe under concurrent access")
  void testParseMessageCacheThreadSafety() throws Exception
  {
    final var THREAD_COUNT = 8;

    final var factory = new MessageFactory(LRUMessagePartNormalizer.create(64), 16);
    final var barrier = new CyclicBarrier(THREAD_COUNT);
    final var errors = new ArrayList<Throwable>();
    final var threads = new Thread[THREAD_COUNT];

    for(int t = 0; t < THREAD_COUNT; t++)
    {
      threads[t] = new Thread(() -> {
        try {
          barrier.await();
          for(int i = 0; i < 100; i++)
            factory.parseMessage("msg " + (i % 20));
        } catch(Throwable ex) {
          synchronized(errors) {
            errors.add(ex);
          }
        }
      });
      threads[t].start();
    }

    for(var thread: threads)
      thread.join();

    assertTrue(errors.isEmpty(), "concurrent cache access should not throw: " + errors);
  }
}
