/**
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
package de.sayayi.lib.message.formatter;


/**
 * @author Jeroen Gremmen
 */
public interface FormatterService
{
  /**
   * <p>
   *   Returns a data formatter for the given {@code format} and {@code type}.
   * </p>
   * <p>
   *   Implementing classes must make sure that for any combination of {@code format} and {@code type} this function
   *   always returns a formatter. A good choice for a default formatter would be
   *   {@link de.sayayi.lib.message.formatter.support.StringFormatter} associated with {@link Object}.
   * </p>
   *
   * @param format  name of the formatter or {@code null}
   * @param type    type of the value to format
   *
   * @return  data formatter, never {@code null}
   *
   * @see GenericFormatterRegistry
   */
  ParameterFormatter getFormatter(String format, Class<?> type);
}
