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
 * Exceptions thrown by the message format library during parsing, formatting and message management.
 * <p>
 * All exceptions are unchecked and derive from {@link de.sayayi.lib.message.exception.MessageException
 * MessageException}:
 * <ul>
 *   <li>{@link de.sayayi.lib.message.exception.MessageParserException MessageParserException}: a message or template
 *       format string could not be parsed</li>
 *   <li>{@link de.sayayi.lib.message.exception.MessageFormatException MessageFormatException}: a message or template
 *       could not be formatted</li>
 *   <li>{@link de.sayayi.lib.message.exception.DuplicateMessageException DuplicateMessageException} and
 *       {@link de.sayayi.lib.message.exception.DuplicateTemplateException DuplicateTemplateException}: a message code
 *       or template name is already in use by a message or template with different content</li>
 *   <li>{@link de.sayayi.lib.message.exception.MessageAdopterException MessageAdopterException}: messages or
 *       templates could not be adopted from an external source</li>
 *   <li>{@link de.sayayi.lib.message.exception.FormatterServiceException FormatterServiceException}: a formatter
 *       could not be registered with a formatter service</li>
 * </ul>
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
package de.sayayi.lib.message.exception;
