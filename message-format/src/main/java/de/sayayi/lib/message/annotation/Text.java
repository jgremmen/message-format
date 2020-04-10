/*
 * Copyright 2019 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sayayi.lib.message.annotation;

import de.sayayi.lib.message.Message.Parameters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Text
{
  /**
   * <p>
   *   Message locale, either the language code (de, es) or the language with country (de_DE, fr_CA).
   * </p>
   * <p>
   *   The default value corresponds to {@link java.util.Locale#ROOT} which will match any locale used for formatting
   *   this message.
   * </p>
   *
   * @return  message locale
   *
   * @see de.sayayi.lib.message.Message#format(Parameters)
   */
  String locale() default "";


  /**
   * Localized message text.
   *
   * @return  message text
   */
  String text() default "";


  /**
   * <p>
   *   Not localized message text.
   * </p>
   * <p>
   *   This value is used only if {@code locale} and {@code text} are not set. Otherwise its value is ignored.
   * </p>
   *
   * @return  message text
   */
  String value() default "";
}
