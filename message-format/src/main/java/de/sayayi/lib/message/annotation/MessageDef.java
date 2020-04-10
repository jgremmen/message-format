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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MessageDef
{
  /**
   * <p>
   *   Unique message code.
   * </p>
   * <p>
   *   Uniqueness is required to identify and retrieve a message by its {@code code} and is defined within the scope
   *   of a message bundle.
   * </p>
   *
   * @return  unique message code, not empty
   *
   * @see de.sayayi.lib.message.MessageBundle
   */
  String code();


  /**
   * 1..n localized texts.
   *
   * @return  localized texts
   */
  Text[] texts();
}
