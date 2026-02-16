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
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter.ConfigKeyComparator;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter.SizeQueryable;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MapKey.MatchResult;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalLong;
import java.util.function.Supplier;


/**
 * @author Jeroen Gremmen
 */
public final class SupplierFormatter
    extends AbstractSingleTypeParameterFormatter<Supplier<?>>
    implements SizeQueryable, ConfigKeyComparator<Supplier<?>>
{
  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull Supplier<?> supplier) {
    return context.format(supplier.get());
  }


  @Override
  public @NotNull OptionalLong size(@NotNull ParameterFormatterContext context, @NotNull Object supplier) {
    return context.size(((Supplier<?>)supplier).get());
  }


  @Override
  public @NotNull FormattableType getFormattableType() {
    return new FormattableType(Supplier.class);
  }


  @Override
  public @NotNull MatchResult compareToBoolKey(@NotNull Supplier<?> supplier, @NotNull ComparatorContext context) {
    return context.matchForObject(supplier.get());
  }


  @Override
  public @NotNull MatchResult compareToNumberKey(@NotNull Supplier<?> supplier, @NotNull ComparatorContext context) {
    return context.matchForObject(supplier.get());
  }


  @Override
  public @NotNull MatchResult compareToStringKey(@NotNull Supplier<?> supplier, @NotNull ComparatorContext context) {
    return context.matchForObject(supplier.get());
  }
}
