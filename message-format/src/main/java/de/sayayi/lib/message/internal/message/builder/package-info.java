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
 * Provides the internal builders that implement the public message building API.
 * <p>
 * {@link FluentMessageBuilder} assembles messages from text, parameter, post-formatter and template parts. The other
 * builders configure individual parts before they are added to a message. These classes are implementation details and
 * are not intended for direct application use.
 *
 * @see de.sayayi.lib.message.MessageBuilder
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
package de.sayayi.lib.message.internal.message.builder;
