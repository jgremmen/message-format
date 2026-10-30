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

import java.util.OptionalLong;

import static de.sayayi.lib.message.formatter.parameter.ParameterFormatter.ClassifierContext.CLASSIFIER_NUMBER;
import static de.sayayi.lib.message.part.MapKey.MatchResult.Defined.MISMATCH;
import static de.sayayi.lib.message.part.MapKey.MatchResult.forEmptyKey;
import static de.sayayi.lib.message.part.TextPartFactory.emptyText;


/**
 * Parameter formatter for {@link OptionalLong} values.
 * <p>
 * If the optional contains a value, it is unwrapped and formatting is delegated to the appropriate {@code long}
 * formatter. If the optional is empty, it is treated as an empty value.
 * <p>
 * Map key comparisons for {@code bool}, {@code number} and {@code string} keys are based on the contained long value.
 * An empty optional results in a mismatch for these key types.
 * <p>
 * <b>Examples:</b>
 * <p>
 * Number keys and configuration keys apply to the contained value and the {@code empty} key handles an absent one:
 * <pre>{@code
 * var message = "Rows: %{rows,number:'#,##0',empty:'not counted',0:'none'}";
 *
 * messageSupport.message(message).with("rows", OptionalLong.of(1_250_000L)).locale(Locale.US).format();
 * // "Rows: 1,250,000"
 *
 * messageSupport.message(message).with("rows", OptionalLong.of(0L)).locale(Locale.US).format();
 * // "Rows: none"
 *
 * messageSupport.message(message).with("rows", OptionalLong.empty()).locale(Locale.US).format();
 * // "Rows: not counted"
 * }</pre>
 *
 * @author Jeroen Gremmen
 * @since 0.4.2
 */
public final class OptionalLongFormatter
    extends AbstractSingleTypeParameterFormatter<OptionalLong>
    implements MapKeyComparator<OptionalLong>
{
  /**
   * {@inheritDoc}
   * <p>
   * Adds the {@code optional} classifier. If the optional contains a value, the
   * {@link ClassifierContext#CLASSIFIER_NUMBER number} classifier is added as well.
   */
  @Override
  protected boolean updateTypedClassifiers(@NotNull ClassifierContext context, @NotNull OptionalLong value)
  {
    context.addClassifier("optional");
    if (value.isPresent())
      context.addClassifier(CLASSIFIER_NUMBER);

    return true;
  }


  /**
   * {@inheritDoc}
   * <p>
   * Unwraps the optional and delegates formatting of the contained {@code long} value. Returns empty text if the
   * optional is empty.
   */
  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull OptionalLong optionalLong)
  {
    return optionalLong.isPresent()
        ? context.format(optionalLong.getAsLong(), long.class)
        : emptyText();
  }


  /**
   * {@inheritDoc}
   *
   * @return  formattable type for {@link OptionalLong}, never {@code null}
   */
  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(OptionalLong.class);
  }


  /** {@inheritDoc} */
  @Override
  @SuppressWarnings("OptionalAssignedToNull")
  public @NotNull MatchResult compareToEmptyKey(OptionalLong optionalLong, @NotNull ComparatorContext context) {
    return forEmptyKey(context.getCompareType(), optionalLong == null || optionalLong.isEmpty());
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToBoolKey(@NotNull OptionalLong optionalLong,
                                               @NotNull ComparatorContext context) {
    return optionalLong.isPresent() ? context.matchForObject(optionalLong.getAsLong(), long.class) : MISMATCH;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToNumberKey(@NotNull OptionalLong optionalLong,
                                                 @NotNull ComparatorContext context) {
    return optionalLong.isPresent() ? context.matchForObject(optionalLong.getAsLong(), long.class) : MISMATCH;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToStringKey(@NotNull OptionalLong optionalLong,
                                                 @NotNull ComparatorContext context) {
    return optionalLong.isPresent() ? context.matchForObject(optionalLong.getAsLong(), long.class) : MISMATCH;
  }
}
