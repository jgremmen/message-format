/*
 * Copyright 2021 Jeroen Gremmen
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

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.data.Data;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;

import static de.sayayi.lib.message.internal.part.MessagePartFactory.noSpaceText;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.nullText;
import static java.util.Collections.singleton;


/**
 * @author Jeroen Gremmen
 */
public final class FieldFormatter extends AbstractParameterFormatter
{
  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull MessageContext messageContext, Object value, String format,
                                   @NotNull Parameters parameters, Data data)
  {
    if (value == null)
      return nullText();

    final Field field = (Field)value;
    final StringBuilder formattedField = new StringBuilder();
    final String fieldFormat =
        getConfigValueString(messageContext, "field", parameters, data, true, "juM");

    if ("type".equals(format))
      return noSpaceText(TypeFormatter.toString(field.getGenericType(), fieldFormat));

    if (!"name".equals(format))
    {
      // c = short class
      // j = no java.lang. prefix
      // u = no java.util. prefix
      // M = with modifiers

      if (fieldFormat.indexOf('M') >= 0)
        formattedField.append(Modifier.toString(field.getModifiers())).append(' ');

      formattedField.append(TypeFormatter.toString(field.getGenericType(), fieldFormat)).append(' ');
    }

    formattedField.append(field.getName());

    return noSpaceText(formattedField.toString());
  }


  @Override
  @Contract(value = "-> new", pure = true)
  public @NotNull Set<Class<?>> getFormattableTypes() {
    return singleton(Field.class);
  }
}