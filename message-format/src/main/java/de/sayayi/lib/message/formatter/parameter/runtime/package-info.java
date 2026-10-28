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
 * Built-in parameter formatter implementations for common Java runtime types such as numbers, enums, collections,
 * maps, temporals, optionals and more.
 * <p>
 * The formatters in this package are selected by the type of the parameter value and are registered automatically by
 * the {@link de.sayayi.lib.message.formatter.DefaultFormatterService DefaultFormatterService}:
 * <ul>
 *   <li>Numbers and booleans:
 *       {@link de.sayayi.lib.message.formatter.parameter.runtime.NumberFormatter NumberFormatter} and
 *       {@link de.sayayi.lib.message.formatter.parameter.runtime.AtomicBooleanFormatter AtomicBooleanFormatter}</li>
 *   <li>Lists and maps: {@link de.sayayi.lib.message.formatter.parameter.runtime.ArrayFormatter ArrayFormatter},
 *       {@link de.sayayi.lib.message.formatter.parameter.runtime.IterableFormatter IterableFormatter},
 *       {@link de.sayayi.lib.message.formatter.parameter.runtime.MapFormatter MapFormatter},
 *       {@link de.sayayi.lib.message.formatter.parameter.runtime.MapEntryFormatter MapEntryFormatter},
 *       {@link de.sayayi.lib.message.formatter.parameter.runtime.DictionaryFormatter DictionaryFormatter} and
 *       {@link de.sayayi.lib.message.formatter.parameter.runtime.BitSetFormatter BitSetFormatter}, most of them based
 *       on {@link de.sayayi.lib.message.formatter.parameter.runtime.AbstractListFormatter AbstractListFormatter}</li>
 *   <li>Wrappers that format the value they contain or supply: {@code Optional*Formatter}, {@code *SupplierFormatter}
 *       and {@link de.sayayi.lib.message.formatter.parameter.runtime.ReferenceFormatter ReferenceFormatter}</li>
 *   <li>Date and time: {@link de.sayayi.lib.message.formatter.parameter.runtime.TemporalFormatter TemporalFormatter},
 *       {@link de.sayayi.lib.message.formatter.parameter.runtime.ToTemporalDelegate ToTemporalDelegate} (for legacy
 *       date types) and {@link de.sayayi.lib.message.formatter.parameter.runtime.TimeZoneFormatter TimeZoneFormatter}
 *   </li>
 *   <li>Other runtime types: {@link de.sayayi.lib.message.formatter.parameter.runtime.ByteArrayFormatter
 *       ByteArrayFormatter}, {@link de.sayayi.lib.message.formatter.parameter.runtime.CharsetFormatter
 *       CharsetFormatter}, {@link de.sayayi.lib.message.formatter.parameter.runtime.EnumFormatter EnumFormatter},
 *       {@link de.sayayi.lib.message.formatter.parameter.runtime.LocaleFormatter LocaleFormatter},
 *       {@link de.sayayi.lib.message.formatter.parameter.runtime.PathFormatter PathFormatter},
 *       {@link de.sayayi.lib.message.formatter.parameter.runtime.ThrowableFormatter ThrowableFormatter},
 *       {@link de.sayayi.lib.message.formatter.parameter.runtime.TypeFormatter TypeFormatter},
 *       {@link de.sayayi.lib.message.formatter.parameter.runtime.URIFormatter URIFormatter} and
 *       {@link de.sayayi.lib.message.formatter.parameter.runtime.URLFormatter URLFormatter}</li>
 * </ul>
 * <p>
 * Formatters for more specialized types, which are not registered by default, are located in the {@code extra}
 * sub-package.
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
package de.sayayi.lib.message.formatter.parameter.runtime;
