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
 *   <li>
 *     {@link de.sayayi.lib.message.spring.MessageSupportMessageSource MessageSupportMessageSource}: a Spring
 *     {@link org.springframework.context.MessageSource MessageSource} implementation backed by
 *     {@link de.sayayi.lib.message.MessageSupport MessageSupport}, allowing the message format library to serve as a
 *     drop-in replacement for Spring's message resolution
 *   </li>
 *   <li>
 *     {@link de.sayayi.lib.message.spring.formatter.SpELFormatter SpELFormatter}: a parameter formatter that evaluates
 *     Spring Expression Language (SpEL) expressions on parameter values
 *   </li>
 *   <li>
 *     parameter formatters for the Spring types {@link org.springframework.util.unit.DataSize DataSize},
 *     {@link org.springframework.core.ResolvableType ResolvableType} and
 *     {@link org.springframework.core.convert.TypeDescriptor TypeDescriptor}
 *   </li>
 * </ul>
 * <p>
 * The message source is located in package {@code de.sayayi.lib.message.spring} and the parameter formatters in
 * package {@code de.sayayi.lib.message.spring.formatter}.
 * <p>
 * The module provides the formatters for Spring types as implementations of the
 * {@link de.sayayi.lib.message.formatter.parameter.ParameterFormatter ParameterFormatter} service, so they are
 * registered automatically by the {@link de.sayayi.lib.message.formatter.DefaultFormatterService
 * DefaultFormatterService}. The SpEL formatter is not registered automatically; it must be added to the formatter
 * service explicitly.
 *
 * @since 0.12.0
 */
@SuppressWarnings({ "JavaRequiresAutoModule", "requires-transitive-automatic" })
module de.sayayi.lib.message.spring
{
  requires transitive de.sayayi.lib.message;
  requires transitive spring.core;
  requires transitive spring.context;

  requires spring.expression;

  requires static org.jetbrains.annotations;

  exports de.sayayi.lib.message.spring;
  exports de.sayayi.lib.message.spring.formatter;

  provides de.sayayi.lib.message.formatter.parameter.ParameterFormatter with
      de.sayayi.lib.message.spring.formatter.DataSizeFormatter,
      de.sayayi.lib.message.spring.formatter.ResolvableTypeFormatter,
      de.sayayi.lib.message.spring.formatter.TypeDescriptorFormatter;
}
