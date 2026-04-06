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
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.util.OptionalLong;

import static de.sayayi.lib.message.part.MapKey.MatchResult.forEmptyKey;
import static de.sayayi.lib.message.part.MapKey.MatchResult.forNullKey;


/**
 * Parameter formatter for {@link Reference} values (e.g. {@link java.lang.ref.WeakReference WeakReference},
 * {@link java.lang.ref.SoftReference SoftReference}).
 * <p>
 * This formatter unwraps the reference by calling {@link Reference#get()} and delegates the formatting of the
 * referenced value to the formatter appropriate for its type. If the referenced object has been garbage collected,
 * the value is treated as {@code null}.
 * <p>
 * Size queries and map key comparisons are also based on the referenced value.
 *
 * @author Jeroen Gremmen
 */
public final class ReferenceFormatter
    extends AbstractSingleTypeParameterFormatter<Reference<?>>
    implements SizeQueryable, MapKeyComparator<Reference<?>>
{
  /**
   * {@inheritDoc}
   * <p>
   * Unwraps the reference and delegates formatting of the referenced value.
   */
  @Override
  public @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull Reference<?> reference) {
    return context.format(reference.get());
  }


  /**
   * {@inheritDoc}
   * <p>
   * Returns the size of the value obtained from the reference.
   */
  @Override
  public @NotNull OptionalLong size(@NotNull ParameterFormatterContext context, @NotNull Object value) {
    return context.size(((Reference<?>)value).get());
  }


  /**
   * {@inheritDoc}
   *
   * @return  formattable type for {@link Reference}, never {@code null}
   */
  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(Reference.class);
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToNullKey(Reference<?> value, @NotNull ComparatorContext context)
  {
    return value == null || value.get() == null
        ? forNullKey(context.getCompareType(), true)
        : context.matchForObject(value.get());
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToEmptyKey(Reference<?> value, @NotNull ComparatorContext context)
  {
    return value == null || value.get() == null
        ? forEmptyKey(context.getCompareType(), true)
        : context.matchForObject(value.get());
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToBoolKey(@NotNull Reference<?> value, @NotNull ComparatorContext context) {
    return context.matchForObject(value.get());
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToNumberKey(@NotNull Reference<?> value, @NotNull ComparatorContext context) {
    return context.matchForObject(value.get());
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToStringKey(@NotNull Reference<?> value, @NotNull ComparatorContext context) {
    return context.matchForObject(value.get());
  }
}
