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

import de.sayayi.lib.message.formatter.DefaultFormatterService;
import de.sayayi.lib.message.formatter.FormatterService;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter;
import org.openjdk.jmh.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static java.util.concurrent.TimeUnit.MICROSECONDS;


/**
 * Measures the formatter lookup of
 * {@link FormatterService#getFormatters(String, Class, de.sayayi.lib.message.part.MessagePart.Config) getFormatters},
 * which runs for every formatted parameter. All types are cached, so this measures the cache hit path.
 * <p>
 * Run with several thread counts (JMH option {@code -t}) to see how the lookup scales under contention.
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
public class FormatterLookupBenchmark
{
  /** Value types looked up, the number of types must be a power of 2. */
  private static final Class<?>[] TYPES = {
      String.class, Integer.class, Long.class, Double.class,
      BigDecimal.class, LocalDate.class, List.class, Map.class
  };

  /** Formatter service under test, with all {@link #TYPES} already in its formatter cache. */
  private FormatterService formatterService;


  /**
   * Creates the formatter service and looks up the formatters for all {@link #TYPES} once, so the benchmark only
   * measures cache hits.
   */
  @Setup
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public void setup()
  {
    formatterService = new DefaultFormatterService();

    // fill the formatter cache
    for(var type: TYPES)
      formatterService.getFormatters(null, type, null);
  }


  /**
   * Looks up the formatters for the next type of the calling thread.
   *
   * @param cursor  per-thread cursor selecting the type to look up, not {@code null}
   *
   * @return  formatters for the selected type, never {@code null}
   */
  @Benchmark
  public ParameterFormatter[] getFormatters(TypeCursor cursor) {
    return formatterService.getFormatters(null, TYPES[cursor.next()], null);
  }




  /** Per-thread position in {@link #TYPES}, starting at a random type so threads do not run in lockstep. */
  @State(Scope.Thread)
  public static class TypeCursor
  {
    /** Position counter; only its lower bits are used as index into {@link FormatterLookupBenchmark#TYPES TYPES}. */
    private int n = ThreadLocalRandom.current().nextInt(TYPES.length);


    /**
     * Returns the index of the next type and advances the cursor, wrapping around at the end of
     * {@link FormatterLookupBenchmark#TYPES TYPES}.
     *
     * @return  index into {@link FormatterLookupBenchmark#TYPES TYPES}, in the range {@code 0} to
     *          {@code TYPES.length - 1}
     */
    int next() {
      return n++ & (TYPES.length - 1);
    }
  }
}
