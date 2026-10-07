/*
 * Copyright 2026 Jeroen Gremmen
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

import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static de.sayayi.lib.message.MessageSupportFactory.createGeneric;
import static java.util.Locale.GERMANY;
import static java.util.Locale.US;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Concurrency regression tests for {@link MessageSupport} / {@link de.sayayi.lib.message.internal.MessageSupportImpl}.
 * <p>
 * These tests exercise concurrent message/template registration, locale and default configuration changes together
 * with concurrent reads (message/template lookup and message formatting) to make sure the read/write lock used
 * internally does not allow readers and writers to corrupt the registries, lose updates or deadlock.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@DisplayName("MessageSupport concurrency")
@TestMethodOrder(MethodOrderer.DisplayName.class)
@SuppressWarnings("ResultOfMethodCallIgnored")
final class MessageSupportConcurrencyTest
{
  @Test
  @DisplayName("Concurrent message and template registration is visible to concurrent reads")
  void concurrentMessageAndTemplateRegistrationAndReads() throws InterruptedException
  {
    val support = createGeneric();
    val threadCount = 16;
    val latch = new CountDownLatch(1);
    val errors = new AtomicInteger(0);
    val running = new AtomicBoolean(true);

    val writers = new Thread[threadCount];
    val readers = new Thread[8];

    for(var i = 0; i < threadCount; i++)
    {
      final var idx = i;

      (writers[i] = new Thread(() -> {
        try {
          latch.await();

          support.addMessage("MSG-" + idx, "text " + idx);
          support.addTemplate("tpl-" + idx,
              support.getMessageAccessor().getMessageFactory().parseTemplate("template " + idx));
        } catch(Exception ex) {
          errors.incrementAndGet();
        }
      })).start();
    }

    for(var i = 0; i < readers.length; i++)
    {
      (readers[i] = new Thread(() -> {
        try {
          while(running.get())
          {
            val accessor = support.getMessageAccessor();

            accessor.getMessageCodes();
            accessor.getTemplateNames();
            accessor.hasMessageWithCode("MSG-0");
            accessor.hasTemplateWithName("tpl-0");
          }
        } catch(Exception ex) {
          errors.incrementAndGet();
        }
      })).start();
    }

    latch.countDown();

    for(var thread: writers)
      thread.join(5000);

    running.set(false);

    for(var thread: readers)
      thread.join(5000);

    assertFalse(errors.get() > 0, "Concurrent registration/reads caused an error");

    val accessor = support.getMessageAccessor();

    for(var i = 0; i < threadCount; i++)
    {
      assertTrue(accessor.hasMessageWithCode("MSG-" + i), "message MSG-" + i + " missing");
      assertTrue(accessor.hasTemplateWithName("tpl-" + i), "template tpl-" + i + " missing");
    }

    assertEquals(threadCount, accessor.getMessageCodes().size());
    assertEquals(threadCount, accessor.getTemplateNames().size());
  }


  @Test
  @DisplayName("Concurrent locale/default-config writes with concurrent formatting reads do not throw")
  void concurrentLocaleAndDefaultConfigWritesWithReads() throws InterruptedException
  {
    val support = createGeneric();

    support.addMessage("MSG-DATE", "%{d,date:medium}");

    val threadCount = 8;
    val iterationsPerThread = 200;
    val latch = new CountDownLatch(1);
    val errors = new AtomicInteger(0);
    val running = new AtomicBoolean(true);

    val writers = new Thread[threadCount];
    val readers = new Thread[8];

    for(var i = 0; i < threadCount; i++)
    {
      final var idx = i;

      (writers[i] = new Thread(() -> {
        try {
          latch.await();

          for(var j = 0; j < iterationsPerThread; j++)
          {
            support.setLocale(idx % 2 == 0 ? GERMANY : US);
            support.setDefaultConfig("ignore-default-tostring", j % 2 == 0);
          }
        } catch(Exception ex) {
          errors.incrementAndGet();
        }
      })).start();
    }

    for(var i = 0; i < readers.length; i++)
    {
      (readers[i] = new Thread(() -> {
        try {
          while(running.get())
          {
            support.getMessageAccessor().getLocale();
            support.getMessageAccessor().getDefaultConfig("ignore-default-tostring");
            support.code("MSG-DATE").with("d", LocalDate.of(2023, 6, 15)).format();
          }
        } catch(Exception ex) {
          errors.incrementAndGet();
        }
      })).start();
    }

    latch.countDown();

    for(var thread: writers)
      thread.join(5000);

    running.set(false);

    for(var thread: readers)
      thread.join(5000);

    assertFalse(errors.get() > 0,
        "Concurrent locale/default-config writes with concurrent reads caused an error");
  }


  @Test
  @Timeout(value = 5, unit = SECONDS)
  @DisplayName("Missing-template predicates can update the support")
  void missingTemplatePredicateCanUpdateSupport()
  {
    val support = createGeneric();
    val messageAccessor = support.getMessageAccessor();

    support.addMessage("MSG", "%[missing]");

    val missingTemplates = messageAccessor.findMissingTemplates(code -> {
      support.addMessage("FROM-FILTER", "text");
      return true;
    });

    assertEquals(Set.of("missing"), missingTemplates);
    assertTrue(messageAccessor.hasMessageWithCode("FROM-FILTER"));
  }


  @Test
  @Timeout(value = 5, unit = SECONDS)
  @DisplayName("Export predicates and output can update the support")
  void exportCallbacksCanUpdateSupportWithoutChangingExportSnapshot() throws IOException
  {
    val support = createGeneric();
    val messageAccessor = support.getMessageAccessor();

    support.addTemplate("template", messageAccessor.getMessageFactory().parseTemplate("template text"));
    support.addMessage("ORIGINAL", "%[template]");

    val bytes = new ByteArrayOutputStream();
    val streamMutation = new AtomicBoolean();
    val stream = new OutputStream() {
      @Override
      public void write(int value)
      {
        bytes.write(value);
        mutateFromStream();
      }

      @Override
      public void write(byte @NotNull [] values, int offset, int length)
      {
        bytes.write(values, offset, length);
        mutateFromStream();
      }

      private void mutateFromStream()
      {
        if (streamMutation.compareAndSet(false, true))
          support.addMessage("FROM-STREAM", "text");
      }
    };

    support.exportMessages(stream, false, code -> {
      support.addMessage("FROM-MESSAGE-FILTER", "text");
      return true;
    }, name -> {
      support.addTemplate("from-template-filter", messageAccessor.getMessageFactory().parseTemplate("text"));
      return true;
    });

    val imported = createGeneric();
    val importedMessageAccessor = imported.getMessageAccessor();
    imported.importMessages(new ByteArrayInputStream(bytes.toByteArray()));

    assertEquals(Set.of("ORIGINAL"), importedMessageAccessor.getMessageCodes());
    assertEquals(Set.of("template"), importedMessageAccessor.getTemplateNames());

    assertTrue(streamMutation.get());
    assertTrue(messageAccessor.hasMessageWithCode("FROM-MESSAGE-FILTER"));
    assertTrue(messageAccessor.hasMessageWithCode("FROM-STREAM"));
    assertTrue(messageAccessor.hasTemplateWithName("from-template-filter"));
  }


  @RepeatedTest(3)
  @DisplayName("High contention stress test with concurrent reads and writes")
  @SuppressWarnings("resource")
  void highContentionStressTest() throws InterruptedException, ExecutionException, TimeoutException
  {
    val support = createGeneric();
    val threadCount = 24;
    val iterationsPerThread = 200;
    val executor = Executors.newFixedThreadPool(threadCount);
    val latch = new CountDownLatch(1);
    val errors = new AtomicInteger(0);
    val futures = new ArrayList<Future<?>>();

    for(var t = 0; t < threadCount; t++)
    {
      final var idx = t;

      futures.add(executor.submit(() -> {
        try {
          latch.await();

          val rng = ThreadLocalRandom.current();

          for(var i = 0; i < iterationsPerThread; i++)
            switch(rng.nextInt(6))
            {
              case 0 -> support.addMessage("STRESS-" + idx + '-' + i, "text " + i);

              case 1 -> support.addTemplate("stress-tpl-" + idx + '-' + i,
                  support.getMessageAccessor().getMessageFactory().parseTemplate("template " + i));

              case 2 -> support.setLocale(rng.nextBoolean() ? GERMANY : US);

              case 3 -> support.setDefaultConfig("ignore-default-tostring", rng.nextBoolean());

              case 4 -> {
                val accessor = support.getMessageAccessor();

                accessor.getMessageCodes();
                accessor.getTemplateNames();
                accessor.getLocale();
              }

              default -> support.message("plain text").format();
            }
        } catch(Exception ex) {
          errors.incrementAndGet();
        }
      }));
    }

    latch.countDown();

    for(var future: futures)
      future.get(30, SECONDS);

    executor.shutdown();
    executor.awaitTermination(5, SECONDS);

    assertEquals(0, errors.get(), "Stress test produced errors");
  }
}
