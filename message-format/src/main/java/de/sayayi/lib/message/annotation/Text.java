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
package de.sayayi.lib.message.annotation;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import org.intellij.lang.annotations.Language;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;


/**
 * Message text annotation.
 *
 * @author Jeroen Gremmen
 *
 * @see MessageDef
 * @see TemplateDef
 */
@Target(ANNOTATION_TYPE)
@Retention(CLASS)
@SuppressWarnings("UnknownLanguage")
public @interface Text
{
  /**
   * Message locale, either the language code (eg. de, es) or the language with country
   * (eg. de_DE, fr_CA).
   * <p>
   * The default value corresponds to {@link java.util.Locale#ROOT} which will match any locale
   * used for formatting this message.
   *
   * @return  message locale
   *
   * @see de.sayayi.lib.message.Message#format(MessageAccessor, Parameters)
   */
  String locale() default "";


  /**
   * Localized message text.
   *
   * @return  message text
   */
  @Language("MessageFormat")
  String text() default "";


  /**
   * Not localized message text.
   * <p>
   * This value is used only if {@code locale} and {@code text} are not set. Otherwise its value
   * is ignored.
   *
   * @return  message text
   */
  @Language("MessageFormat")
  String value() default "";
}
