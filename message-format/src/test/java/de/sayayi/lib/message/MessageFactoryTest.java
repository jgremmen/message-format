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
import de.sayayi.lib.message.internal.message.CompoundMessage;
import de.sayayi.lib.message.internal.message.EmptyMessage;
import de.sayayi.lib.message.internal.message.EmptyMessageWithCode;
import de.sayayi.lib.message.internal.message.LocalizedMessageBundleWithCode;
import de.sayayi.lib.message.part.MessagePart;
import de.sayayi.lib.message.part.normalizer.LRUMessagePartNormalizer;
import de.sayayi.lib.message.part.normalizer.MessagePartNormalizer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

import static de.sayayi.lib.message.part.normalizer.MessagePartNormalizer.PASS_THROUGH;
import static java.util.Locale.JAPANESE;
import static java.util.Locale.forLanguageTag;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests {@link MessageFactory}: parsing of format strings and localized messages, syntax errors, and the behavior of
 * the message cache (hits, eviction and thread safety).
 *
 * @author Jeroen Gremmen
 * @since 0.3.1
 */
@DisplayName("MessageFactory")
@TestMethodOrder(MethodOrderer.DisplayName.class)
@SuppressWarnings("ResultOfMethodCallIgnored")
final class MessageFactoryTest
{
  @Test
  @DisplayName("Parse message format string into CompoundMessage")
  void testParseString() {
    assertInstanceOf(CompoundMessage.class, MessageFactory.getSharedInstance().parseMessage("this is %{test}"));
  }


  @Test
  @DisplayName("Wrap message with a different code using withCode()")
  void testWithCode()
  {
    final var msgWithCode1 = MessageFactory.getSharedInstance().withCode("ABC", EmptyMessage.INSTANCE);
    assertEquals("ABC", msgWithCode1.getCode());
    assertInstanceOf(EmptyMessageWithCode.class, msgWithCode1);

    final var msgWithCode2 = MessageFactory.getSharedInstance()
        .withCode("ABC", new EmptyMessageWithCode("DEF"));
    assertEquals("ABC", msgWithCode2.getCode());
    assertInstanceOf(EmptyMessageWithCode.class, msgWithCode2);
  }


  @Test
  @DisplayName("Throw MessageParserException on syntax errors")
  void testSyntaxError()
  {
    // lexer error
    assertThrows(MessageParserException.class,
        () -> MessageFactory.getSharedInstance().parseMessage("%{x,{true false:1}"));

    // parser error
    assertThrows(MessageParserException.class,
        () -> MessageFactory.getSharedInstance().parseMessage("%{x,true false:1}"));
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
    final var messageFactory = new MessageFactory(PASS_THROUGH, 0);
    final var msg1 = messageFactory.parseMessage("hello %{name}");
    final var msg2 = messageFactory.parseMessage("hello %{name}");

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


  @Test
  @DisplayName("Localized message bundle preserves insertion order for fallback selection")
  void testParseLocalizedMessagePreservesFallbackOrder()
  {
    final var localizedTexts = new LinkedHashMap<Locale,String>();
    localizedTexts.put(forLanguageTag("fr-CA"), "Bonjour");
    localizedTexts.put(forLanguageTag("en"), "Hello");

    final var messageSupport = MessageSupportFactory.shared();
    final var messageAccessor = messageSupport.getMessageAccessor();
    final var message = messageAccessor.getMessageFactory().parseMessage("MSG-001", localizedTexts);

    assertInstanceOf(LocalizedMessageBundleWithCode.class, message);
    assertEquals(
        "Bonjour",
        message.format(messageAccessor, Message.Parameters.empty(JAPANESE)));
  }


  @Test
  @DisplayName("Cache does not block other threads while compiling a message")
  void testParseMessageCacheCompilesOutsideLock() throws Exception
  {
    final var normalizer = new BlockingNormalizer();
    final var messageFactory = new MessageFactory(normalizer, 16);
    final var blockedParse = supplyAsync(() -> messageFactory.parseMessage("blocked %{a}"));

    try {
      assertTrue(normalizer.entered.await(5, SECONDS));

      // must not wait for the blocked compilation of another text
      final var otherParse = supplyAsync(() -> messageFactory.parseMessage("other %{b}"));
      assertInstanceOf(CompoundMessage.class, otherParse.get(5, SECONDS));
    } finally {
      normalizer.release.countDown();
    }

    assertInstanceOf(CompoundMessage.class, blockedParse.get(5, SECONDS));
  }


  @Test
  @DisplayName("Cache returns the first cached message for concurrently compiled identical texts")
  void testParseMessageCacheConcurrentIdenticalText() throws Exception
  {
    final var normalizer = new BlockingNormalizer();
    final var messageFactory = new MessageFactory(normalizer, 16);
    final var blockedParse = supplyAsync(() -> messageFactory.parseMessage("same %{a}"));
    final Message.WithSpaces message;

    try {
      assertTrue(normalizer.entered.await(5, SECONDS));

      // compiles and caches the text while the other thread is still compiling it
      message = supplyAsync(() -> messageFactory.parseMessage("same %{a}")).get(5, SECONDS);
    } finally {
      normalizer.release.countDown();
    }

    assertSame(message, blockedParse.get(5, SECONDS));
    assertSame(message, messageFactory.parseMessage("same %{a}"));
  }




  /**
   * Normalizer that blocks the first thread which normalizes a message part until {@link #release} is counted down.
   */
  private static final class BlockingNormalizer implements MessagePartNormalizer
  {
    private final CountDownLatch entered = new CountDownLatch(1);
    private final CountDownLatch release = new CountDownLatch(1);
    private volatile Thread blockedThread;


    @Override
    public <T extends MessagePart> @NotNull T normalize(@NotNull T part)
    {
      synchronized(this) {
        if (blockedThread == null)
          blockedThread = Thread.currentThread();
      }

      if (blockedThread == Thread.currentThread())
      {
        entered.countDown();

        try {
          if (!release.await(10, SECONDS))
            throw new IllegalStateException("normalizer was not released");
        } catch(InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      }

      return part;
    }
  }
}
