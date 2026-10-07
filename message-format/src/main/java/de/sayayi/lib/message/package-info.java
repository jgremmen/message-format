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
 * Core API for parsing, formatting and managing parameterized messages with locale support.
 * <p>
 * The main types in this package include:
 * <ul>
 *   <li>
 *     {@link de.sayayi.lib.message.Message Message} for formatted messages and their message parts
 *   </li>
 *   <li>
 *     {@link de.sayayi.lib.message.MessageSupport MessageSupport} for accessing and configuring messages
 *   </li>
 *   <li>
 *     {@link de.sayayi.lib.message.MessageFactory MessageFactory} for parsing message format strings
 *   </li>
 *   <li>
 *     {@link de.sayayi.lib.message.MessageSupportFactory MessageSupportFactory} for creating message support
 *   </li>
 *   <li>
 *     {@link de.sayayi.lib.message.MessageBuilder MessageBuilder} for building messages with named parameters
 *   </li>
 * </ul>
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
package de.sayayi.lib.message;
