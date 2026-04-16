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
import de.sayayi.lib.message.formatter.parameter.AbstractMultiSelectFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter.MapKeyComparator;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MapKey.MatchResult;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static de.sayayi.lib.message.formatter.parameter.ParameterFormatter.ClassifierContext.CLASSIFIER_ENUM;
import static de.sayayi.lib.message.part.MapKey.MatchResult.Defined.*;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;


/**
 * Parameter formatter for {@link Enum} values.
 * <p>
 * This formatter uses the {@code enum} configuration key to select the output format:
 * <ul>
 *   <li>
 *     {@code name} (default) &ndash; formats the enum constant's name; string map keys can be used to override the
 *     output for specific names
 *   </li>
 *   <li>
 *     {@code ord} or {@code ordinal} &ndash; formats the enum constant's ordinal value; number map keys can be used
 *     to override the output for specific ordinals
 *   </li>
 * </ul>
 * <p>
 * If the configuration key is absent, the enum's name is used. If the value does not match a known option, formatting
 * is delegated to the next available formatter.
 * <p>
 * Map key comparison supports {@code number} keys (compared against the ordinal) and {@code string} keys
 * (compared against the name).
 *
 * @author Jeroen Gremmen
 */
public final class EnumFormatter extends AbstractMultiSelectFormatter<Enum<?>> implements MapKeyComparator<Enum<?>>
{
  @Override
  public boolean updateClassifiers(@NotNull ClassifierContext context, @NotNull Object value)
  {
    context.addClassifier(CLASSIFIER_ENUM);

    return true;
  }


  /**
   * Creates a new enum formatter with the configuration key {@code enum} and selection options for name and ordinal.
   */
  public EnumFormatter()
  {
    super("enum", "name", true);

    register("name", this::formatEnumName);
    register(new String[] { "ord", "ordinal" }, this::formatEnumOrdinal);
  }


  private @NotNull Text formatEnumOrdinal(@NotNull ParameterFormatterContext context, @NotNull Enum<?> enumValue)
  {
    final var ordinal = enumValue.ordinal();

    return formatUsingMappedNumber(context, ordinal, true)
        .orElseGet(() -> context.format(ordinal, int.class));
  }


  private @NotNull Text formatEnumName(@NotNull ParameterFormatterContext context, @NotNull Enum<?> enumValue)
  {
    final var name = enumValue.name();

    return formatUsingMappedString(context, name, true).orElseGet(() -> noSpaceText(name));
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToNumberKey(@NotNull Enum<?> enumValue, @NotNull ComparatorContext context)
  {
    return context.getCompareType()
        .match(Long.compare(enumValue.ordinal(), context.getNumberKeyValue())) ? LENIENT : MISMATCH;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToStringKey(@NotNull Enum<?> enumValue, @NotNull ComparatorContext context) {
    return context.getCompareType().match(enumValue.name().compareTo(context.getStringKeyValue())) ? EXACT : MISMATCH;
  }


  /**
   * {@inheritDoc}
   *
   * @return  a set containing the {@link Enum} formattable type, never {@code null}
   */
  @Override
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return Set.of(new FormattableType(Enum.class));
  }
}
