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

import java.util.OptionalInt;

import static de.sayayi.lib.message.formatter.parameter.ParameterFormatter.ClassifierContext.CLASSIFIER_NUMBER;
import static de.sayayi.lib.message.part.MapKey.MatchResult.Defined.MISMATCH;
import static de.sayayi.lib.message.part.MapKey.MatchResult.forEmptyKey;
import static de.sayayi.lib.message.part.TextPartFactory.emptyText;


/**
 * Parameter formatter for {@link OptionalInt} values.
 * <p>
 * If the optional contains a value, it is unwrapped and formatting is delegated to the appropriate {@code int}
 * formatter. If the optional is empty, it is treated as an empty value.
 * <p>
 * Map key comparisons for {@code bool}, {@code number} and {@code string} keys are based on the contained int value.
 * An empty optional results in a mismatch for these key types.
 *
 * @author Jeroen Gremmen
 */
public final class OptionalIntFormatter
    extends AbstractSingleTypeParameterFormatter<OptionalInt>
    implements MapKeyComparator<OptionalInt>
{
  @Override
  protected boolean updateTypedClassifiers(@NotNull ClassifierContext context, @NotNull OptionalInt value)
  {
    context.addClassifier("optional");
    if (value.isPresent())
      context.addClassifier(CLASSIFIER_NUMBER);

    return true;
  }


  /**
   * {@inheritDoc}
   * <p>
   * Unwraps the optional and delegates formatting of the contained {@code int} value. Returns empty text if the
   * optional is empty.
   */
  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull OptionalInt optionalInt)
  {
    return optionalInt.isPresent()
        ? context.format(optionalInt.getAsInt(), int.class)
        : emptyText();
  }


  /**
   * {@inheritDoc}
   *
   * @return  formattable type for {@link OptionalInt}, never {@code null}
   */
  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(OptionalInt.class);
  }


  /** {@inheritDoc} */
  @Override
  @SuppressWarnings("OptionalAssignedToNull")
  public @NotNull MatchResult compareToEmptyKey(OptionalInt optionalInt, @NotNull ComparatorContext context) {
    return forEmptyKey(context.getCompareType(), optionalInt == null || optionalInt.isEmpty());
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToBoolKey(@NotNull OptionalInt optionalInt,
                                               @NotNull ComparatorContext context) {
    return optionalInt.isPresent() ? context.matchForObject(optionalInt.getAsInt(), int.class) : MISMATCH;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToNumberKey(@NotNull OptionalInt optionalInt,
                                                 @NotNull ComparatorContext context) {
    return optionalInt.isPresent() ? context.matchForObject(optionalInt.getAsInt(), int.class) : MISMATCH;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToStringKey(@NotNull OptionalInt optionalInt,
                                                 @NotNull ComparatorContext context) {
    return optionalInt.isPresent() ? context.matchForObject(optionalInt.getAsInt(), int.class) : MISMATCH;
  }
}
