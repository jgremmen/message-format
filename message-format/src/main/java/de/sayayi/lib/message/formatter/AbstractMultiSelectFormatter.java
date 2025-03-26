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
package de.sayayi.lib.message.formatter;

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
 * @param <T>  parameter type
 *
 * @author Jeroen Gremmen
 * @since 0.12.0
 */
public abstract class AbstractMultiSelectFormatter<T> extends AbstractParameterFormatter<T>
{
  private final String configKey;
  private final String defaultConfigValueForAbsentKey;
  private final boolean delegateOnMismatch;
  private final Map<String,MultiSelectFunction<T>> multiSelectFunctionMap;


  protected AbstractMultiSelectFormatter(@NotNull String configKey) {
    this(configKey, null, true);
  }


  protected AbstractMultiSelectFormatter(@NotNull String configKey, String defaultConfigValueForAbsentKey,
                                         boolean delegateOnMismatch)
  {
    this.configKey = requireNonNull(configKey);
    this.defaultConfigValueForAbsentKey = defaultConfigValueForAbsentKey;
    this.delegateOnMismatch = delegateOnMismatch;

    multiSelectFunctionMap = new HashMap<>();
  }


  protected void register(@NotNull String[] configKeys, @NotNull MultiSelectFunction<T> function)
  {
    for(var configKey: requireNonNull(configKeys))
      register(configKey, function);
  }


  protected void register(@NotNull String configValue, @NotNull MultiSelectFunction<T> function) {
    multiSelectFunctionMap.put(requireNonNull(configValue), requireNonNull(function));
  }


  @Override
  protected @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull T value)
  {
    var function = multiSelectFunctionMap
        .get(context.getConfigValueString(configKey).orElse(defaultConfigValueForAbsentKey));

    if (function != null)
      return function.apply(context, value);
    else if (delegateOnMismatch)
      return context.delegateToNextFormatter();
    else
      return formatMultiSelectMismatch(context, value);
  }


  @Contract(pure = true)
  @SuppressWarnings("unused")
  protected Text formatMultiSelectMismatch(@NotNull FormatterContext context, @NotNull T value) {
    return nullText();
  }


  @Override
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames() {
    return Set.of(configKey);
  }




  protected interface MultiSelectFunction<T>
  {
    @NotNull Text apply(@NotNull FormatterContext context, @NotNull T value);
  }
}
