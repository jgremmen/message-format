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
package de.sayayi.lib.message.formatter.parameter.runtime;

import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.parameter.AbstractSingleTypeParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter.MapKeyComparator;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MapKey.MatchResult;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalDouble;

import static de.sayayi.lib.message.part.MapKey.MatchResult.Defined.MISMATCH;
import static de.sayayi.lib.message.part.MapKey.MatchResult.forEmptyKey;
import static de.sayayi.lib.message.part.TextPartFactory.emptyText;


/**
 * Parameter formatter for {@link OptionalDouble} values.
 * <p>
 * If the optional contains a value, it is unwrapped and formatting is delegated to the appropriate {@code double}
 * formatter. If the optional is empty, it is treated as an empty value.
 * <p>
 * Map key comparisons for {@code bool}, {@code number} and {@code string} keys are based on the contained double
 * value. An empty optional results in a mismatch for these key types.
 *
 * @author Jeroen Gremmen
 */
public final class OptionalDoubleFormatter
    extends AbstractSingleTypeParameterFormatter<OptionalDouble>
    implements MapKeyComparator<OptionalDouble>
{
  /**
   * {@inheritDoc}
   * <p>
   * Unwraps the optional and delegates formatting of the contained {@code double} value. Returns empty text if the
   * optional is empty.
   */
  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull OptionalDouble optionalDouble)
  {
    return optionalDouble.isPresent()
        ? context.format(optionalDouble.getAsDouble(), double.class)
        : emptyText();
  }


  /**
   * {@inheritDoc}
   *
   * @return  formattable type for {@link OptionalDouble}, never {@code null}
   */
  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(OptionalDouble.class);
  }


  /** {@inheritDoc} */
  @Override
  @SuppressWarnings("OptionalAssignedToNull")
  public @NotNull MatchResult compareToEmptyKey(OptionalDouble optionalDouble, @NotNull ComparatorContext context) {
    return forEmptyKey(context.getCompareType(), optionalDouble == null || optionalDouble.isEmpty());
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToBoolKey(@NotNull OptionalDouble optionalDouble,
                                               @NotNull ComparatorContext context)
  {
    return optionalDouble.isPresent()
        ? context.matchForObject(optionalDouble.getAsDouble(), double.class)
        : MISMATCH;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToNumberKey(@NotNull OptionalDouble optionalDouble,
                                                 @NotNull ComparatorContext context)
  {
    return optionalDouble.isPresent()
        ? context.matchForObject(optionalDouble.getAsDouble(), double.class)
        : MISMATCH;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToStringKey(@NotNull OptionalDouble optionalDouble,
                                                 @NotNull ComparatorContext context)
  {
    return optionalDouble.isPresent()
        ? context.matchForObject(optionalDouble.getAsDouble(), double.class)
        : MISMATCH;
  }
}
