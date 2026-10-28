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
 * This package is the main entry point of the library. Applications typically obtain a {@code MessageSupport} from
 * {@code MessageSupportFactory}, register messages and templates with it and format messages by code.
 * <p>
 * The main types in this package are:
 * <ul>
 *   <li>
 *     {@link de.sayayi.lib.message.Message Message}: the parsed, immutable representation of a message format string
 *   </li>
 *   <li>
 *     {@link de.sayayi.lib.message.MessageSupport MessageSupport}: looks up, configures and formats messages
 *   </li>
 *   <li>
 *     {@link de.sayayi.lib.message.MessageSupportFactory MessageSupportFactory}: creates message support instances
 *   </li>
 *   <li>
 *     {@link de.sayayi.lib.message.MessageFactory MessageFactory}: parses message format strings into messages and
 *     templates
 *   </li>
 *   <li>
 *     {@link de.sayayi.lib.message.MessageBuilder MessageBuilder}: builds messages programmatically
 *   </li>
 *   <li>
 *     {@link de.sayayi.lib.message.SpacesAware SpacesAware}: tracks leading and trailing spaces of messages and
 *     message parts
 *   </li>
 *   <li>
 *     {@link de.sayayi.lib.message.FormatStringSerializer FormatStringSerializer}: serializes messages back into
 *     their message format string representation
 *   </li>
 * </ul>
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
package de.sayayi.lib.message;
