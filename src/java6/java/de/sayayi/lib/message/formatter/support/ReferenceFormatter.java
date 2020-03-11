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
package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.data.ParameterData;
import de.sayayi.lib.message.formatter.ParameterFormatter;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.util.Collections;
import java.util.Set;


/**
 * @author Jeroen Gremmen
 */
public final class ReferenceFormatter implements ParameterFormatter
{
  @SuppressWarnings("rawtypes")
  @Override
  public String format(Object value, String format, @NotNull Parameters parameters, ParameterData data)
  {
    if (value != null)
    {
      Reference reference = (Reference)value;
      value = reference.get();

      if (value != null)
        return parameters.getFormatter(format, value.getClass()).format(value, format, parameters, data);
    }

    return null;
  }


  @NotNull
  @Override
  public Set<Class<?>> getFormattableTypes() {
    return Collections.<Class<?>>singleton(Reference.class);
  }
}