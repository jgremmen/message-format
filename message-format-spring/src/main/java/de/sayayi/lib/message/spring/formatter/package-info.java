/*
 * Copyright 2023 Jeroen Gremmen
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
 * Spring-specific parameter formatters, providing integration with the Spring Expression Language (SpEL) and Spring
 * utility types.
 * <ul>
 *   <li>
 *     {@link de.sayayi.lib.message.spring.formatter.SpELFormatter SpELFormatter} ({@code spel}) evaluates a SpEL
 *     expression on a parameter value and formats the result.
 *   </li>
 *   <li>
 *     {@link de.sayayi.lib.message.spring.formatter.DataSizeFormatter DataSizeFormatter} formats
 *     {@link org.springframework.util.unit.DataSize DataSize} values as file sizes.
 *   </li>
 *   <li>
 *     {@link de.sayayi.lib.message.spring.formatter.TypeDescriptorFormatter TypeDescriptorFormatter} formats
 *     {@link org.springframework.core.convert.TypeDescriptor TypeDescriptor} values as the type they describe.
 *   </li>
 *   <li>
 *     {@link de.sayayi.lib.message.spring.formatter.ResolvableTypeFormatter ResolvableTypeFormatter} formats
 *     {@link org.springframework.core.ResolvableType ResolvableType} values as their underlying type.
 *   </li>
 * </ul>
 * <p>
 * The formatters for Spring types are provided as services and are registered automatically by the
 * {@link de.sayayi.lib.message.formatter.DefaultFormatterService DefaultFormatterService}. The SpEL formatter is not
 * registered automatically and must be added to a formatter service explicitly.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
package de.sayayi.lib.message.spring.formatter;
