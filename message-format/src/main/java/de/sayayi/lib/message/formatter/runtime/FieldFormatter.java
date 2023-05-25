/*
 * Copyright 2021 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.formatter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.part.TextPartFactory.nullText;
import static java.util.Collections.singleton;


/**
 * @author Jeroen Gremmen
 */
public final class FieldFormatter extends AbstractParameterFormatter
{
  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull FormatterContext context, Object value)
  {
    if (value == null)
      return nullText();

    final Field field = (Field)value;
    final StringBuilder formattedField = new StringBuilder();
    final String format = context.getConfigValueString("field").orElse("juMTN");

    // c = short class
    // j = no java.lang. prefix
    // u = no java.util. prefix
    // M = with modifiers
    // T = field type
    // N = field name

    if (format.indexOf('M') >= 0)
      formattedField.append(Modifier.toString(field.getModifiers())).append(' ');

    if (format.indexOf('T') >= 0)
      formattedField.append(TypeFormatter.toString(field.getGenericType(), format)).append(' ');

    if (format.indexOf('N') >= 0)
      formattedField.append(field.getName());

    return noSpaceText(formattedField.toString());
  }


  @Override
  @Contract(value = "-> new", pure = true)
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return singleton(new FormattableType(Field.class));
  }
}
