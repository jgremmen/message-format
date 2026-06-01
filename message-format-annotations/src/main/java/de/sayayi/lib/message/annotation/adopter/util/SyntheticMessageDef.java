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

import de.sayayi.lib.message.annotation.MessageDef;
import de.sayayi.lib.message.annotation.Text;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.util.Arrays;


/**
 * Synthetic implementation of the {@link MessageDef} annotation interface, allowing {@code MessageDef} instances to be
 * constructed programmatically. This is useful for registering messages directly via
 * {@link de.sayayi.lib.message.annotation.adopter.AnnotationAdopter#adopt(MessageDef) adopt(MessageDef)} without
 * requiring an annotated class.
 * <p>
 * The {@code code} is trimmed on construction. A {@code null} text defaults to an empty string.
 *
 * @param code   unique message code, not {@code null}
 * @param text   single message text (used when {@code texts} is empty), not {@code null}
 * @param texts  localized text variants, not {@code null}
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public record SyntheticMessageDef(@NotNull String code, @Language("MessageFormat") @NotNull String text,
                                  @NotNull Text[] texts) implements MessageDef
{
  /**
   * Convenience constructor for a non-localized message with a single text and no {@link Text} variants.
   *
   * @param code  unique message code, not {@code null}
   * @param text  message text, or {@code null} to default to an empty string
   */
  public SyntheticMessageDef(@NotNull String code, String text) {
    this(code, text, new Text[0]);
  }


  /**
   * Creates a new {@code SyntheticMessageDef} instance. The {@code code} is trimmed and a {@code null} text is
   * replaced with an empty string.
   *
   * @param code   unique message code, not {@code null}
   * @param text   single message text, or {@code null} to default to an empty string
   * @param texts  localized text variants, not {@code null}
   */
  public SyntheticMessageDef(@NotNull String code, String text, @NotNull Text[] texts)
  {
    this.code = code.trim();
    this.text = text == null ? "" : text.trim();
    this.texts = texts;
  }


  /** {@inheritDoc} */
  @Override
  public Class<? extends Annotation> annotationType() {
    return MessageDef.class;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull String toString() {
    return "MessageDef(code=" + code + ",text=" + text + ",texts=" + Arrays.toString(texts) + ')';
  }
}
