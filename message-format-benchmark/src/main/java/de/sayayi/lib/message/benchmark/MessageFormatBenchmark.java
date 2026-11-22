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

import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import org.intellij.lang.annotations.Language;
import org.openjdk.jmh.annotations.*;

import java.time.LocalDate;
import java.util.Locale;

import static de.sayayi.lib.message.part.normalizer.MessagePartNormalizer.PASS_THROUGH;
import static java.util.concurrent.TimeUnit.MICROSECONDS;


/**
 * Measures end-to-end message formatting with string, integer, decimal and date parameters.
 * <ul>
 *   <li>{@code formatByCode}: formats a registered message, as used by message bundles.</li>
 *   <li>{@code formatPatterns}: formats custom number and date patterns, which are cached by their formatters.</li>
 *   <li>
 *     {@code formatText}: formats a message text, which is parsed once and then taken from the message factory cache,
 *     as used by {@code MessageSupport.message(String)} and the Log4j message factory.
 *   </li>
 * </ul>
 * <p>
 * Run with several thread counts (JMH option {@code -t}) to see how formatting scales under contention.
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
public class MessageFormatBenchmark
{
  /** Message with string, integer, decimal and date parameters, registered under the code {@code ORDERS-SHIPPED}. */
  @Language("MessageFormat")
  private static final String MESSAGE =
      "Hello %{name}, %{count} orders totalling %{amount,number:'#,##0.00'} have been shipped on %{date,date:medium}";

  /** Message with custom number and date patterns, which are taken from the formatters' pattern caches. */
  @Language("MessageFormat")
  private static final String PATTERN_MESSAGE =
      "%{amount,number:'0.00'} / %{ratio,number:'0.0%'} on %{date,date:'yyyy-MM-dd'}";

  /** Date parameter value used by all benchmarks. */
  private static final LocalDate DATE = LocalDate.of(2026, 11, 20);

  /** Message support under test, with {@link #MESSAGE} registered as {@code ORDERS-SHIPPED}. */
  private MessageSupport messageSupport;


  /**
   * Creates the message support, using a message factory with a message cache of 256 entries, and registers
   * {@link #MESSAGE} under the code {@code ORDERS-SHIPPED}.
   */
  @Setup
  public void setup()
  {
    final var configurableMessageSupport =
        MessageSupportFactory.create(new DefaultFormatterService(), new MessageFactory(PASS_THROUGH, 256));

    configurableMessageSupport.addMessage("ORDERS-SHIPPED", MESSAGE);
    messageSupport = configurableMessageSupport;
  }


  /**
   * Formats the registered message {@code ORDERS-SHIPPED}.
   *
   * @return  formatted message, never {@code null}
   */
  @Benchmark
  public String formatByCode()
  {
    return messageSupport
        .code("ORDERS-SHIPPED")
        .with("name", "Alice")
        .with("count", 42)
        .with("amount", 1234.5)
        .with("date", DATE)
        .locale(Locale.US)
        .format();
  }


  /**
   * Formats {@link #PATTERN_MESSAGE}, which uses custom number and date patterns.
   *
   * @return  formatted message, never {@code null}
   */
  @Benchmark
  public String formatPatterns()
  {
    return messageSupport
        .message(PATTERN_MESSAGE)
        .with("amount", 1234.5)
        .with("ratio", 0.125)
        .with("date", DATE)
        .locale(Locale.US)
        .format();
  }


  /**
   * Formats the message text {@link #MESSAGE}, which is taken from the message factory cache after the first call.
   *
   * @return  formatted message, never {@code null}
   */
  @Benchmark
  public String formatText()
  {
    return messageSupport
        .message(MESSAGE)
        .with("name", "Alice")
        .with("count", 42)
        .with("amount", 1234.5)
        .with("date", DATE)
        .locale(Locale.US)
        .format();
  }
}
