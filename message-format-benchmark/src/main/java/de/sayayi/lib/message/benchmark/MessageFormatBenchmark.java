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
import org.openjdk.jmh.annotations.*;

import java.time.LocalDate;
import java.util.Locale;

import static de.sayayi.lib.message.part.normalizer.MessagePartNormalizer.PASS_THROUGH;
import static java.util.concurrent.TimeUnit.MICROSECONDS;


/**
 * Measures end-to-end message formatting with string, integer, decimal and date parameters.
 * <ul>
 *   <li>{@code formatByCode}: formats a registered message, as used by message bundles.</li>
 *   <li>
 *     {@code formatText}: formats a message text, which is parsed once and then taken from the message factory cache,
 *     as used by {@code MessageSupport.message(String)} and the Log4j message factory.
 *   </li>
 * </ul>
 * Run with several thread counts (JMH option {@code -t}) to see how formatting scales under contention.
 *
 * @author Jeroen Gremmen
 * @since 0.25.1
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(MICROSECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
public class MessageFormatBenchmark
{
  private static final String MESSAGE =
      "Hello %{name}, %{count} orders totalling %{amount,number:'#,##0.00'} have been shipped on %{date,date:medium}";

  private static final LocalDate DATE = LocalDate.of(2026, 11, 20);

  private MessageSupport messageSupport;


  @Setup
  public void setup()
  {
    final var configurableMessageSupport =
        MessageSupportFactory.create(new DefaultFormatterService(), new MessageFactory(PASS_THROUGH, 256));

    configurableMessageSupport.addMessage("ORDERS-SHIPPED", MESSAGE);
    messageSupport = configurableMessageSupport;
  }


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
