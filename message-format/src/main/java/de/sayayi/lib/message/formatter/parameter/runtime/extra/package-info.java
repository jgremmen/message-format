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
 * Additional parameter formatter implementations for specialized Java runtime types such as reflection types,
 * network addresses, XML nodes, security principals, ANTLR tokens and more.
 * <p>
 * Unlike the formatters in the {@code runtime} package, these formatters are not registered by default and must be
 * added to a formatter service explicitly:
 * <ul>
 *   <li>Reflection: {@link de.sayayi.lib.message.formatter.parameter.runtime.extra.AnnotationFormatter
 *       AnnotationFormatter}, {@link de.sayayi.lib.message.formatter.parameter.runtime.extra.FieldFormatter
 *       FieldFormatter}, {@link de.sayayi.lib.message.formatter.parameter.runtime.extra.MethodFormatter
 *       MethodFormatter}, {@link de.sayayi.lib.message.formatter.parameter.runtime.extra.MethodParameterFormatter
 *       MethodParameterFormatter} and {@link de.sayayi.lib.message.formatter.parameter.runtime.extra.PackageFormatter
 *       PackageFormatter}</li>
 *   <li>Security: {@link de.sayayi.lib.message.formatter.parameter.runtime.extra.KeyFormatter KeyFormatter} and
 *       {@link de.sayayi.lib.message.formatter.parameter.runtime.extra.PrincipalFormatter PrincipalFormatter}</li>
 *   <li>Miscellaneous: {@link de.sayayi.lib.message.formatter.parameter.runtime.extra.CollationKeyFormatter
 *       CollationKeyFormatter}, {@link de.sayayi.lib.message.formatter.parameter.runtime.extra.InetAddressFormatter
 *       InetAddressFormatter}, {@link de.sayayi.lib.message.formatter.parameter.runtime.extra.ManifestFormatter
 *       ManifestFormatter}, {@link de.sayayi.lib.message.formatter.parameter.runtime.extra.MatchResultFormatter
 *       MatchResultFormatter}, {@link de.sayayi.lib.message.formatter.parameter.runtime.extra.NodeFormatter
 *       NodeFormatter} (XML DOM nodes) and
 *       {@link de.sayayi.lib.message.formatter.parameter.runtime.extra.TokenFormatter TokenFormatter} (ANTLR
 *       tokens)</li>
 * </ul>
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
package de.sayayi.lib.message.formatter.parameter.runtime.extra;
