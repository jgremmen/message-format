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
package de.sayayi.lib.message.formatter.parameter.runtime;

import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.parameter.AbstractSingleTypeParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter.MapKeyComparator;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter.SizeQueryable;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MapKey.MatchResult;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.TextPartFactory;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.OptionalLong;

import static de.sayayi.lib.message.part.MapKey.MatchResult.Defined.MISMATCH;
import static de.sayayi.lib.message.part.MapKey.MatchResult.forEmptyKey;
import static de.sayayi.lib.message.part.MapKey.MatchResult.forNullKey;


/**
 * @author Jeroen Gremmen
 */
public final class OptionalFormatter extends AbstractSingleTypeParameterFormatter<Optional<?>>
    implements SizeQueryable, MapKeyComparator<Optional<?>>
{
  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull Optional<?> optional)
  {
    return optional
        .map(context::format)
        .orElseGet(TextPartFactory::emptyText);
  }


  @Override
  public @NotNull OptionalLong size(@NotNull ParameterFormatterContext context, @NotNull Object optional)
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
