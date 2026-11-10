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
 * Internal implementations of the message format API.
 * <p>
 * This package contains:
 * <ul>
 *   <li>
 *     {@link MessageSupportImpl} for managing messages and templates
 *   </li>
 *   <li>
 *     {@link MessageTemplate} for representing message templates
 *   </li>
 *   <li>
 *     {@link MessageParameters} and {@link NoParameters} for formatting parameters
 *   </li>
 *   <li>
 *     {@code ValueProtector} for protecting supplier parameter values from being evaluated more than once
 *   </li>
 * </ul>
 * <p>
 * <strong>This package is not part of the public API.</strong> Its classes are subject to change without notice and
 * should not be referenced directly by application code.
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
package de.sayayi.lib.message.internal;
