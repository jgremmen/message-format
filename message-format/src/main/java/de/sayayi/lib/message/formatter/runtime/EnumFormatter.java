/*
 * Copyright 2020 Jeroen Gremmen
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

import de.sayayi.lib.message.formatter.AbstractSingleTypeParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.ParameterFormatter.ConfigKeyComparator;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult;
import org.jetbrains.annotations.NotNull;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.Defined.*;


/**
 * @author Jeroen Gremmen
 */
public final class EnumFormatter
    extends AbstractSingleTypeParameterFormatter<Enum<?>>
    implements ConfigKeyComparator<Enum<?>>
{
  @Override
  protected @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull Enum<?> value)
  {
    switch(context.getConfigValueString("enum").orElse("name"))
    {
      case "ordinal":
      case "ord": {
        final int ordinal = value.ordinal();

        return formatUsingMappedNumber(context, ordinal, true)
            .orElseGet(() -> context.format(ordinal, int.class, true));
      }

      case "name": {
        final String name = value.name();

        return formatUsingMappedString(context, name, true).orElseGet(() -> noSpaceText(name));
      }
    }

    return context.delegateToNextFormatter();
  }


  @Override
  public @NotNull FormattableType getFormattableType() {
    return new FormattableType(Enum.class);
  }


  @Override
  public @NotNull MatchResult compareToNumberKey(@NotNull Enum<?> value, @NotNull ComparatorContext context)
  {
    return context.getCompareType()
        .match(Long.compare(value.ordinal(), context.getNumberKeyValue())) ? LENIENT : MISMATCH;
  }


  @Override
  public @NotNull MatchResult compareToStringKey(@NotNull Enum<?> value, @NotNull ComparatorContext context) {
    return context.getCompareType().match(value.name().compareTo(context.getStringKeyValue())) ? EXACT : MISMATCH;
  }
}
