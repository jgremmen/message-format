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
 * Annotations for defining messages and templates directly in Java source code.
 * <p>
 * Annotated classes can be scanned and published using the
 * {@link de.sayayi.lib.message.annotation.adopter.AnnotationAdopter AnnotationAdopter} from the
 * {@code de.sayayi.lib.message.annotation.adopter} package.
 * <p>
 * The annotations have class retention, so they are available in class files but not at runtime:
 * <ul>
 *   <li>{@link de.sayayi.lib.message.annotation.MessageDef MessageDef}: defines a message with a unique code</li>
 *   <li>{@link de.sayayi.lib.message.annotation.TemplateDef TemplateDef}: defines a template with a unique name</li>
 *   <li>{@link de.sayayi.lib.message.annotation.Text Text}: provides a localized text for a message or template</li>
 *   <li>{@link de.sayayi.lib.message.annotation.MessageDefs MessageDefs} and
 *       {@link de.sayayi.lib.message.annotation.TemplateDefs TemplateDefs}: containers for repeated definitions</li>
 * </ul>
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
package de.sayayi.lib.message.annotation;
