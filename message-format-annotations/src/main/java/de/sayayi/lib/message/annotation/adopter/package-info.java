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
 * Adopter for discovering and publishing messages and templates defined by
 * {@link de.sayayi.lib.message.annotation.MessageDef MessageDef} and
 * {@link de.sayayi.lib.message.annotation.TemplateDef TemplateDef} annotations in compiled class files.
 * <p>
 * The {@link de.sayayi.lib.message.annotation.adopter.AbstractAnnotationAdopter AbstractAnnotationAdopter}
 * provides multiple strategies for locating annotated classes, including classpath scanning, single class file
 * analysis, and direct annotation instance processing.
 * <p>
 * The {@link de.sayayi.lib.message.annotation.adopter.AnnotationAdopter AnnotationAdopter} is the concrete
 * ASM-based implementation that scans compiled class files for message and template annotations without loading
 * the classes into the JVM.
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 */
package de.sayayi.lib.message.annotation.adopter;
