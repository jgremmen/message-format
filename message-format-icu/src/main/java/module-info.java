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
 * Provides ICU4J-based parameter formatters for the message-format library.
 * <p>
 * This module extends the core {@code de.sayayi.lib.message} library with formatters that leverage the
 * <a href="https://unicode-org.github.io/icu/userguide/icu4j/">ICU4J</a> library for advanced locale-sensitive
 * formatting:
 * <ul>
 *   <li>
 *     {@link de.sayayi.lib.message.icu.formatter.ICUFormatter ICUFormatter} ({@code icu}): formats parameter values
 *     using ICU {@link com.ibm.icu.text.MessageFormat MessageFormat} patterns, supporting plurals, select expressions
 *     and locale-aware number/date formatting.
 *   </li>
 *   <li>
 *     {@link de.sayayi.lib.message.icu.formatter.ICUPersonFormatter ICUPersonFormatter} ({@code icu-person}): formats
 *     person names with configurable control over name part visibility, ordering, formality and length.
 *   </li>
 * </ul>
 * <p>
 * Both formatters are located in the exported package {@code de.sayayi.lib.message.icu.formatter}. They are listed in
 * a {@code META-INF/services} file, so they are registered automatically by the
 * {@link de.sayayi.lib.message.formatter.DefaultFormatterService DefaultFormatterService} when this module is on the
 * class path. On the module path, they must be registered explicitly.
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 */
module de.sayayi.lib.message.icu
{
  requires transitive de.sayayi.lib.message;

  requires com.ibm.icu;

  requires static org.jetbrains.annotations;

  exports de.sayayi.lib.message.icu.formatter;
}
