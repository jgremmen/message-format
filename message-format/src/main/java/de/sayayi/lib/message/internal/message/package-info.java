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
 * Contains internal implementations of the message API.
 * <p>
 * Classes in this package include:
 * <ul>
 *   <li>
 *     {@link TextMessage} and {@link CompoundMessage} for text and multipart messages
 *   </li>
 *   <li>
 *     {@link EmptyMessage} for empty messages
 *   </li>
 *   <li>
 *     {@link EmptyMessageWithCode}, {@link MessageDelegateWithCode} and {@link LocalizedMessageBundleWithCode} for
 *     messages associated with codes
 *   </li>
 * </ul>
 * {@link AbstractMessageWithCode} is the base class for messages associated with codes.
 * <p>
 * <strong>This package is not part of the public API.</strong> Its classes are subject to change without notice and
 * should not be referenced directly by application code.
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
package de.sayayi.lib.message.internal.message;
