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
package de.sayayi.lib.message.benchmark;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageFactory;
import org.intellij.lang.annotations.Language;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.ThreadLocalRandom;

import static de.sayayi.lib.message.part.normalizer.MessagePartNormalizer.PASS_THROUGH;
import static java.util.concurrent.TimeUnit.MICROSECONDS;


/**
 * Measures parsing of message texts without a message cache, as happens for every distinct text passed to
 * {@code MessageSupport.message(String)} or logged through the Log4j message factory that is not in the cache.
 * <ul>
 *   <li>{@code parsePlainText}: plain texts without parameters, like most log messages.</li>
 *   <li>{@code parseMessageWithParameters}: texts with parameters, which always require the parser.</li>
 * </ul>
 * <p>
 * Run with several thread counts (JMH option {@code -t}) to see how parsing scales under contention.
 *
 * @author Jeroen Gremmen
 * @since 0.26.0
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(MICROSECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
public class MessageParseBenchmark
{
  /** Plain message texts, the number of texts must be a power of 2. */
  @Language("MessageFormat")
  private static final String[] PLAIN_TEXTS = {
      "Application started in 3.42 seconds",
      "Connection to database server db-01.example.com:5432 established",
      "User 'alice' logged in from 192.168.1.17",
      "Cache refresh completed: 1,024 entries loaded, 12 expired",
      "Request GET /api/v1/orders/4711 completed with status 200 (35 ms)",
      "Scheduled job \"nightly-cleanup\" finished, next run at 02:00",
      "Größe der Warteschlange überschreitet den Grenzwert von 500 Einträgen",
      "Shutting down executor service, waiting for 3 running tasks"
  };

  /** Message texts with parameters, the number of texts must be a power of 2. */
  @Language("MessageFormat")
  private static final String[] PARAMETER_TEXTS = {
      "Application started in %{seconds} seconds",
      "Connection to %{host}:%{port} established",
      "User %{user} logged in from %{address}",
      "Cache refresh completed: %{loaded,number:'#,##0'} entries loaded, %{expired} expired",
      "Request %{method} %{path} completed with status %{status} (%{millis} ms)",
      "Scheduled job %{job} finished, next run at %{next,date:short}",
      "Queue size %{size} exceeds the limit of %{limit} entries",
      "Shutting down %{service}, waiting for %{tasks,1:'1 task',:'%{tasks} tasks'}"
  };

  /** Message factory under test, without a message cache. */
  private MessageFactory messageFactory;


  /** Creates a message factory without a message cache, so every call parses its text. */
  @Setup
  public void setup()
  {
    // no message cache: every call must parse the text
    messageFactory = new MessageFactory(PASS_THROUGH, 0);
  }


  /**
   * Parses the next plain text of the calling thread.
   *
   * @param cursor  per-thread cursor selecting the text to parse, not {@code null}
   *
   * @return  parsed message, never {@code null}
   */
  @Benchmark
  public Message.WithSpaces parsePlainText(TextCursor cursor) {
    return messageFactory.parseMessage(PLAIN_TEXTS[cursor.next()]);
  }


  /**
   * Parses the next message text with parameters of the calling thread.
   *
   * @param cursor  per-thread cursor selecting the text to parse, not {@code null}
   *
   * @return  parsed message, never {@code null}
   */
  @Benchmark
  public Message.WithSpaces parseMessageWithParameters(TextCursor cursor) {
    return messageFactory.parseMessage(PARAMETER_TEXTS[cursor.next()]);
  }




  /** Per-thread position in the text arrays, starting at a random text so threads do not run in lockstep. */
  @State(Scope.Thread)
  public static class TextCursor
  {
    /** Position counter; only its lower bits are used as index into the text arrays. */
    private int n = ThreadLocalRandom.current().nextInt(PLAIN_TEXTS.length);


    /**
     * Returns the index of the next text and advances the cursor, wrapping around at the end of the text arrays.
     *
     * @return  index into {@link MessageParseBenchmark#PLAIN_TEXTS PLAIN_TEXTS} and
     *          {@link MessageParseBenchmark#PARAMETER_TEXTS PARAMETER_TEXTS}, in the range {@code 0} to
     *          {@code PLAIN_TEXTS.length - 1}
     */
    int next() {
      return n++ & (PLAIN_TEXTS.length - 1);
    }
  }
}
