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

import org.intellij.lang.annotations.Language;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.CLASS;


/**
 * Defines a message with a unique code.
 * <p>
 * For example:
 * <blockquote><pre>
 * &#x40;MessageDef(code = "MSG-001", texts = {
 *   &#x40;Text(locale = "en", text = "%{r,size,1:'1 result',:'%{r,size,0:no} results'} found"),
 *   &#x40;Text(locale = "de", text = "%{r,size,1:'1 Ergebnis',:'%{r,size,0:keine} Ergebnisse'} gefunden")
 * })
 * </pre></blockquote>
 *
 * @see AbstractAnnotationAdopter
 *
 * @author Jeroen Gremmen
 * @since 0.1.0 (renamed in 0.3.0)
 */
@Target({ ANNOTATION_TYPE, METHOD, TYPE })
@Retention(CLASS)
@Repeatable(MessageDefs.class)
@SuppressWarnings({"UnknownLanguage", "GrazieInspection"})
public @interface MessageDef
{
  /**
   * Unique message code.
   * <p>
   * Uniqueness is required to identify and retrieve a message by its {@code code} and is defined
   * within the scope of a {@link de.sayayi.lib.message.MessageSupport MessageSupport} instance.
   *
   * @return  unique message code, not empty
   */
  String code();


  /**
   * 1..n localized texts.
   *
   * @return  localized texts
   */
  Text[] texts() default {};


  /**
   * Message text. Short for {@code texts = @Text("...")}
   *
   * @return  message text
   */
  @Language("MessageFormat")
  String text() default "";
}
