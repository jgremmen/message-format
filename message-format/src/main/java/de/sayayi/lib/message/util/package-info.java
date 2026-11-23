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
 * Utilities which are used throughout the message format library and are available to applications and formatter
 * implementations as well.
 * <p>
 * The package provides the following classes:
 * <ul>
 *   <li>
 *     {@link de.sayayi.lib.message.util.MessageUtil MessageUtil}: static helpers for name validation, space handling,
 *     format string serialization and pack file import
 *   </li>
 *   <li>
 *     {@link de.sayayi.lib.message.util.ParameterValueHelper ParameterValueHelper}: type-safe conversions of message
 *     parameter values
 *   </li>
 *   <li>
 *     {@link de.sayayi.lib.message.util.SortedStringMap SortedStringMap}: a compact, array-backed map with sorted
 *     string keys, which can be sealed to make it immutable
 *   </li>
 *   <li>
 *     {@link de.sayayi.lib.message.util.SupplierDelegate SupplierDelegate}: a supplier caching the value of another
 *     supplier, which is invoked once while holding a lock
 *   </li>
 *   <li>
 *     {@link de.sayayi.lib.message.util.SharedInstanceSupplier SharedInstanceSupplier}: a supplier of a lazily created
 *     instance shared by all threads, which is created without holding a lock
 *   </li>
 *   <li>
 *     {@link de.sayayi.lib.message.util.AbstractAntlr4Parser AbstractAntlr4Parser}: base class for ANTLR4 parsers
 *     reporting syntax errors with messages formatted by a message support
 *   </li>
 * </ul>
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
package de.sayayi.lib.message.util;
