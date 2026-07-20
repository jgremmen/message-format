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
 * Named parameter formatter implementations that integrate ICU4J formatting capabilities into the message-format
 * library.
 * <p>
 * This package provides the following formatters:
 * <ul>
 *   <li>
 *     {@link de.sayayi.lib.message.icu.formatter.ICUFormatter ICUFormatter} ({@code icu}) – formats parameter values
 *     using ICU {@link com.ibm.icu.text.MessageFormat MessageFormat} patterns, providing access to ICU's rich
 *     locale-sensitive formatting features such as plurals, select and number/date formatting.
 *   </li>
 *   <li>
 *     {@link de.sayayi.lib.message.icu.formatter.ICUPersonFormatter ICUPersonFormatter} ({@code icu-person}) – formats
 *     person names using the ICU {@link com.ibm.icu.text.PersonNameFormatter PersonNameFormatter}, with configurable
 *     control over name part visibility, ordering, formality and length.
 *   </li>
 * </ul>
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 */
package de.sayayi.lib.message.icu.formatter;
