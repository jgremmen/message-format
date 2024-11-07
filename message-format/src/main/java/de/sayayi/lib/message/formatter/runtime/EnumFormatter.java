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

import de.sayayi.lib.message.formatter.AbstractMultiSelectFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.ParameterFormatter.ConfigKeyComparator;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.Defined.*;


/**
 * @author Jeroen Gremmen
 */
public final class EnumFormatter extends AbstractMultiSelectFormatter<Enum<?>> implements ConfigKeyComparator<Enum<?>>
{
  public EnumFormatter()
  {
    super("enum", "name", true);

    register("name", this::formatEnumName);
    register(new String[] { "ord", "ordinal" }, this::formatEnumOrdinal);
  }


  private @NotNull Text formatEnumOrdinal(@NotNull FormatterContext context, @NotNull Enum<?> enumValue)
  {
    final int ordinal = enumValue.ordinal();

    return formatUsingMappedNumber(context, ordinal, true)
        .orElseGet(() -> context.format(ordinal, int.class));
  }


  private @NotNull Text formatEnumName(@NotNull FormatterContext context, @NotNull Enum<?> enumValue)
  {
    final String name = enumValue.name();

    return formatUsingMappedString(context, name, true).orElseGet(() -> noSpaceText(name));
  }


  @Override
  public @NotNull MatchResult compareToNumberKey(@NotNull Enum<?> enumValue, @NotNull ComparatorContext context)
  {
    return context.getCompareType()
        .match(Long.compare(enumValue.ordinal(), context.getNumberKeyValue())) ? LENIENT : MISMATCH;
  }


  @Override
  public @NotNull MatchResult compareToStringKey(@NotNull Enum<?> enumValue, @NotNull ComparatorContext context) {
    return context.getCompareType().match(enumValue.name().compareTo(context.getStringKeyValue())) ? EXACT : MISMATCH;
  }


  @Override
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return Set.of(new FormattableType(Enum.class));
  }
}
