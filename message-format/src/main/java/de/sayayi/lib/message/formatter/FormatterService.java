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
package de.sayayi.lib.message.formatter;

import de.sayayi.lib.message.formatter.parameter.NamedParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.named.StringFormatter;
import de.sayayi.lib.message.formatter.post.PostFormatter;
import de.sayayi.lib.message.part.MessagePart;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Map;
import java.util.Set;


/**
 * Service interface for resolving parameter formatters and post formatters.
 * <p>
 * A formatter service provides the ability to look up the appropriate {@link ParameterFormatter} instances for a
 * given value type, optional formatter name and parameter configuration. It also manages {@link PostFormatter}
 * instances that perform text post-processing after formatting.
 * <p>
 * The {@link WithRegistry} sub-interface adds mutating methods for registering formatters.
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
 *
 * @see GenericFormatterService
 * @see DefaultFormatterService
 */
public sealed interface FormatterService
    permits FormatterService.WithRegistry, GenericFormatterService.SealedFormatterService
{
  /**
   * Returns a list of parameter formatters for the given {@code format}, {@code type} and {@code config}.
   * <p>
   * Implementing classes must make sure that for any combination of {@code format} and {@code type} this function
   * always returns at least 1 formatter. A good choice for a default formatter would be {@link StringFormatter}.
   *
   * @param format  name of the formatter or {@code null}
   * @param type    type of the value to format
   * @param config  message part config
   *
   * @return  array of prioritized parameter formatters, never {@code null} and never empty
   *
   * @see GenericFormatterService
   */
  @Contract(value = "_, _, _ -> new", pure = true)
  @NotNull ParameterFormatter[] getFormatters(String format, @NotNull Class<?> type, MessagePart.Config config);


  /**
   * Returns all registered post formatters, keyed by their name.
   *
   * @return  unmodifiable map of post formatter name to post formatter, never {@code null}
   *
   * @since 0.21.0
   */
  @Contract(pure = true)
  @UnmodifiableView @NotNull Map<String,PostFormatter> getPostFormatters();


  /**
   * Return the accumulated set of parameter configuration names for all registered parameter formatters.
   *
   * @return  all known parameter config names, never {@code null}
   *
   * @since 0.20.0
   */
  @Contract(pure = true)
  @UnmodifiableView @NotNull Set<String> getParameterConfigNames();




  /**
   * Extension of {@link FormatterService} that adds methods for registering parameter formatters and post formatters.
   */
  sealed interface WithRegistry extends FormatterService permits GenericFormatterService
  {
    /**
     * Register a parameter formatter for the given formattable type. The formattable types returned by
     * {@link ParameterFormatter#getFormattableTypes()} are ignored by this method.
     *
     * @param formattableType  formattable type, not {@code null}
     * @param formatter        formatter to be registered, not {@code null}
     *
     * @see #addFormatter(ParameterFormatter)
     */
    @Contract(mutates = "this")
    void addFormatterForType(@NotNull FormattableType formattableType, @NotNull ParameterFormatter formatter);


    /**
     * Register a parameter formatter.
     * <p>
     * The formatter will be registered for all formattable types, returned by
     * {@link ParameterFormatter#getFormattableTypes()}.
     * <p>
     * If the formatter implements {@link NamedParameterFormatter}, it will additionally be registered as a named
     * parameter with the name returned by {@link NamedParameterFormatter#getName()}.
     *
     * @param formatter  parameter formatter, not {@code null}
     *
     * @see #addFormatterForType(FormattableType, ParameterFormatter)                   
     */
    @Contract(mutates = "this")
    void addFormatter(@NotNull ParameterFormatter formatter);


    /**
     * Register a post formatter.
     *
     * @param postFormatter  post formatter to register, not {@code null}
     *
     * @since 0.21.0
     */
    void addPostFormatter(@NotNull PostFormatter postFormatter);


    /**
     * Creates a sealed, immutable snapshot of this formatter service. The returned instance can no longer be modified.
     *
     * @return  an immutable formatter service, never {@code null}
     *
     * @since 0.22.0
     */
    @NotNull FormatterService seal();
  }
}
