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

import java.util.function.LongSupplier;


/**
 * @author Jeroen Gremmen
 */
public final class LongSupplierFormatter
    extends AbstractSingleTypeParameterFormatter<LongSupplier>
    implements MapKeyComparator<LongSupplier>
{
  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull LongSupplier longSupplier) {
    return context.format(longSupplier.getAsLong(), long.class);
  }


  @Override
  public @NotNull FormattableType getFormattableType() {
    return new FormattableType(LongSupplier.class);
  }


  @Override
  public @NotNull MatchResult compareToBoolKey(@NotNull LongSupplier longSupplier,
                                               @NotNull ComparatorContext context) {
    return context.matchForObject(longSupplier.getAsLong(), long.class);
  }


  @Override
  public @NotNull MatchResult compareToNumberKey(@NotNull LongSupplier longSupplier,
                                                 @NotNull ComparatorContext context) {
    return context.matchForObject(longSupplier.getAsLong(), long.class);
  }


  @Override
  public @NotNull MatchResult compareToStringKey(@NotNull LongSupplier longSupplier,
                                                 @NotNull ComparatorContext context) {
    return context.matchForObject(longSupplier.getAsLong(), long.class);
  }
}
