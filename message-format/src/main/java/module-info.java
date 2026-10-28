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

import de.sayayi.lib.message.template.AbstractNamedTemplate;

/**
 * Provides the message format library for parsing, formatting and managing parameterized, locale-aware messages.
 *
 * <p>The central entry point is {@link de.sayayi.lib.message.MessageSupport MessageSupport}, which lets you look up
 * messages by code, bind parameters and format them for a given locale. Instances are obtained through
 * {@link de.sayayi.lib.message.MessageSupportFactory MessageSupportFactory}.
 *
 * <h2>Core concepts</h2>
 * <ul>
 *   <li>
 *     {@link de.sayayi.lib.message.Message Message}: the parsed, immutable representation of a message format string,
 *     composed of text, parameter, template and post format parts.
 *   </li>
 *   <li>
 *     {@link de.sayayi.lib.message.MessageFactory MessageFactory}: parses message format strings (and locale-keyed
 *     maps) into {@code Message} instances.
 *   </li>
 *   <li>
 *     {@link de.sayayi.lib.message.MessageBuilder MessageBuilder}: fluent builder for constructing messages
 *     programmatically.
 *   </li>
 *   <li>
 *     {@link de.sayayi.lib.message.template.Template Template}: reusable template that can be registered by name
 *     and referenced from messages. Custom templates extend
 *     {@link AbstractNamedTemplate AbstractNamedTemplate} and can be discovered automatically
 *     via the {@link java.util.ServiceLoader} mechanism.
 *   </li>
 * </ul>
 *
 * <h2>Exported packages</h2>
 * <ul>
 *   <li>{@code de.sayayi.lib.message}: core API (messages, message support, factories and builders)</li>
 *   <li>{@code de.sayayi.lib.message.adopter}: adopters reading messages and templates from resource bundles and
 *       properties</li>
 *   <li>{@code de.sayayi.lib.message.exception}: exceptions thrown by the library</li>
 *   <li>{@code de.sayayi.lib.message.formatter}: formatter services resolving parameter and post formatters</li>
 *   <li>{@code de.sayayi.lib.message.formatter.parameter}: parameter formatter SPI and base classes</li>
 *   <li>{@code de.sayayi.lib.message.formatter.parameter.named} and {@code ...named.extra}: formatters selected by
 *       name</li>
 *   <li>{@code de.sayayi.lib.message.formatter.parameter.runtime} and {@code ...runtime.extra}: formatters selected by
 *       the type of the parameter value</li>
 *   <li>{@code de.sayayi.lib.message.formatter.post} and {@code ...post.runtime}: post formatter SPI and built-in post
 *       formatters</li>
 *   <li>{@code de.sayayi.lib.message.pack}: constants and Apache Tika detectors for packed messages</li>
 *   <li>{@code de.sayayi.lib.message.part} and {@code ...part.normalizer}: building blocks of parsed messages and
 *       message part normalizers</li>
 *   <li>{@code de.sayayi.lib.message.template}: template API</li>
 *   <li>{@code de.sayayi.lib.message.util}: general-purpose helper classes</li>
 * </ul>
 *
 * <h2>Formatting</h2>
 * <p>Parameter values are converted to text by
 * {@linkplain de.sayayi.lib.message.formatter.parameter.ParameterFormatter parameter formatters}, which are type-based
 * or {@linkplain de.sayayi.lib.message.formatter.parameter.NamedParameterFormatter named}. Additional transformations
 * can be applied through {@linkplain de.sayayi.lib.message.formatter.post.PostFormatter post formatters}.
 *
 * <h2>Services</h2>
 * <p>The module uses the following service interfaces:
 * <ul>
 *   <li>{@link de.sayayi.lib.message.formatter.parameter.ParameterFormatter ParameterFormatter} and
 *       {@link de.sayayi.lib.message.formatter.post.PostFormatter PostFormatter}: discovered by the
 *       {@link de.sayayi.lib.message.formatter.DefaultFormatterService DefaultFormatterService}</li>
 *   <li>{@link de.sayayi.lib.message.template.NamedTemplate NamedTemplate}: discovered by
 *       {@link de.sayayi.lib.message.MessageSupport.ConfigurableMessageSupport#registerTemplatesFromService
 *       registerTemplatesFromService}</li>
 * </ul>
 * <p>It provides its built-in named and runtime parameter formatters and its post formatters as implementations of
 * these services, as well as a {@link java.nio.file.spi.FileTypeDetector FileTypeDetector} that recognizes message
 * format pack files.
 *
 * <h2>Message sources</h2>
 * <p>Messages can be loaded from properties files or resource bundles using the
 * {@linkplain de.sayayi.lib.message.adopter adopter} classes, or packed into a compact binary format for efficient
 * storage and distribution.
 *
 * @see de.sayayi.lib.message.MessageSupport
 * @see de.sayayi.lib.message.MessageFactory
 *
 * @since 0.12.0
 */
