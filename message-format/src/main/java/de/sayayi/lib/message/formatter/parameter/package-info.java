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
 * APIs and implementations for formatting message parameter values, including formatter selection and the context
 * available to parameter formatters.
 * <p>
 * This package defines the parameter formatter SPI. Built-in implementations are located in the {@code named} and
 * {@code runtime} sub-packages. The main types are:
 * <ul>
 *   <li>{@link de.sayayi.lib.message.formatter.parameter.ParameterFormatter ParameterFormatter}: converts a parameter
 *       value into text and defines optional capabilities such as size queries and map key comparison</li>
 *   <li>{@link de.sayayi.lib.message.formatter.parameter.NamedParameterFormatter NamedParameterFormatter}: a parameter
 *       formatter that can be selected by name in a message format</li>
 *   <li>{@link de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext ParameterFormatterContext}: the
 *       context passed to a parameter formatter while formatting a value</li>
 *   <li>{@link de.sayayi.lib.message.formatter.parameter.AbstractParameterFormatter AbstractParameterFormatter}: base
 *       class handling {@code null} and empty values</li>
 *   <li>{@link de.sayayi.lib.message.formatter.parameter.AbstractSingleTypeParameterFormatter
 *       AbstractSingleTypeParameterFormatter}: base class for formatters supporting a single type</li>
 *   <li>{@link de.sayayi.lib.message.formatter.parameter.AbstractMultiSelectFormatter AbstractMultiSelectFormatter}:
 *       base class for formatters selecting a formatting function by configuration value</li>
 *   <li>{@link de.sayayi.lib.message.formatter.parameter.SingletonParameters SingletonParameters}: lightweight
 *       parameters holding a single, reusable parameter value</li>
 * </ul>
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
package de.sayayi.lib.message.formatter.parameter;
