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

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


/**
 * Defines a message with a unique code.
 * <p>
 * For example:
 * <blockquote><pre>
 * &#x40;MessageDef(code = "MSG-001", texts = {
 *   &#x40;Text(locale = "en", text = "%{r,size,1:'1 result',:'%{r,size} results'} found"),
 *   &#x40;Text(locale = "de", text = "%{r,size,1:'1 Ergebnis',:'%{r,size} Ergebnisse'} gefunden")
 * })
 * </pre></blockquote>
 *
 *
 * @author Jeroen Gremmen
 *
 * @see de.sayayi.lib.message.adopter.AnnotationAdopter AnnotationAdopter
 * @see de.sayayi.lib.message.adopter.AsmClassPathScannerAdopter AsmClassPathScannerAdopter
 * @see de.sayayi.lib.message.adopter.SpringClassPathScannerAdopter SpringClassPathScannerAdopter
 */
@Target({ ANNOTATION_TYPE, METHOD, TYPE })
@Retention(RUNTIME)
@Repeatable(MessageDefs.class)
public @interface MessageDef
{
  /**
   * <p>
   *   Unique message code.
   * </p>
   * <p>
   *   Uniqueness is required to identify and retrieve a message by its {@code code} and is defined within
   *   the scope of a {@link de.sayayi.lib.message.MessageSupport MessageSupport} instance.
   * </p>
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
  String text() default "";
}
