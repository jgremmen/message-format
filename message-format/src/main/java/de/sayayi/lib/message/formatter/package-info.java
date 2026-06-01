/*
 * Copyright 2019 Jeroen Gremmen
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
 * Formatter service and related infrastructure for resolving parameter formatters and post formatters during
 * message formatting.
 * <p>
 * The main types in this package are:
 * <ul>
 *   <li>{@link de.sayayi.lib.message.formatter.FormatterService FormatterService} – service interface for looking
 *       up parameter formatters by value type and name, and for managing post formatters</li>
 *   <li>{@link de.sayayi.lib.message.formatter.GenericFormatterService GenericFormatterService} – base implementation
 *       with a mutable formatter registry</li>
 *   <li>{@link de.sayayi.lib.message.formatter.DefaultFormatterService DefaultFormatterService} – pre-configured
 *       formatter service that auto-registers all built-in formatters</li>
 *   <li>{@link de.sayayi.lib.message.formatter.FormattableType FormattableType} – describes a type that a parameter
 *       formatter can handle, along with a priority order</li>
 * </ul>
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
package de.sayayi.lib.message.formatter;
