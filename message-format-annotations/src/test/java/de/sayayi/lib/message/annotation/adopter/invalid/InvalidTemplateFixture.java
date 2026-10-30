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
package de.sayayi.lib.message.annotation.adopter.invalid;

import de.sayayi.lib.message.annotation.TemplateDef;


/**
 * Fixture class with an invalid template, used by {@code AnnotationAdopterTest}. It lives in its own package, so
 * scanning this package finds no other fixture that fails differently.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@TemplateDef(name = "invalid-template", text = "%{x,true false:1}")
public final class InvalidTemplateFixture {
}
