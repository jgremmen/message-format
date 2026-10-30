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
 * This formatter uses the {@code enum} configuration key to select the output format.
 * <p>
 * If the configuration key is absent, the enum's name is used. If the value does not match a known option, formatting
 * is delegated to the next available formatter.
 * <p>
 * Map key comparison supports {@code number} keys (compared against the ordinal) and {@code string} keys
 * (compared against the name).
 * <p>
 * <b>Configuration keys:</b>
 * <table border="1">
 *   <caption>Configuration keys for the enum formatter</caption>
 *   <tr><th>Key</th><th>Values</th><th>Default</th><th>Purpose</th></tr>
 *   <tr>
 *     <td>{@code enum}</td>
 *     <td>{@code name}, {@code ord}, {@code ordinal}</td>
 *     <td>{@code name}</td>
 *     <td>
 *       Formats the name or the ordinal of the enum constant. String keys override the output for specific names
 *       and number keys for specific ordinals. Any other value delegates to the next formatter.
 *     </td>
 *   </tr>
 * </table>
 * <p>
 * <b>Examples:</b>
 * <p>
 * Translate enum constants using string keys. The default entry is used for all other constants:
 * <pre>{@code
 * var message = "Today is %{day,'SATURDAY':'weekend','SUNDAY':'weekend',:'a working day'}";
 *
 * messageSupport.message(message).with("day", DayOfWeek.SUNDAY).format();
 * // "Today is weekend"
 *
 * messageSupport.message(message).with("day", DayOfWeek.MONDAY).format();
 * // "Today is a working day"
 * }</pre>
 * <p>
 * Use the ordinal, optionally with number keys and comparison operators:
 * <pre>{@code
 * messageSupport.message("%{day,enum:ordinal}").with("day", DayOfWeek.WEDNESDAY).format();
 * // "2"
 *
 * messageSupport
 *     .message("%{day,enum:ord,<5:'working day',:'weekend'}")
 *     .with("day", DayOfWeek.FRIDAY)
 *     .format();
 * // "working day"
 * }</pre>
 * <p>
 * {@code null} is matched by the {@code null} key:
 * <pre>{@code
 * messageSupport
 *     .message("%{policy,null:'no policy'}")
 *     .with("policy", null)
 *     .format();
 * // "no policy"
 * }</pre>
 *
 * @author Jeroen Gremmen
 * @since 0.4.2
 */
public final class EnumFormatter extends AbstractMultiSelectFormatter<Enum<?>> implements MapKeyComparator<Enum<?>>
{
  /**
   * {@inheritDoc}
   * <p>
   * Adds the {@link ClassifierContext#CLASSIFIER_ENUM enum} classifier.
   */
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


  /**
   * Formats the ordinal of {@code enumValue}, using a message mapped to the ordinal if available.
   *
   * @param context    formatter context, not {@code null}
   * @param enumValue  enum value to format, not {@code null}
   *
   * @return  formatted ordinal, never {@code null}
   */
  private @NotNull Text formatEnumOrdinal(@NotNull ParameterFormatterContext context, @NotNull Enum<?> enumValue)
  {
    final var ordinal = enumValue.ordinal();

    return formatUsingMappedNumber(context, ordinal, true)
        .orElseGet(() -> context.format(ordinal, int.class));
  }


  /**
   * Formats the name of {@code enumValue}, using a message mapped to the name if available.
   *
   * @param context    formatter context, not {@code null}
   * @param enumValue  enum value to format, not {@code null}
   *
   * @return  formatted name, never {@code null}
   */
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
