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
package de.sayayi.lib.message.annotation;

import org.intellij.lang.annotations.Language;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.CLASS;


/**
 * Defines a template message with a unique name.
 * <p>
 * For example:
 * <blockquote><pre>
 * &#x40;TemplateDef(name = "ex-msg", text = "%{ex,!empty:': %{ex}'}")
 * </pre></blockquote>
 *
 * @see AbstractAnnotationAdopter
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@Target({ ANNOTATION_TYPE, METHOD, TYPE })
@Retention(CLASS)
@Repeatable(TemplateDefs.class)
@SuppressWarnings("UnknownLanguage")
public @interface TemplateDef
{
  /**
   * Unique template name.
   *
   * @return  unique template name, not empty
   */
  String name();


  /**
   * 1..n localized texts.
   *
   * @return  localized texts
   */
  Text[] texts() default {};


  /**
   * Template text. Short for {@code texts = @Text("...")}
   *
   * @return  template text
   */
  @Language("MessageFormat")
  String text() default "";
}
