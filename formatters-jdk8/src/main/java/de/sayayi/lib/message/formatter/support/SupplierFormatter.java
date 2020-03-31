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
import java.util.function.Supplier;


/**
 * @author Jeroen Gremmen
 */
public final class SupplierFormatter extends AbstractParameterFormatter
{
  @Override
  @Contract(pure = true)
  public String formatValue(Object value, String format, @NotNull Parameters parameters, Data data)
  {
    final Supplier<?> supplier = (Supplier<?>)value;
    if (supplier == null)
      return formatNull(parameters, data);

    value = supplier.get();
    if (value == null)
      return formatNull(parameters, data);

    return parameters.getFormatter(format, value.getClass()).format(value, format, parameters, data);
  }


  @NotNull
  @Override
  @Contract(value = "-> new", pure = true)
  public Set<Class<?>> getFormattableTypes() {
    return Collections.singleton(Supplier.class);
  }
}
