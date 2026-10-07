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
 * Internal support classes used by the formatter service to look up and order parameter formatters.
 * <p>
 * The types in this package are implementation details of the formatter lookup process and are not part of the
 * public API.
 * <p>
 * The main types in this package are:
 * <ul>
 *   <li>{@link de.sayayi.lib.message.internal.formatter.FormatterServiceDelegate FormatterServiceDelegate} forwards
 *       formatter service calls to another {@code FormatterService}</li>
 *   <li>{@link de.sayayi.lib.message.internal.formatter.FormatterCache FormatterCache} caches the parameter
 *       formatters resolved for a value type so that repeated lookups for the same type are fast</li>
 *   <li>{@link de.sayayi.lib.message.internal.formatter.PrioritizedParameterFormatter PrioritizedParameterFormatter}
 *       pairs a parameter formatter with its registration priority so that formatters can be ordered</li>
 * </ul>
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
package de.sayayi.lib.message.internal.formatter;
