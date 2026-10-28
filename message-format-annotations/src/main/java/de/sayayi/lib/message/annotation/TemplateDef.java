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

import de.sayayi.lib.message.annotation.adopter.AnnotationAdopter;
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
 * @see AnnotationAdopter
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@Target({ ANNOTATION_TYPE, METHOD, CONSTRUCTOR, TYPE })
@Retention(CLASS)
@Repeatable(TemplateDefs.class)
public @interface TemplateDef
{
  /**
   * Unique template name. The name must follow the kebab-case naming convention.
   *
   * @return  unique template name, not empty
   */
  String name();


  /**
   * Localized template texts. Use this attribute to provide template texts for one or more locales.
   * <p>
   * Either this attribute or {@link #text()} must be specified, but not both.
   *
   * @return  localized texts
   */
  Text[] texts() default {};


  /**
   * Convenience attribute for specifying a single locale-independent template text.
   * Short for {@code texts = @Text("...")}.
   * <p>
   * Either this attribute or {@link #texts()} must be specified, but not both.
   *
   * @return  template text
   */
  @Language("MessageFormat")
  String text() default "";
}
