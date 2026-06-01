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
 * Core API for the message format library. This package provides the primary interfaces and classes for parsing,
 * formatting, and managing parameterized messages with locale support.
 * <p>
 * The main types in this package are:
 * <ul>
 *   <li>{@link de.sayayi.lib.message.Message Message} – the parsed representation of a message format string,
 *       composed of message parts that can be formatted with parameter values</li>
 *   <li>{@link de.sayayi.lib.message.MessageSupport MessageSupport} – central registry for messages and templates,
 *       providing message access, configuration, and formatting capabilities</li>
 *   <li>{@link de.sayayi.lib.message.MessageFactory MessageFactory} – factory for parsing message format strings
 *       into {@code Message} instances</li>
 *   <li>{@link de.sayayi.lib.message.MessageSupportFactory MessageSupportFactory} – factory for creating
 *       {@code MessageSupport} instances</li>
 *   <li>{@link de.sayayi.lib.message.MessageBuilder MessageBuilder} – fluent builder for constructing and formatting
 *       messages with named parameters</li>
 * </ul>
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
package de.sayayi.lib.message;
