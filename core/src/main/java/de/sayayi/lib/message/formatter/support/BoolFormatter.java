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

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.data.Data;
import de.sayayi.lib.message.data.DataMap;
import de.sayayi.lib.message.formatter.NamedParameterFormatter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.util.ResourceBundle.getBundle;


/**
 * @author Jeroen Gremmen
 */
public final class BoolFormatter extends AbstractParameterFormatter implements NamedParameterFormatter
{
  @NotNull
  @Override
  @Contract(pure = true)
  public String getName() {
    return "bool";
  }


  @Override
  @Contract(pure = true)
  public String format(Object value, String format, @NotNull Parameters parameters, Data data)
  {
    boolean bool;

    if (value == null)
      return formatNull(parameters, data);

    if (value instanceof Boolean)
      bool = (Boolean)value;
    else if (value instanceof BigInteger)
      bool = ((java.math.BigInteger)value).signum() != 0;
    else if (value instanceof BigDecimal)
      bool = ((java.math.BigDecimal)value).signum() != 0;
    else if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long)
      bool = ((Number)value).longValue() != 0;
    else if (value instanceof Number)
      bool = Math.signum(((Number)value).doubleValue()) != 0;
    else
      bool = Boolean.parseBoolean(String.valueOf(value));

    // allow custom messages for true/false value?
    if (data instanceof DataMap)
    {
      final Message message = ((DataMap)data).getMessage(bool, null, false);
      if (message != null)
        return message.format(parameters);
    }

    return getBundle(getClass().getPackage().getName() + ".Formatter",
        parameters.getLocale()).getString(Boolean.toString(bool));
  }


  @NotNull
  @Override
  @Contract(value = "-> new", pure = true)
  public Set<Class<?>> getFormattableTypes() {
    return new HashSet<Class<?>>(Arrays.<Class<?>>asList(Boolean.class, boolean.class));
  }
}
