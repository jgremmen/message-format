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

import de.sayayi.lib.message.exception.FormatterServiceException;
import de.sayayi.lib.message.formatter.parameter.NamedParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.formatter.post.PostFormatter;
import de.sayayi.lib.message.formatter.post.PostFormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import static de.sayayi.lib.message.internal.part.config.MessagePartConfig.EMPTY_CONFIG;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.util.ConcurrentTestSupport.runWithReaders;
import static de.sayayi.lib.message.util.ConcurrentTestSupport.stress;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.ThreadLocalRandom.current;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Concurrency regression tests for {@link GenericFormatterService}.
 * <p>
 * These tests exercise concurrent formatter registration ({@link GenericFormatterService#addFormatterForType},
 * {@link GenericFormatterService#addFormatter}, {@link GenericFormatterService#addPostFormatter}) together with
 * concurrent lookups ({@link GenericFormatterService#getFormatters}, {@link GenericFormatterService#getPostFormatters},
 * {@link GenericFormatterService#getParameterConfigNames}) to make sure that concurrent registrations do not corrupt
 * the registry or deadlock, and that lookups never see a partially published registration.
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
  void concurrentTypeFormatterRegistrationAndLookup() throws Exception
  {
    var service = new GenericFormatterService();

    runWithReaders(TYPES.length, idx -> service.addFormatterForType(
        new FormattableType(TYPES[idx]), new MarkerFormatter(TYPES[idx])), 8, reader -> {
      for(var type: TYPES)
        service.getFormatters(null, type, EMPTY_CONFIG);
    });

    // all registered types must now be resolvable
    for(var type: TYPES)
    {
      var formatters = service.getFormatters(null, type, EMPTY_CONFIG);

      assertTrue(Stream
              .of(formatters)
              .anyMatch(f -> f instanceof MarkerFormatter marker && marker.type == type),
          "formatter for " + type + " was not registered");
    }
  }


  @Test
  @DisplayName("Concurrent named formatter and post formatter registration do not corrupt the registry")
  void concurrentNamedFormatterAndPostFormatterRegistration() throws Exception
  {
    var service = new GenericFormatterService();
    var threadCount = 16;

    runWithReaders(threadCount, idx -> {
      service.addFormatter(new MarkerNamedFormatter("named-" + idx));
      service.addPostFormatter(new MarkerPostFormatter("post-" + idx));
    }, 8, reader -> {
      service.getPostFormatters();
      service.getParameterConfigNames();
    });

    var postFormatters = service.getPostFormatters();
    assertEquals(threadCount, postFormatters.size());

    for(var i = 0; i < threadCount; i++)
    {
      assertTrue(postFormatters.containsKey("post-" + i));

      var formatters = service.getFormatters("named-" + i, String.class, EMPTY_CONFIG);
      assertEquals(1, formatters.length);
      assertEquals("named-" + i, ((NamedParameterFormatter)formatters[0]).getName());
    }
  }


  @RepeatedTest(5)
  @DisplayName("Formatter registered during concurrent lookups is returned by every subsequent lookup")
  void registrationVisibleToSubsequentLookups() throws Exception
  {
    var service = new GenericFormatterService();

    // readers keep (re)filling the formatter cache from whatever registry they see, while each registration must be
    // visible to the writer's own lookup as soon as the registration has returned
    runWithReaders(1, writer -> {
      for(var i = 0; i < 200; i++)
      {
        var formatter = new MarkerFormatter(Integer.class);

        service.addFormatterForType(new FormattableType(Integer.class), formatter);

        assertTrue(Stream
            .of(service.getFormatters(null, Integer.class, EMPTY_CONFIG))
            .anyMatch(f -> f == formatter), "registered formatter is not returned by lookup");
      }
    }, 8, reader -> service.getFormatters(null, Integer.class, EMPTY_CONFIG));
  }


  @Test
  @DisplayName("Named formatter registering a formatter from canFormat does not hang the lookup")
  void registrationFromCanFormatDoesNotHang()
  {
    var service = new GenericFormatterService();

    service.addFormatter(new MarkerNamedFormatter("reentrant") {
      @Override
      public boolean canFormat(@NotNull Class<?> type)
      {
        service.addPostFormatter(new MarkerPostFormatter("registered-by-can-format"));
        return true;
      }
    });

    assertTimeoutPreemptively(ofSeconds(5), () -> service.getFormatters("reentrant", String.class, EMPTY_CONFIG));
    assertTrue(service.getPostFormatters().containsKey("registered-by-can-format"));
  }


  @Test
  @DisplayName("Formatter method synchronized with a concurrent registration does not deadlock")
  void synchronizedFormatterMethodDoesNotDeadlock() throws Exception
  {
    var service = new GenericFormatterService();
    var formatter = new ParameterFormatter() {
      @Override
      public @NotNull Text format(@NotNull ParameterFormatterContext context, Object value) {
        return noSpaceText(String.valueOf(value));
      }

      @Override
      public synchronized @NotNull Set<FormattableType> getFormattableTypes() {
        return Set.of(new FormattableType(Long.class));
      }
    };

    assertNoDeadlock(formatter,
        () -> service.addPostFormatter(new MarkerPostFormatter("registered-while-synchronized")),
        () -> service.addFormatter(formatter));

    assertTrue(service.getPostFormatters().containsKey("registered-while-synchronized"));
    assertSame(formatter, service.getFormatters(null, Long.class, EMPTY_CONFIG)[0]);
  }


  @Test
  @DisplayName("Registered formatter with synchronized equals does not deadlock a concurrent registration")
  void synchronizedEqualsOfRegisteredFormatterDoesNotDeadlock() throws Exception
  {
    var service = new GenericFormatterService();
    var registered = new AutoApplyFormatter("registered") {
      @Override
      public synchronized boolean equals(Object o) {
        return this == o;
      }

      @Override
      public int hashCode() {
        return System.identityHashCode(this);
      }
    };

    service.addFormatter(registered);

    // the conflicting configuration name makes the registration compare both formatters
    assertNoDeadlock(registered,
        () -> service.addPostFormatter(new MarkerPostFormatter("registered-while-synchronized")),
        () -> assertThrows(FormatterServiceException.class,
            () -> service.addFormatter(new AutoApplyFormatter("conflicting"))));

    assertTrue(service.getPostFormatters().containsKey("registered-while-synchronized"));
  }


  @RepeatedTest(3)
  @DisplayName("High contention stress test with concurrent reads and writes")
  void highContentionStressTest() throws Exception
  {
    var service = new GenericFormatterService();

    stress(24, 200,
        (idx, i) -> service.addFormatterForType(
            new FormattableType(TYPES[current().nextInt(TYPES.length)]),
            new MarkerFormatter(TYPES[current().nextInt(TYPES.length)])),
        (idx, i) -> service.addFormatter(new MarkerNamedFormatter("stress-" + idx + '-' + i)),
        (idx, i) -> service.addPostFormatter(new MarkerPostFormatter("stress-post-" + idx + '-' + i)),
        (idx, i) -> service.getFormatters(null, TYPES[current().nextInt(TYPES.length)], EMPTY_CONFIG),
        (idx, i) -> {
          service.getPostFormatters();
          service.getParameterConfigNames();
        });
  }




  /**
   * Runs {@code registration} while another thread holds the monitor of {@code monitor} and runs
   * {@code whileHoldingMonitor}, and asserts that both threads terminate. The threads are daemon threads, as a
   * deadlocked lock cannot be interrupted.
   */
  private static void assertNoDeadlock(@NotNull Object monitor, @NotNull Runnable whileHoldingMonitor,
                                       @NotNull Runnable registration) throws InterruptedException
  {
    var monitorHeld = new CountDownLatch(1);
    var failures = new ConcurrentLinkedQueue<Throwable>();

    var monitorThread = startDaemon(failures, () -> {
      synchronized(monitor) {
        monitorHeld.countDown();

        // give the registration time to start waiting for the monitor
        Thread.sleep(200);

        whileHoldingMonitor.run();
      }
    });

    var registrationThread = startDaemon(failures, () -> {
      monitorHeld.await();
      registration.run();
    });

    monitorThread.join(5000);
    registrationThread.join(5000);

    assertFalse(monitorThread.isAlive() || registrationThread.isAlive(), "registration deadlocks");
    assertTrue(failures.isEmpty(), () -> "unexpected failure: " + failures.peek());
  }


  private static @NotNull Thread startDaemon(@NotNull Queue<Throwable> failures,
                                             @NotNull InterruptibleRunnable runnable)
  {
    var thread = new Thread(() -> {
      try {
        runnable.run();
      } catch(Throwable ex) {
        failures.add(ex);
      }
    });

    thread.setDaemon(true);
    thread.start();

    return thread;
  }




  @FunctionalInterface
  private interface InterruptibleRunnable {
    void run() throws InterruptedException;
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
  private static class MarkerNamedFormatter implements NamedParameterFormatter
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




  /** Named formatter which is applied automatically on its configuration name {@code shared-config}. */
  private static class AutoApplyFormatter extends MarkerNamedFormatter
  {
    private AutoApplyFormatter(String name) {
      super(name);
    }


    @Override
    public boolean autoApplyOnNamedConfigParameter() {
      return true;
    }


    @Override
    public @NotNull Set<String> getParameterConfigNames() {
      return Set.of("shared-config");
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
