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
 * Synthetic annotation implementations for programmatic construction of
 * {@link de.sayayi.lib.message.annotation.MessageDef MessageDef},
 * {@link de.sayayi.lib.message.annotation.TemplateDef TemplateDef} and
 * {@link de.sayayi.lib.message.annotation.Text Text} instances.
 * <p>
 * These records allow messages and templates to be registered via
 * {@link de.sayayi.lib.message.annotation.adopter.AbstractAnnotationAdopter#adopt(de.sayayi.lib.message.annotation.MessageDef)
 * adopt(MessageDef)} and
 * {@link de.sayayi.lib.message.annotation.adopter.AbstractAnnotationAdopter#adopt(de.sayayi.lib.message.annotation.TemplateDef)
 * adopt(TemplateDef)} without requiring annotated class files, which is useful in programmatic and testing scenarios.
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 */
package de.sayayi.lib.message.annotation.adopter.util;
