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
 * Named parameter formatter implementations that can be explicitly referenced by name in a message format, such as
 * {@code bool}, {@code choice}, {@code classifier}, {@code size} and {@code string}.
 * <p>
 * A named formatter is selected with the {@code format} configuration key, e.g. {@code %{p,format:bool}}:
 * <ul>
 *   <li>{@link de.sayayi.lib.message.formatter.parameter.named.BoolFormatter BoolFormatter} ({@code bool}): formats a
 *       value as a boolean</li>
 *   <li>{@link de.sayayi.lib.message.formatter.parameter.named.ChoiceFormatter ChoiceFormatter} ({@code choice}):
 *       selects a mapped message based on the value</li>
 *   <li>{@link de.sayayi.lib.message.formatter.parameter.named.ClassifierFormatter ClassifierFormatter}
 *       ({@code classifier}): selects a mapped message based on the classifiers of the value</li>
 *   <li>{@link de.sayayi.lib.message.formatter.parameter.named.SizeFormatter SizeFormatter} ({@code size}): formats
 *       the size of a value</li>
 *   <li>{@link de.sayayi.lib.message.formatter.parameter.named.StringFormatter StringFormatter} ({@code string}):
 *       formats the string representation of a value; also the default formatter for arbitrary objects</li>
 * </ul>
 * <p>
 * Additional named formatters for specialized purposes are located in the {@code extra} sub-package.
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
package de.sayayi.lib.message.formatter.parameter.named;
