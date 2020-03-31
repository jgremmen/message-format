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
import de.sayayi.lib.message.data.Data;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;


/**
 * @author Jeroen Gremmen
 */
public final class ClassFormatter extends AbstractParameterFormatter
{
  @Override
  public String formatValue(Object value, String format, @NotNull Parameters parameters, Data data)
  {
    String s = null;

    if (value != null)
    {
      final Class<?> clazz = (Class<?>)value;

      if ("name".equals(format))
        s = clazz.getSimpleName();
      else if ("package".equals(format))
        return parameters.getFormatter(Package.class).format(clazz.getPackage(), null, parameters, data);
      else
        s = clazz.getName();
    }

    return formatString(s, parameters, data);
  }


  @NotNull
  @Override
  @Contract(value = "-> new", pure = true)
  public Set<Class<?>> getFormattableTypes() {
    return Collections.<Class<?>>singleton(Class.class);
  }
}
