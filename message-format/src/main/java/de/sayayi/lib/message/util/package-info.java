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
 * Utility classes for the message format library.
 * <p>
 * This package provides supporting utilities used throughout the message format framework:
 * <ul>
 *   <li>{@link de.sayayi.lib.message.util.MessageUtil MessageUtil}: static helper methods for message processing
 *       and analysis</li>
 *   <li>{@link de.sayayi.lib.message.util.ParameterValueHelper ParameterValueHelper}: type-safe conversions for
 *       message parameter values</li>
 *   <li>{@link de.sayayi.lib.message.util.SortedStringMap SortedStringMap}: a compact, array-backed map with
 *       sorted string keys</li>
 *   <li>{@link de.sayayi.lib.message.util.SupplierDelegate SupplierDelegate}: a caching supplier decorator</li>
 *   <li>{@link de.sayayi.lib.message.util.AbstractAntlr4Parser AbstractAntlr4Parser}: base parser integrating
 *       ANTLR4 syntax error reporting with the message support framework</li>
 * </ul>
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
package de.sayayi.lib.message.util;
