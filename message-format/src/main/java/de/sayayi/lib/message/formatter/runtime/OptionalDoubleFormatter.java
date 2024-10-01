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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalDouble;

import static de.sayayi.lib.message.part.TextPartFactory.emptyText;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.Defined.MISMATCH;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.forEmptyKey;


/**
 * @author Jeroen Gremmen
 */
public final class OptionalDoubleFormatter
    extends AbstractSingleTypeParameterFormatter<OptionalDouble>
    implements ConfigKeyComparator<OptionalDouble>
{
  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull OptionalDouble optionalDouble)
  {
    return optionalDouble.isPresent()
        ? context.format(optionalDouble.getAsDouble(), double.class, true)
        : emptyText();
  }


  @Override
  public @NotNull FormattableType getFormattableType() {
    return new FormattableType(OptionalDouble.class);
  }


  @Override
  @SuppressWarnings("OptionalAssignedToNull")
  public @NotNull MatchResult compareToEmptyKey(OptionalDouble value, @NotNull ComparatorContext context) {
    return forEmptyKey(context.getCompareType(), value == null || value.isEmpty());
  }


  @Override
  public @NotNull MatchResult compareToBoolKey(@NotNull OptionalDouble value, @NotNull ComparatorContext context) {
    return value.isPresent() ? context.matchForObject(value.getAsDouble(), double.class) : MISMATCH;
  }


  @Override
  public @NotNull MatchResult compareToNumberKey(@NotNull OptionalDouble value, @NotNull ComparatorContext context) {
    return value.isPresent() ? context.matchForObject(value.getAsDouble(), double.class) : MISMATCH;
  }


  @Override
  public @NotNull MatchResult compareToStringKey(@NotNull OptionalDouble value, @NotNull ComparatorContext context) {
    return value.isPresent() ? context.matchForObject(value.getAsDouble(), double.class) : MISMATCH;
  }
}
