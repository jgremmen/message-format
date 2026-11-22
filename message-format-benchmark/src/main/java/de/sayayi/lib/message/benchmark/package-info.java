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

/**
 * JMH benchmarks for the performance critical paths of the message format library. The benchmarks measure throughput
 * and are meant to be run with several thread counts (JMH option {@code -t}) to show how each path scales under
 * contention.
 * <p>
 * The benchmarks in this package are:
 * <ul>
 *   <li>
 *     {@link de.sayayi.lib.message.benchmark.FormatterLookupBenchmark FormatterLookupBenchmark}: formatter lookup in
 *     the formatter service, which runs for every formatted parameter
 *   </li>
 *   <li>
 *     {@link de.sayayi.lib.message.benchmark.MessageFormatBenchmark MessageFormatBenchmark}: end to end formatting of
 *     registered messages and message texts
 *   </li>
 *   <li>
 *     {@link de.sayayi.lib.message.benchmark.MessageParseBenchmark MessageParseBenchmark}: parsing of message texts
 *     without a message cache
 *   </li>
 * </ul>
 *
 * @since 0.26.0
 */
package de.sayayi.lib.message.benchmark;
