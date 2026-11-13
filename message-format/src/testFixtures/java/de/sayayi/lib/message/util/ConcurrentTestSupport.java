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
package de.sayayi.lib.message.util;

import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * Helpers for running test code in multiple threads at once.
 * <p>
 * All threads are released together by a start latch, so they actually run concurrently. Every failure in a thread,
 * including assertion errors, is rethrown in the calling thread, and a thread that does not finish within
 * {@value #TIMEOUT_SECONDS} seconds fails the test.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
public final class ConcurrentTestSupport
{
  /** Maximum number of seconds a thread may run. */
  private static final long TIMEOUT_SECONDS = 30;


  private ConcurrentTestSupport() {
  }


  /**
   * Runs {@code task} concurrently in {@code threads} threads. Each thread receives its index ({@code 0..threads-1}).
   *
   * @param threads  number of threads
   * @param task     task to run in each thread, not {@code null}
   *
   * @throws Exception  the first failure of a thread, in thread order
   */
  public static void runConcurrently(int threads, @NotNull ThreadTask task) throws Exception
  {
    getConcurrently(threads, thread -> {
      task.run(thread);
      return null;
    });
  }


  /**
   * Runs {@code task} concurrently in {@code threads} threads and returns the results in thread order. Each thread
   * receives its index ({@code 0..threads-1}).
   *
   * @param threads  number of threads
   * @param task     task to run in each thread, not {@code null}
   *
   * @return  list with the result of each thread, never {@code null}
   *
   * @throws Exception  the first failure of a thread, in thread order
   */
  public static <T> @NotNull List<T> getConcurrently(int threads, @NotNull ThreadFunction<T> task) throws Exception
  {
    val executor = newFixedThreadPool(threads);

    try {
      val start = new CountDownLatch(1);
      val futures = new ArrayList<Future<T>>(threads);

      for(var t = 0; t < threads; t++)
      {
        val thread = t;

        futures.add(executor.submit(() -> {
          start.await();
          return task.apply(thread);
        }));
      }

      start.countDown();

      val results = new ArrayList<T>(threads);
      for(val future: futures)
        results.add(get(future));

      return results;
    } finally {
      shutdown(executor);
    }
  }


  /**
   * Runs {@code writer} concurrently in {@code writers} threads while {@code reader} runs repeatedly in {@code readers}
   * other threads. Each reader runs at least once and stops as soon as all writers have finished.
   *
   * @param writers  number of writer threads
   * @param writer   task to run once in each writer thread, not {@code null}
   * @param readers  number of reader threads
   * @param reader   task to run repeatedly in each reader thread, not {@code null}
   *
   * @throws Exception  the first failure of a writer, or else the first failure of a reader
   */
  public static void runWithReaders(int writers, @NotNull ThreadTask writer, int readers, @NotNull ThreadTask reader)
      throws Exception
  {
    val executor = newFixedThreadPool(writers + readers);
    val writing = new AtomicBoolean(true);

    try {
      val start = new CountDownLatch(1);
      val writerFutures = new ArrayList<Future<?>>(writers);
      val readerFutures = new ArrayList<Future<?>>(readers);

      for(var t = 0; t < writers; t++)
      {
        val thread = t;

        writerFutures.add(executor.submit(() -> {
          start.await();
          writer.run(thread);
          return null;
        }));
      }

      for(var t = 0; t < readers; t++)
      {
        val thread = t;

        readerFutures.add(executor.submit(() -> {
          start.await();

          do {
            reader.run(thread);
          } while(writing.get());

          return null;
        }));
      }

      start.countDown();

      try {
        for(val future: writerFutures)
          get(future);
      } finally {
        writing.set(false);
      }

      for(val future: readerFutures)
        get(future);
    } finally {
      shutdown(executor);
    }
  }


  /**
   * Runs {@code iterations} randomly chosen {@code operations} in each of {@code threads} concurrent threads.
   *
   * @param threads     number of threads
   * @param iterations  number of operations per thread
   * @param operations  operations to choose from, not {@code null}
   *
   * @throws Exception  the first failure of a thread, in thread order
   */
  public static void stress(int threads, int iterations, @NotNull StressOperation @NotNull ... operations)
      throws Exception
  {
    runConcurrently(threads, thread -> {
      val random = ThreadLocalRandom.current();

      for(var iteration = 0; iteration < iterations; iteration++)
        operations[random.nextInt(operations.length)].run(thread, iteration);
    });
  }


  private static <T> T get(@NotNull Future<T> future) throws Exception
  {
    try {
      return future.get(TIMEOUT_SECONDS, SECONDS);
    } catch(ExecutionException ex) {
      if (ex.getCause() instanceof Exception cause)
        throw cause;

      throw (Error)ex.getCause();
    } catch(TimeoutException ex) {
      return fail("thread did not finish within " + TIMEOUT_SECONDS + " seconds", ex);
    }
  }


  private static void shutdown(@NotNull ExecutorService executor) throws InterruptedException
  {
    executor.shutdownNow();
    executor.awaitTermination(5, SECONDS);
  }




  /** Task run in a thread. */
  @FunctionalInterface
  public interface ThreadTask
  {
    /**
     * Runs the task.
     *
     * @param thread  index of the thread
     *
     * @throws Exception  if the task fails
     */
    void run(int thread) throws Exception;
  }




  /** Task run in a thread, returning a result. */
  @FunctionalInterface
  public interface ThreadFunction<T>
  {
    /**
     * Runs the task.
     *
     * @param thread  index of the thread
     *
     * @return  result of the task
     *
     * @throws Exception  if the task fails
     */
    T apply(int thread) throws Exception;
  }




  /** Operation run by {@link #stress(int, int, StressOperation...) stress}. */
  @FunctionalInterface
  public interface StressOperation
  {
    /**
     * Runs the operation.
     *
     * @param thread     index of the thread
     * @param iteration  iteration within the thread
     *
     * @throws Exception  if the operation fails
     */
    void run(int thread, int iteration) throws Exception;
  }
}
