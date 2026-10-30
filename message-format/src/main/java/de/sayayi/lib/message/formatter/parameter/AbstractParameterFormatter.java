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
package de.sayayi.lib.message.formatter.parameter;

import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.TextPartFactory;
import org.jetbrains.annotations.NotNull;

import static de.sayayi.lib.message.part.MapKey.EMPTY_NULL_TYPE;
import static de.sayayi.lib.message.part.TextPartFactory.nullText;


/**
 * This class provides a basic formatter implementation that handles empty and {@code null} values and is suitable for
 * most parameter formatters.
 * <p>
 * Method {@link #format(ParameterFormatterContext, Object)} takes care of mapping {@code null} and empty values. It
 * does so for both the value to be formatted and the formatted text returned by
 * {@link #formatValue(ParameterFormatterContext, Object)}.
 * <p>
 * If a {@code null} or empty value is matched in the parameter configuration (e.g.
 * {@code %{val,null:'no text',empty:'empty text'} }) the associated message is returned.
 * If no match was found for {@code null} then {@link TextPartFactory#nullText() nullText()} is returned.
 * <p>
 * Non-{@code null} values are delegated to {@link #formatValue(ParameterFormatterContext, Object)}. The returned text
 * will be matched against {@code null} and empty parameter configuration entries and is replaced by the best matching
 * message found.
 * <p>
 * <b>Examples:</b>
 * <p>
 * A subclass only provides the formatted text and its supported types. Both the {@code null} and {@code empty} map keys
 * work without further code, including for an empty text returned by {@code formatValue}:
 * <pre>{@code
 * public final class DurationFormatter extends AbstractParameterFormatter<Duration>
 * {
 *   @Override
 *   protected @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull Duration duration) {
 *     return duration.isZero() ? emptyText() : noSpaceText(duration.toMinutes() + " min");
 *   }
 *
 *   @Override
 *   public @NotNull Set<FormattableType> getFormattableTypes() {
 *     return Set.of(new FormattableType(Duration.class));
 *   }
 * }
 * }</pre>
 * <pre>{@code
 * var formatterService = new DefaultFormatterService();
 * formatterService.addFormatter(new DurationFormatter());
 *
 * var messageSupport = MessageSupportFactory.create(formatterService).seal();
 * var message = "Timeout: %{timeout,null:'not set',empty:'disabled'}";
 *
 * messageSupport.message(message).with("timeout", Duration.ofMinutes(5)).format();
 * // "Timeout: 5 min"
 *
 * messageSupport.message(message).with("timeout", Duration.ZERO).format();
 * // "Timeout: disabled"
 *
 * messageSupport.message(message).with("timeout", null).format();
 * // "Timeout: not set"
 * }</pre>
 *
 * @param <T>  parameter type
 *
 * @author Jeroen Gremmen
 * @since 0.3.1
 */
@SuppressWarnings("WeakerAccess")
public abstract class AbstractParameterFormatter<T> implements ParameterFormatter
{
  /**
   * {@inheritDoc}
   * <p>
   * This implementation first matches {@code value} against the {@code null} and {@code empty} keys of the parameter
   * configuration map. If no key matches and {@code value} is not {@code null}, formatting is delegated to
   * {@link #formatValue(ParameterFormatterContext, Object)} and the resulting text is matched against the same keys.
   */
  @Override
  @SuppressWarnings("unchecked")
  public final @NotNull Text format(@NotNull ParameterFormatterContext context, Object value)
  {
    // handle empty, !empty, null and !null first
    final var msg = context
        .getMapMessage(value, EMPTY_NULL_TYPE)
        .orElse(null);

    if (msg != null)
      return context.format(msg);

    if (value == null)
      return nullText();

    final var text = formatValue(context, (T)value);

    // handle empty, !empty, null and !null for result
    return context
        .getMapMessage(text.getText(), EMPTY_NULL_TYPE)
        .map(context::format)
        .orElse(text);
  }


  /**
   * Formats {@code value} using {@code context}.
   * <p>
   * This method differs from {@link #format(ParameterFormatterContext, Object)} in that it has already handled
   * {@code empty} and {@code null} cases matched in the parameter configuration.
   * <p>
   * In the same way it will handle {@code empty} and {@code null} cases for the text returned by this method.
   *
   * @param context  formatter context, not {@code null}
   * @param value    value to be formatted, not {@code null}
   *
   * @return  formatted text, never {@code null}
   */
  protected abstract @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull T value);
}
