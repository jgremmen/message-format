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
package de.sayayi.lib.message.annotation.adopter.util;

import de.sayayi.lib.message.annotation.Text;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;


/**
 * Synthetic implementation of the {@link Text} annotation interface, allowing {@code Text} instances to be constructed
 * programmatically for use as localized text variants in {@link SyntheticMessageDef} or {@link SyntheticTemplateDef}.
 * <p>
 * All string fields are trimmed on construction. {@code null} values default to empty strings.
 *
 * @param locale  locale tag (e.g. {@code "en"} or {@code "de"}), not {@code null}
 * @param text    localized message or template text, not {@code null}
 * @param value   shorthand text value (used when both {@code locale} and {@code text} are empty), not {@code null}
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public record SyntheticText(@NotNull String locale, @Language("MessageFormat") @NotNull String text,
                            @Language("MessageFormat") @NotNull String value) implements Text
{
  /**
   * Creates a new {@code SyntheticText} instance. All {@code null} string parameters are replaced with empty strings
   * and all values are trimmed.
   *
   * @param locale  locale tag (e.g. {@code "en"} or {@code "de"}), or {@code null} to default to an empty string
   * @param text    localized message or template text, or {@code null} to default to an empty string
   * @param value   shorthand text value, or {@code null} to default to an empty string
   */
  public SyntheticText(String locale, String text, String value)
  {
    this.locale = locale == null ? "" : locale.trim();
    this.text = text == null ? "" : text.trim();
    this.value = value == null ? "" : value.trim();
  }


  /** {@inheritDoc} */
  @Override
  public Class<? extends Annotation> annotationType() {
    return Text.class;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull String toString() {
    return "Text(locale=" + locale + ",text=" + text + ",value=" + value + ')';
  }
}
