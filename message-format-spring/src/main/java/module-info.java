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
 * Spring Framework integration for the message format library.
 * <p>
 * This module provides:
 * <ul>
 *   <li>{@link de.sayayi.lib.message.spring.MessageSupportMessageSource MessageSupportMessageSource} – a Spring
 *       {@link org.springframework.context.MessageSource MessageSource} implementation backed by
 *       {@link de.sayayi.lib.message.MessageSupport MessageSupport}, allowing the message format library to serve
 *       as a drop-in replacement for Spring's message resolution</li>
 *   <li>{@link de.sayayi.lib.message.spring.formatter.SpELFormatter SpELFormatter} – a parameter formatter that
 *       evaluates Spring Expression Language (SpEL) expressions within message templates</li>
 * </ul>
 */
module de.sayayi.lib.message.spring
{
  requires transitive de.sayayi.lib.message;
  requires transitive de.sayayi.lib.message.annotations;

  requires spring.core;
  requires spring.context;
  requires spring.expression;

  requires static org.jetbrains.annotations;

  exports de.sayayi.lib.message.spring;
  exports de.sayayi.lib.message.spring.formatter;
}
