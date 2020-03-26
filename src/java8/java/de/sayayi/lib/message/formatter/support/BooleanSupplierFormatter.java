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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;
import java.util.function.BooleanSupplier;


/**
 * @author Jeroen Gremmen
 */
public final class BooleanSupplierFormatter extends AbstractParameterFormatter
{
  @Override
  @Contract(pure = true)
  public String format(Object value, String format, @NotNull Parameters parameters, ParameterData data)
  {
    BooleanSupplier supplier = (BooleanSupplier)value;
    if (supplier == null)
      return formatNull(parameters, data);

    return parameters.getFormatter(format, boolean.class).format(supplier.getAsBoolean(), format, parameters, data);
  }


  @NotNull
  @Override
  @Contract(value = "-> new", pure = true)
  public Set<Class<?>> getFormattableTypes() {
    return Collections.singleton(BooleanSupplier.class);
  }
}
