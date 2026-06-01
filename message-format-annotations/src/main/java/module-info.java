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
 * Provides annotations for defining messages and templates directly in Java source code, and an adopter framework
 * for discovering and publishing them from compiled class files.
 * <p>
 * <b>Annotations</b> ({@code de.sayayi.lib.message.annotation}):
 * <ul>
 *   <li>{@link de.sayayi.lib.message.annotation.MessageDef MessageDef} – defines a message with a unique code</li>
 *   <li>{@link de.sayayi.lib.message.annotation.TemplateDef TemplateDef} – defines a reusable template with a
 *       unique name</li>
 *   <li>{@link de.sayayi.lib.message.annotation.Text Text} – provides localized text variants for messages and
 *       templates</li>
 * </ul>
 * <p>
 * <b>Adopter</b> ({@code de.sayayi.lib.message.annotation.adopter}):
 * <br>
 * The {@link de.sayayi.lib.message.annotation.adopter.AbstractAnnotationAdopter AbstractAnnotationAdopter} provides
 * multiple strategies for locating annotated classes, including classpath scanning, single class file analysis, and
 * direct annotation instance processing. The
 * {@link de.sayayi.lib.message.annotation.adopter.AnnotationAdopter AnnotationAdopter} is the concrete
 * implementation that scans compiled class files for message and template annotations without loading the classes
 * into the JVM, and publishes them to a
 * {@link de.sayayi.lib.message.MessageSupport.MessagePublisher MessagePublisher}.
 * <p>
 * <b>Synthetic annotations</b> ({@code de.sayayi.lib.message.annotation.adopter.util}):
 * <br>
 * Synthetic implementations of the annotation interfaces allow messages and templates to be constructed and
 * registered programmatically without requiring annotated class files, which is useful in programmatic and testing
 * scenarios.
 */
module de.sayayi.lib.message.annotations
{
  requires transitive de.sayayi.lib.message;

  requires static org.jetbrains.annotations;

  requires static org.objectweb.asm;

  exports de.sayayi.lib.message.annotation;
  exports de.sayayi.lib.message.annotation.adopter;
  exports de.sayayi.lib.message.annotation.adopter.util;
}
