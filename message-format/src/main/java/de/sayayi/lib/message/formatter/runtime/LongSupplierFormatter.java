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
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.LongSupplier;


/**
 * @author Jeroen Gremmen
 */
public final class LongSupplierFormatter
    extends AbstractSingleTypeParameterFormatter<LongSupplier>
    implements ConfigKeyComparator<LongSupplier>
{
  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull LongSupplier longSupplier) {
    return context.format(longSupplier.getAsLong(), long.class, true);
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
