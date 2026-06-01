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
 * The {@link de.sayayi.lib.message.annotation.adopter.AnnotationAdopter AnnotationAdopter} interface scans compiled
 * class files for message and template annotations and publishes them to a
 * {@link de.sayayi.lib.message.MessageSupport.MessagePublisher MessagePublisher}. Use
 * {@link de.sayayi.lib.message.annotation.adopter.AnnotationAdopter#getAutoDetected(
 * de.sayayi.lib.message.MessageSupport.ConfigurableMessageSupport) AnnotationAdopter.getAutoDetected(...)} to obtain
 * an implementation that is automatically selected based on the bytecode analysis library available on the classpath.
 * <p>
 * <b>Library-specific implementations</b> ({@code de.sayayi.lib.message.annotation.adopter.lib}):
 * <br>
 * Concrete adopter implementations are provided for standalone
 * <a href="https://asm.ow2.io/">ASM</a>, <a href="https://bytebuddy.net/">Byte Buddy</a>'s bundled ASM,
 * and <a href="https://spring.io/projects/spring-framework">Spring Framework</a>'s bundled ASM. Only one of these
 * libraries needs to be present on the classpath.
 */
module de.sayayi.lib.message.annotations
{
  requires transitive de.sayayi.lib.message;

  requires static org.jetbrains.annotations;

  requires static org.objectweb.asm;
  requires static net.bytebuddy;
  requires static spring.core;

  exports de.sayayi.lib.message.annotation;
  exports de.sayayi.lib.message.annotation.adopter;
  exports de.sayayi.lib.message.annotation.adopter.lib;
  exports de.sayayi.lib.message.annotation.adopter.util;

  uses de.sayayi.lib.message.annotation.adopter.AnnotationAdopterProvider;
}
