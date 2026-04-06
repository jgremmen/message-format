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
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Parameter formatter for {@link AtomicBoolean} values.
 * <p>
 * This formatter extracts the boolean value from the {@link AtomicBoolean} using {@link AtomicBoolean#get()} and
 * delegates the formatting to the appropriate boolean formatter.
 * <p>
 * Map key comparison for {@code bool} and {@code string} keys is also based on the extracted boolean value.
 *
 * @author Jeroen Gremmen
 */
public final class AtomicBooleanFormatter
    extends AbstractSingleTypeParameterFormatter<AtomicBoolean>
    implements MapKeyComparator<AtomicBoolean>
{
  /**
   * {@inheritDoc}
   * <p>
   * Extracts the boolean value and delegates formatting to the boolean formatter.
   */
  @Override
  public @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull AtomicBoolean atomicBoolean) {
    return context.format(atomicBoolean.get(), boolean.class);
  }


  /**
   * {@inheritDoc}
   *
   * @return  formattable type for {@link AtomicBoolean}, never {@code null}
   */
  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(AtomicBoolean.class);
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToBoolKey(@NotNull AtomicBoolean value, @NotNull ComparatorContext context) {
    return context.matchForObject(value.get(), boolean.class);
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToStringKey(@NotNull AtomicBoolean value, @NotNull ComparatorContext context) {
    return context.matchForObject(value.get(), boolean.class);
  }
}
