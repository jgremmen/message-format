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
package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.data.ParameterData;
import de.sayayi.lib.message.data.ParameterMap;
import de.sayayi.lib.message.formatter.NamedParameterFormatter;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;


/**
 * @author Jeroen Gremmen
 */
public final class ChoiceFormatter implements NamedParameterFormatter
{
  @Override
  public String getName() {
    return "choice";
  }


  @Override
  public String format(Object value, String format, Parameters parameters, ParameterData data)
  {
    if (!(data instanceof ParameterMap))
      throw new IllegalArgumentException("data must be a choice map");

    if (value != null && !(value instanceof Serializable))
      throw new IllegalArgumentException("value must be serializable");

    return data.format(parameters, (Serializable)value);
  }


  @Override
  public Set<Class<?>> getFormattableTypes() {
    return Collections.emptySet();
  }
}