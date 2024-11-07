/*
 * Copyright 2019 Jeroen Gremmen
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
import de.sayayi.lib.message.formatter.ParameterFormatter.SizeQueryable;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.TextPartFactory;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.OptionalLong;

import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.Defined.MISMATCH;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.forEmptyKey;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.forNullKey;


/**
 * @author Jeroen Gremmen
 */
public final class OptionalFormatter extends AbstractSingleTypeParameterFormatter<Optional<?>>
    implements SizeQueryable, ConfigKeyComparator<Optional<?>>
{
  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull Optional<?> optional)
  {
    return optional
        .map(o -> context.format(o, true))
        .orElseGet(TextPartFactory::emptyText);
  }


  @Override
  public @NotNull OptionalLong size(@NotNull FormatterContext context, @NotNull Object optional)
  {
    return ((Optional<?>)optional)
        .map(context::size)
        .orElseGet(OptionalLong::empty);
  }


  @Override
  public @NotNull FormattableType getFormattableType() {
    return new FormattableType(Optional.class);
  }


  @Override
  @SuppressWarnings("OptionalAssignedToNull")
  public @NotNull MatchResult compareToNullKey(Optional<?> optional, @NotNull ComparatorContext context)
  {
    return optional == null || optional.isEmpty()
        ? forNullKey(context.getCompareType(), true)
        : context.matchForObject(optional.get());
  }


  @Override
  @SuppressWarnings("OptionalAssignedToNull")
  public @NotNull MatchResult compareToEmptyKey(Optional<?> optional, @NotNull ComparatorContext context)
  {
    return optional == null || optional.isEmpty()
        ? forEmptyKey(context.getCompareType(), true)
        : context.matchForObject(optional.get());
  }


  @Override
  public @NotNull MatchResult compareToBoolKey(@NotNull Optional<?> optional, @NotNull ComparatorContext context) {
    return optional.map(context::matchForObject).orElse(MISMATCH);
  }


  @Override
  public @NotNull MatchResult compareToNumberKey(@NotNull Optional<?> optional, @NotNull ComparatorContext context) {
    return optional.map(context::matchForObject).orElse(MISMATCH);
  }


  @Override
  public @NotNull MatchResult compareToStringKey(@NotNull Optional<?> optional, @NotNull ComparatorContext context) {
    return optional.map(context::matchForObject).orElse(MISMATCH);
  }
}
