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
package de.sayayi.lib.message.formatter.runtime.extra;

import de.sayayi.lib.message.formatter.AbstractSingleTypeParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.ParameterFormatter.ConfigKeyComparator;
import de.sayayi.lib.message.formatter.runtime.TypeFormatter;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.Defined.EQUIVALENT;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.Defined.MISMATCH;


/**
 * @author Jeroen Gremmen
 */
public final class FieldFormatter
    extends AbstractSingleTypeParameterFormatter<Field>
    implements ConfigKeyComparator<Field>
{
  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull Field field)
  {
    var formattedField = new StringBuilder();
    var format = context.getConfigValueString("field").orElse("juMTN");

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
  public @NotNull FormattableType getFormattableType() {
    return new FormattableType(Field.class);
  }


  @Override
  public @NotNull MatchResult compareToStringKey(@NotNull Field value, @NotNull ComparatorContext context)
  {
    return context.getCompareType().match(value.getName().compareTo(context.getStringKeyValue()))
        ? EQUIVALENT : MISMATCH;
  }
}
