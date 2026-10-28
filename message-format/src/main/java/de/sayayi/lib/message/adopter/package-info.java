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
 * Adopters for reading messages and templates from external sources such as
 * {@link java.util.ResourceBundle ResourceBundle} and {@link java.util.Properties Properties} and publishing them to
 * a {@link de.sayayi.lib.message.MessageSupport.MessagePublisher MessagePublisher}.
 * <p>
 * The types in this package are:
 * <ul>
 *   <li>{@link de.sayayi.lib.message.adopter.AbstractMessageAdopter AbstractMessageAdopter}: base class holding the
 *       message factory and message publisher shared by all adopters</li>
 *   <li>{@link de.sayayi.lib.message.adopter.PropertiesAdopter PropertiesAdopter}: adopts messages and templates
 *       from {@code Properties}</li>
 *   <li>{@link de.sayayi.lib.message.adopter.ResourceBundleAdopter ResourceBundleAdopter}: adopts messages and
 *       templates from resource bundles, combining localized values into locale aware messages</li>
 * </ul>
 * <p>
 * The annotation based adopter is provided by the separate {@code de.sayayi.lib.message.annotations} module.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
package de.sayayi.lib.message.adopter;
