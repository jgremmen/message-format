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
 * Template API for the message format library. This package provides the interfaces and classes for defining
 * reusable templates that can be registered by name and referenced from messages.
 * <p>
 * The main types in this package are:
 * <ul>
 *   <li>{@link de.sayayi.lib.message.template.Template Template} – represents a reusable template that can be
 *       registered by name in a
 *       {@link de.sayayi.lib.message.MessageSupport.ConfigurableMessageSupport ConfigurableMessageSupport} and
 *       referenced from messages</li>
 *   <li>{@link de.sayayi.lib.message.template.NamedTemplate NamedTemplate} – a template that
 *       carries a name, enabling automatic registration via the {@link java.util.ServiceLoader} mechanism</li>
 *   <li>{@link de.sayayi.lib.message.template.AbstractNamedTemplate AbstractNamedTemplate} – base class for custom
 *       template implementations; extends {@code NamedTemplate} and provides a default
 *       {@link de.sayayi.lib.message.template.Template#isSame(de.sayayi.lib.message.template.Template) isSame}
 *       implementation</li>
 * </ul>
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 */
package de.sayayi.lib.message.template;
