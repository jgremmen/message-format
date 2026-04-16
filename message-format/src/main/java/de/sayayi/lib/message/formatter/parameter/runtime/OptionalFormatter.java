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
 * Parameter formatter for {@link Optional} values.
 * <p>
 * If the optional contains a value, it is unwrapped and formatting is delegated to the formatter appropriate for the
 * contained value's type. If the optional is empty, it is treated as an empty value.
 * <p>
 * Size queries are delegated to the contained value. An empty optional has no size.
 * <p>
 * Map key comparisons for {@code bool}, {@code number} and {@code string} keys are based on the contained value.
 * An empty optional results in a mismatch for these key types.
 *
 * @author Jeroen Gremmen
 */
public final class OptionalFormatter extends AbstractSingleTypeParameterFormatter<Optional<?>>
    implements SizeQueryable, MapKeyComparator<Optional<?>>
{
  @Override
  protected boolean updateTypedClassifiers(@NotNull ClassifierContext context, @NotNull Optional<?> value)
  {
    context.addClassifier("optional");
    value.ifPresent(context::updateClassifiers);

    return true;
  }


  /**
   * {@inheritDoc}
   * <p>
   * Unwraps the optional and delegates formatting of the contained value. Returns empty text if the optional is empty.
   */
  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull Optional<?> optional)
  {
    return optional
        .map(context::format)
        .orElseGet(TextPartFactory::emptyText);
  }


  /**
   * {@inheritDoc}
   * <p>
   * Returns the size of the value contained in the optional, or empty if the optional has no value.
   */
  @Override
  public @NotNull OptionalLong size(@NotNull ParameterFormatterContext context, @NotNull Object optional)
  {
    return ((Optional<?>)optional)
        .map(context::size)
        .orElseGet(OptionalLong::empty);
  }


  /**
   * {@inheritDoc}
   *
   * @return  formattable type for {@link Optional}, never {@code null}
   */
  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(Optional.class);
  }


  /** {@inheritDoc} */
  @Override
  @SuppressWarnings("OptionalAssignedToNull")
  public @NotNull MatchResult compareToNullKey(Optional<?> optional, @NotNull ComparatorContext context)
  {
    return optional == null || optional.isEmpty()
        ? forNullKey(context.getCompareType(), true)
        : context.matchForObject(optional.get());
  }


  /** {@inheritDoc} */
  @Override
  @SuppressWarnings("OptionalAssignedToNull")
  public @NotNull MatchResult compareToEmptyKey(Optional<?> optional, @NotNull ComparatorContext context)
  {
    return optional == null || optional.isEmpty()
        ? forEmptyKey(context.getCompareType(), true)
        : context.matchForObject(optional.get());
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToBoolKey(@NotNull Optional<?> optional, @NotNull ComparatorContext context) {
    return optional.map(context::matchForObject).orElse(MISMATCH);
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToNumberKey(@NotNull Optional<?> optional, @NotNull ComparatorContext context) {
    return optional.map(context::matchForObject).orElse(MISMATCH);
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToStringKey(@NotNull Optional<?> optional, @NotNull ComparatorContext context) {
    return optional.map(context::matchForObject).orElse(MISMATCH);
  }
}
