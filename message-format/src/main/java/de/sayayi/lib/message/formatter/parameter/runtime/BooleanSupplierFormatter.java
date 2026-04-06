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
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MapKey.MatchResult;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;


/**
 * Parameter formatter for {@link BooleanSupplier} values.
 * <p>
 * This formatter evaluates the supplier by calling {@link BooleanSupplier#getAsBoolean()} and delegates the
 * formatting of the resulting {@code boolean} value to the appropriate boolean formatter.
 * <p>
 * Map key comparison for {@code bool} and {@code string} keys is also based on the evaluated boolean value.
 *
 * @author Jeroen Gremmen
 */
public final class BooleanSupplierFormatter
    extends AbstractSingleTypeParameterFormatter<BooleanSupplier>
    implements MapKeyComparator<BooleanSupplier>
{
  /**
   * {@inheritDoc}
   * <p>
   * Evaluates the supplier and delegates formatting of the resulting {@code boolean} value.
   */
  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull BooleanSupplier booleanSupplier) {
    return context.format(booleanSupplier.getAsBoolean(), boolean.class);
  }


  /**
   * {@inheritDoc}
   *
   * @return  formattable type for {@link BooleanSupplier}, never {@code null}
   */
  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(BooleanSupplier.class);
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToBoolKey(@NotNull BooleanSupplier booleanSupplier,
                                               @NotNull ComparatorContext context) {
    return context.matchForObject(booleanSupplier.getAsBoolean(), boolean.class);
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToStringKey(@NotNull BooleanSupplier booleanSupplier,
                                                 @NotNull ComparatorContext context) {
    return context.matchForObject(booleanSupplier.getAsBoolean(), boolean.class);
  }
}
