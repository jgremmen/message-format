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
 * Parts, map keys and accessors that form the building blocks of a parsed message format.
 * <p>
 * These types are mainly used by formatter implementations to inspect the message being formatted:
 * <ul>
 *   <li>{@link de.sayayi.lib.message.part.MessagePart MessagePart}: a part of a message, such as text, a parameter, a
 *       template reference or a post format part, together with its configuration and map</li>
 *   <li>{@link de.sayayi.lib.message.part.MapKey MapKey}: a key in a parameter configuration map and the result of
 *       matching a value against it</li>
 *   <li>{@link de.sayayi.lib.message.part.TypedValue TypedValue}: a typed configuration or map value</li>
 *   <li>{@link de.sayayi.lib.message.part.ConfigAccessor ConfigAccessor} and
 *       {@link de.sayayi.lib.message.part.MapAccessor MapAccessor}: typed access to the configuration and map of a
 *       message part</li>
 *   <li>{@link de.sayayi.lib.message.part.TextPartFactory TextPartFactory} and
 *       {@link de.sayayi.lib.message.part.TextJoiner TextJoiner}: create text parts and join them while handling
 *       spaces</li>
 * </ul>
 * <p>
 * Message part normalizers are located in the {@code normalizer} sub-package.
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
package de.sayayi.lib.message.part;
