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
package de.sayayi.lib.message.formatter;

import de.sayayi.lib.message.formatter.parameter.NamedParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.formatter.post.PostFormatter;
import de.sayayi.lib.message.formatter.post.PostFormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static de.sayayi.lib.message.internal.part.config.MessagePartConfig.EMPTY_CONFIG;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Concurrency regression tests for {@link GenericFormatterService}.
 * <p>
 * These tests exercise concurrent formatter registration ({@link GenericFormatterService#addFormatterForType},
 * {@link GenericFormatterService#addFormatter}, {@link GenericFormatterService#addPostFormatter}) together with
 * concurrent lookups ({@link GenericFormatterService#getFormatters}, {@link GenericFormatterService#getPostFormatters},
 * {@link GenericFormatterService#getParameterConfigNames}) to make sure the read/write lock used internally does not
 * allow readers and writers to corrupt the registry or deadlock.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@DisplayName("GenericFormatterService concurrency")
@TestMethodOrder(MethodOrderer.DisplayName.class)
@SuppressWarnings({"ResultOfMethodCallIgnored", "ClassCanBeRecord"})
final class GenericFormatterServiceConcurrencyTest
{
  /** Distinct marker types used as keys for per-thread formatter registration. */
  private static final Class<?>[] TYPES = {
      Integer.class, Long.class, Double.class, Float.class, Short.class, Byte.class,
      List.class, Set.class, Runnable.class, Thread.class, Exception.class, Number.class
  };


  @Test
  @DisplayName("Concurrent type-based formatter registration is visible to concurrent lookups")
  void concurrentTypeFormatterRegistrationAndLookup() throws InterruptedException
  {
    val service = new GenericFormatterService();
    val latch = new CountDownLatch(1);
    val errors = new AtomicInteger(0);

    val writers = new Thread[TYPES.length];
    val readers = new Thread[8];

    for(var i = 0; i < writers.length; i++)
    {
      final var type = TYPES[i];

      (writers[i] = new Thread(() -> {
        try {
          latch.await();
          service.addFormatterForType(new FormattableType(type), new MarkerFormatter(type));
        } catch(Exception ex) {
          errors.incrementAndGet();
        }
      })).start();
    }

    for(var i = 0; i < readers.length; i++)
    {
      (readers[i] = new Thread(() -> {
        try {
          latch.await();

          for(var j = 0; j < 200; j++)
            for(var type: TYPES)
              service.getFormatters(null, type, EMPTY_CONFIG);
        } catch(Exception ex) {
          errors.incrementAndGet();
        }
      })).start();
    }

    latch.countDown();

    for(var thread: writers)
      thread.join(5000);
    for(var thread: readers)
      thread.join(5000);

    assertFalse(errors.get() > 0, "Concurrent registration/lookup caused an error");

    // all registered types must now be resolvable
    for(var type: TYPES)
    {
      val formatters = service.getFormatters(null, type, EMPTY_CONFIG);

      assertTrue(Stream
              .of(formatters)
              .anyMatch(f -> f instanceof MarkerFormatter marker && marker.type == type),
          "formatter for " + type + " was not registered");
    }
  }


  @Test
  @DisplayName("Concurrent named formatter and post formatter registration do not corrupt the registry")
  void concurrentNamedFormatterAndPostFormatterRegistration() throws InterruptedException
  {
    val service = new GenericFormatterService();
    val threadCount = 16;
    val latch = new CountDownLatch(1);
    val errors = new AtomicInteger(0);
    val writers = new Thread[threadCount];
    val readers = new Thread[8];
    val running = new AtomicBoolean(true);

    for(var i = 0; i < threadCount; i++)
    {
      final var idx = i;

      (writers[i] = new Thread(() -> {
        try {
          latch.await();

          service.addFormatter(new MarkerNamedFormatter("named-" + idx));
          service.addPostFormatter(new MarkerPostFormatter("post-" + idx));
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
            service.getPostFormatters();
            service.getParameterConfigNames();
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

    assertFalse(errors.get() > 0, "Concurrent registration/lookup caused an error");

    val postFormatters = service.getPostFormatters();
    assertEquals(threadCount, postFormatters.size());

    for(var i = 0; i < threadCount; i++)
    {
      assertTrue(postFormatters.containsKey("post-" + i));

      val formatters = service.getFormatters("named-" + i, String.class, EMPTY_CONFIG);
      assertEquals(1, formatters.length);
      assertEquals("named-" + i, ((NamedParameterFormatter)formatters[0]).getName());
    }
  }


  @RepeatedTest(3)
  @DisplayName("High contention stress test with concurrent reads and writes")
  @SuppressWarnings("resource")
  void highContentionStressTest() throws InterruptedException, ExecutionException, TimeoutException
  {
    val service = new GenericFormatterService();
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
            switch(rng.nextInt(5))
            {
              case 0 -> service.addFormatterForType(
                  new FormattableType(TYPES[rng.nextInt(TYPES.length)]),
                  new MarkerFormatter(TYPES[rng.nextInt(TYPES.length)]));

              case 1 -> service.addFormatter(new MarkerNamedFormatter("stress-" + idx + '-' + i));

              case 2 -> service.addPostFormatter(new MarkerPostFormatter("stress-post-" + idx + '-' + i));

              case 3 -> service.getFormatters(null, TYPES[rng.nextInt(TYPES.length)], EMPTY_CONFIG);

              default -> {
                service.getPostFormatters();
                service.getParameterConfigNames();
              }
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
    executor.awaitTermination(5, TimeUnit.SECONDS);

    assertEquals(0, errors.get(), "Stress test produced errors");
  }




  /** Minimal type-based formatter used to verify that a registration is retrievable after concurrent access. */
  private static final class MarkerFormatter implements ParameterFormatter
  {
    private final Class<?> type;


    private MarkerFormatter(Class<?> type) {
      this.type = type;
    }


    @Override
    public @NotNull Text format(@NotNull ParameterFormatterContext context, Object value) {
      return noSpaceText(String.valueOf(value));
    }


    @Override
    public @NotNull Set<FormattableType> getFormattableTypes() {
      return Set.of(new FormattableType(type));
    }
  }




  /** Minimal named formatter used to verify that concurrent named registrations do not corrupt the registry. */
  private static final class MarkerNamedFormatter implements NamedParameterFormatter
  {
    private final String name;


    private MarkerNamedFormatter(String name) {
      this.name = name;
    }


    @Override
    public @NotNull String getName() {
      return name;
    }


    @Override
    public @NotNull Text format(@NotNull ParameterFormatterContext context, Object value) {
      return noSpaceText(String.valueOf(value));
    }
  }




  /** Minimal post formatter used to verify that concurrent post formatter registrations do not corrupt the registry. */
  private static final class MarkerPostFormatter implements PostFormatter
  {
    private final String name;


    private MarkerPostFormatter(String name) {
      this.name = name;
    }


    @Override
    public @NotNull String getName() {
      return name;
    }


    @Override
    public @NotNull String format(@NotNull String string, @NotNull PostFormatterContext context) {
      return string;
    }
  }
}
