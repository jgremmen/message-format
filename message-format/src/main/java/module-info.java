/*
 * Copyright 2025 Jeroen Gremmen
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
 * Message Format – a Java library for parsing, formatting and managing parameterized, locale-aware messages.
 *
 * <p>The central entry point is {@link de.sayayi.lib.message.MessageSupport MessageSupport}, which lets you look up
 * messages by code, bind parameters and format them for a given locale. Instances are obtained through
 * {@link de.sayayi.lib.message.MessageSupportFactory MessageSupportFactory}.
 *
 * <h2>Core concepts</h2>
 * <ul>
 *   <li>
 *     {@link de.sayayi.lib.message.Message Message} – the parsed, immutable representation of a message format string,
 *     composed of text, parameter and template parts.
 *   </li>
 *   <li>
 *     {@link de.sayayi.lib.message.MessageFactory MessageFactory} – parses message format strings (and locale-keyed
 *     maps) into {@code Message} instances.
 *   </li>
 *   <li>
 *     {@link de.sayayi.lib.message.MessageBuilder MessageBuilder} – fluent builder for constructing messages
 *     programmatically.
 *   </li>
 * </ul>
 *
 * <h2>Formatting</h2>
 * <p>Parameter values are converted to text by
 * {@linkplain de.sayayi.lib.message.formatter.parameter.ParameterFormatter parameter formatters}, which are type-based
 * or {@linkplain de.sayayi.lib.message.formatter.parameter.NamedParameterFormatter named}. Additional transformations
 * can be applied through {@linkplain de.sayayi.lib.message.formatter.post.PostFormatter post-formatters}. Both
 * formatter types are discovered via the {@link java.util.ServiceLoader} mechanism.
 *
 * <h2>Message sources</h2>
 * <p>Messages can be loaded from properties files or resource bundles using the
 * {@linkplain de.sayayi.lib.message.adopter adopter} classes, or packed into a compact binary format for efficient
 * storage and distribution.
 *
 * <h2>Message parts and utilities</h2>
 * <p>The {@link de.sayayi.lib.message.part} package exposes the building blocks of a parsed message (text, parameters,
 * templates, maps), while {@link de.sayayi.lib.message.util} provides general-purpose helper methods.
 *
 * @see de.sayayi.lib.message.MessageSupport
 * @see de.sayayi.lib.message.MessageFactory
 */
module de.sayayi.lib.message
{
  requires de.sayayi.lib.antlr;
  requires de.sayayi.lib.pack;
  requires org.antlr.antlr4.runtime;
  requires java.sql;

  requires static java.xml;
  requires static lombok;
  requires static org.apache.tika.core;
  requires static org.jetbrains.annotations;

  exports de.sayayi.lib.message;
  exports de.sayayi.lib.message.adopter;
  exports de.sayayi.lib.message.exception;
  exports de.sayayi.lib.message.formatter;
  exports de.sayayi.lib.message.formatter.parameter;
  exports de.sayayi.lib.message.formatter.parameter.named;
  exports de.sayayi.lib.message.formatter.parameter.named.extra;
  exports de.sayayi.lib.message.formatter.parameter.runtime;
  exports de.sayayi.lib.message.formatter.parameter.runtime.extra;
  exports de.sayayi.lib.message.formatter.post;
  exports de.sayayi.lib.message.formatter.post.runtime;
  exports de.sayayi.lib.message.part;
  exports de.sayayi.lib.message.part.normalizer;
  exports de.sayayi.lib.message.util;

  uses de.sayayi.lib.message.formatter.parameter.ParameterFormatter;
  uses de.sayayi.lib.message.formatter.post.PostFormatter;

  // internal service implementations
  provides java.nio.file.spi.FileTypeDetector with de.sayayi.lib.message.internal.pack.PackFileTypeDetector;
}