@SuppressWarnings({ "JavaRequiresAutoModule", "requires-transitive-automatic" })
module de.sayayi.lib.message
{
  requires transitive de.sayayi.lib.antlr;
  requires transitive de.sayayi.lib.pack;
  requires transitive org.antlr.antlr4.runtime;

  requires java.sql;

  requires static java.xml;
  requires static lombok;
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
  exports de.sayayi.lib.message.pack;
  exports de.sayayi.lib.message.part;
  exports de.sayayi.lib.message.part.normalizer;
  exports de.sayayi.lib.message.template;
  exports de.sayayi.lib.message.util;

  // service provider interfaces
  uses de.sayayi.lib.message.formatter.parameter.ParameterFormatter;
  uses de.sayayi.lib.message.formatter.post.PostFormatter;
  uses de.sayayi.lib.message.template.NamedTemplate;

  // internal service implementations
  provides java.nio.file.spi.FileTypeDetector with
      de.sayayi.lib.message.internal.pack.PackFileTypeDetector;
  provides de.sayayi.lib.message.formatter.parameter.ParameterFormatter with
      de.sayayi.lib.message.formatter.parameter.named.BoolFormatter,
      de.sayayi.lib.message.formatter.parameter.named.ChoiceFormatter,
      de.sayayi.lib.message.formatter.parameter.named.SizeFormatter,
      de.sayayi.lib.message.formatter.parameter.named.StringFormatter,
      de.sayayi.lib.message.formatter.parameter.runtime.ArrayFormatter,
      de.sayayi.lib.message.formatter.parameter.runtime.AtomicBooleanFormatter,
      de.sayayi.lib.message.formatter.parameter.runtime.BitSetFormatter,
      de.sayayi.lib.message.formatter.parameter.runtime.BooleanSupplierFormatter,
      de.sayayi.lib.message.formatter.parameter.runtime.ByteArrayFormatter,
      de.sayayi.lib.message.formatter.parameter.runtime.CharsetFormatter,
      de.sayayi.lib.message.formatter.parameter.runtime.DictionaryFormatter,
      de.sayayi.lib.message.formatter.parameter.runtime.DoubleSupplierFormatter,
      de.sayayi.lib.message.formatter.parameter.runtime.EnumFormatter,
      de.sayayi.lib.message.formatter.parameter.runtime.IntSupplierFormatter,
      de.sayayi.lib.message.formatter.parameter.runtime.IterableFormatter,
      de.sayayi.lib.message.formatter.parameter.runtime.ToTemporalDelegate,
      de.sayayi.lib.message.formatter.parameter.runtime.LocaleFormatter,
      de.sayayi.lib.message.formatter.parameter.runtime.LongSupplierFormatter,
      de.sayayi.lib.message.formatter.parameter.runtime.MapEntryFormatter,
      de.sayayi.lib.message.formatter.parameter.runtime.MapFormatter,
      de.sayayi.lib.message.formatter.parameter.runtime.NumberFormatter,
      de.sayayi.lib.message.formatter.parameter.runtime.OptionalDoubleFormatter,
      de.sayayi.lib.message.formatter.parameter.runtime.OptionalFormatter,
      de.sayayi.lib.message.formatter.parameter.runtime.OptionalIntFormatter,
      de.sayayi.lib.message.formatter.parameter.runtime.OptionalLongFormatter,
      de.sayayi.lib.message.formatter.parameter.runtime.PathFormatter,
      de.sayayi.lib.message.formatter.parameter.runtime.ReferenceFormatter,
      de.sayayi.lib.message.formatter.parameter.runtime.SupplierFormatter,
      de.sayayi.lib.message.formatter.parameter.runtime.TemporalFormatter,
      de.sayayi.lib.message.formatter.parameter.runtime.ThrowableFormatter,
      de.sayayi.lib.message.formatter.parameter.runtime.TimeZoneFormatter,
      de.sayayi.lib.message.formatter.parameter.runtime.TypeFormatter,
      de.sayayi.lib.message.formatter.parameter.runtime.URIFormatter,
      de.sayayi.lib.message.formatter.parameter.runtime.URLFormatter;
  provides de.sayayi.lib.message.formatter.post.PostFormatter with
      de.sayayi.lib.message.formatter.post.runtime.CasePostFormatter,
      de.sayayi.lib.message.formatter.post.runtime.ClipPostFormatter;
}
