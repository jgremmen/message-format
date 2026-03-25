/*
 * Copyright 2024 Jeroen Gremmen
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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static de.sayayi.lib.message.part.TextPartFactory.nullText;
import static java.util.Objects.requireNonNull;


/**
 * Base class for parameter formatters that select a formatting strategy based on a configuration key value.
 * <p>
 * A multi-select formatter reads the string value of a named configuration key from the parameter configuration
 * (e.g. {@code %{val,locale:'country'}}). That value is then used to look up a {@link MultiSelectFunction} registered
 * by the subclass. If a matching function is found, it formats the value; otherwise the formatter either
 * {@linkplain ParameterFormatterContext#delegateToNextFormatter() delegates to the next formatter} or falls back to
 * {@link #formatMultiSelectMismatch(ParameterFormatterContext, Object)}, depending on the {@code delegateOnMismatch}
 * flag.
 * <p>
 * Subclasses typically register their formatting functions in their constructor:
 * <pre>{@code
 * public MyFormatter() {
 *   super("my-key", "default-value", true);
 *
 *   register("option-a", this::formatOptionA);
 *   register(new String[] { "option-b", "option-c" }, this::formatOptionBC);
 * }}</pre>
 * <p>
 * The {@code null}/empty value handling inherited from {@link AbstractParameterFormatter} is applied before the
 * multi-select dispatch, so registered functions only receive non-{@code null} values.
 *
 * @param <T>  the parameter value type handled by this formatter
 *
 * @author Jeroen Gremmen
 * @since 0.12.0
 */
public abstract class AbstractMultiSelectFormatter<T> extends AbstractParameterFormatter<T>
{
  /** Configuration key name used to select the formatting function. */
  private final String configKey;

  /**
   * Config value to use when the config key is absent from the parameter configuration, or {@code null} if no default
   * is defined.
   */
  private final String defaultConfigValueForAbsentKey;

  /** Whether to delegate to the next formatter when no registered function matches. */
  private final boolean delegateOnMismatch;

  /** Map from config value strings to their associated formatting functions. */
  private final Map<String,MultiSelectFunction<T>> multiSelectFunctionMap;


  /**
   * Creates a multi-select formatter with the given configuration key, no default config value for an absent key,
   * and delegation enabled on mismatch.
   * <p>
   * This is equivalent to calling {@link #AbstractMultiSelectFormatter(String, String, boolean)
   * AbstractMultiSelectFormatter(configKey, null, true)}.
   *
   * @param configKey  the configuration key name used to select the formatting function, not {@code null}
   */
  protected AbstractMultiSelectFormatter(@NotNull String configKey) {
    this(configKey, null, true);
  }


  /**
   * Creates a multi-select formatter with full control over the selection behavior.
   *
   * @param configKey                       the configuration key name used to look up the config value in the
   *                                        parameter configuration, not {@code null}
   * @param defaultConfigValueForAbsentKey  the config value to use when the config key is absent from the parameter
   *                                        configuration, or {@code null} if no default is defined (resulting in a
   *                                        mismatch when the key is absent)
   * @param delegateOnMismatch              {@code true} to
   *                                        {@linkplain ParameterFormatterContext#delegateToNextFormatter() delegate to
   *                                        the next formatter} when no registered function matches the config value,
   *                                        {@code false} to call
   *                                        {@link #formatMultiSelectMismatch(ParameterFormatterContext, Object)}
   *                                        instead
   */
  protected AbstractMultiSelectFormatter(@NotNull String configKey, String defaultConfigValueForAbsentKey,
                                         boolean delegateOnMismatch)
  {
    this.configKey = requireNonNull(configKey);
    this.defaultConfigValueForAbsentKey = defaultConfigValueForAbsentKey;
    this.delegateOnMismatch = delegateOnMismatch;

    multiSelectFunctionMap = new HashMap<>();
  }


  /**
   * Registers the same formatting function for multiple config values.
   *
   * @param configKeys  the config value strings that should map to the given function, not {@code null}
   * @param function    the formatting function to invoke when one of the config values matches, not {@code null}
   *
   * @see #register(String, MultiSelectFunction)
   */
  protected void register(@NotNull String[] configKeys, @NotNull MultiSelectFunction<T> function)
  {
    for(var configKey: requireNonNull(configKeys))
      register(configKey, function);
  }


  /**
   * Registers a formatting function for a single config value.
   *
   * @param configValue  the config value string that should trigger the given function, not {@code null}
   * @param function     the formatting function to invoke when the config value matches, not {@code null}
   */
  protected void register(@NotNull String configValue, @NotNull MultiSelectFunction<T> function) {
    multiSelectFunctionMap.put(requireNonNull(configValue), requireNonNull(function));
  }


  /**
   * Looks up the config value for the configured key from the parameter configuration, resolves the corresponding
   * registered {@link MultiSelectFunction}, and applies it to the given value.
   * <p>
   * If the config key is absent from the parameter configuration, the {@code defaultConfigValueForAbsentKey} is used
   * instead.  If no registered function matches the resolved config value, behavior depends on the
   * {@code delegateOnMismatch} flag:
   * <ul>
   *   <li>
   *     {@code true} — delegates formatting to the {@linkplain ParameterFormatterContext#delegateToNextFormatter()
   *     next formatter}
   *   </li>
   *   <li>{@code false} — calls {@link #formatMultiSelectMismatch(ParameterFormatterContext, Object)}</li>
   * </ul>
   *
   * @param context  formatter context, not {@code null}
   * @param value    value to be formatted, not {@code null}
   *
   * @return  formatted text, never {@code null}
   */
  @Override
  protected @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull T value)
  {
    final var function = multiSelectFunctionMap
        .get(context.getConfigValueString(configKey).orElse(defaultConfigValueForAbsentKey));

    if (function != null)
      return function.apply(context, value);
    else if (delegateOnMismatch)
      return context.delegateToNextFormatter();
    else
      return formatMultiSelectMismatch(context, value);
  }


  /**
   * Fallback formatting method invoked when no registered {@link MultiSelectFunction} matches the config value and
   * {@code delegateOnMismatch} is {@code false}.
   * <p>
   * The default implementation returns {@linkplain de.sayayi.lib.message.part.TextPartFactory#nullText() null text}.
   * Subclasses may override this method to provide alternative fallback behavior.
   *
   * @param context  formatter context, not {@code null}
   * @param value    value to be formatted, not {@code null}
   *
   * @return  formatted text, or {@code null} to represent a null text value
   */
  @Contract(pure = true)
  @SuppressWarnings("unused")
  protected Text formatMultiSelectMismatch(@NotNull ParameterFormatterContext context, @NotNull T value) {
    return nullText();
  }


  /**
   * Returns the configuration key used by this formatter wrapped in an immutable singleton set.
   *
   * @return  singleton set containing the config key name, never {@code null}
   */
  @Override
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames() {
    return Set.of(configKey);
  }




  /**
   * A function that formats a non-{@code null} value of type {@code T} within a given
   * {@link ParameterFormatterContext}.
   *
   * @param <T>  the parameter value type
   *
   * @see #register(String, MultiSelectFunction)
   * @see #register(String[], MultiSelectFunction)
   */
  @FunctionalInterface
  protected interface MultiSelectFunction<T>
  {
    /**
     * Formats the given {@code value}.
     *
     * @param context  formatter context, not {@code null}
     * @param value    value to format, not {@code null}
     *
     * @return  formatted text, never {@code null}
     */
    @NotNull Text apply(@NotNull ParameterFormatterContext context, @NotNull T value);
  }
}
