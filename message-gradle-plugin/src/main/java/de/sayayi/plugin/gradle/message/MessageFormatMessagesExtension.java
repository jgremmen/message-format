/*
 * Copyright 2026 Jeroen Gremmen
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
package de.sayayi.plugin.gradle.message;

import groovy.lang.GString;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static de.sayayi.lib.message.util.MessageUtil.findEnumValue;


/**
 * Nested extension for configuring message inclusion/exclusion filters and duplicate message handling strategy.
 * <p>
 * This extension is accessible via the {@code messages} block inside the {@code messageFormat} extension:
 * <pre>
 *   messageFormat {
 *     messages {
 *       include 'xy'
 *       exclude 'r.*'
 *       duplicateStrategy = 'fail'
 *     }
 *   }
 * </pre>
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 *
 * @see MessageFormatExtension
 */
public abstract class MessageFormatMessagesExtension
{
  /**
   * Returns a list of regular expressions which will be matched against each message code. If it matches, the message
   * will be included in the packed message file. If it doesn't match the message is skipped.
   * <p>
   * If the list is empty, all messages are included, unless they're explicitly excluded.
   * <p>
   * The property is evaluated lazily when the task executes, so regular expressions added after the task has been
   * configured are taken into account.
   *
   * @return  list property of regular expressions for message inclusion, never {@code null}
   *
   * @see #include(String...)
   * @see #getExcludeRegexFilters()
   */
  @Input
  public abstract ListProperty<@NotNull String> getIncludeRegexFilters();


  /**
   * Returns a list of regular expressions which will be matched against each message code. If it matches, the message
   * will be excluded from the packed message file. If it doesn't match the message is included.
   * <p>
   * The property is evaluated lazily when the task executes, so regular expressions added after the task has been
   * configured are taken into account.
   *
   * @return  list property of regular expressions for message exclusion, never {@code null}
   *
   * @see #exclude(String...)
   * @see #getIncludeRegexFilters()
   */
  @Input
  public abstract ListProperty<@NotNull String> getExcludeRegexFilters();


  /**
   * Property containing the strategy to use in case a duplicate message code or template name (with different message
   * definition) is found. The default strategy is {@link DuplicateStrategy#IGNORE_AND_WARN IGNORE_AND_WARN}.
   * <p>
   * This property accepts various formats:
   * <ul>
   *   <li>
   *     {@link DuplicateStrategy} enum value (e.g. {@link DuplicateStrategy#FAIL FAIL})
   *   </li>
   *   <li>
   *     Duplicate strategy string. The string is converted to uppercase, dashes are translated to underscores and the
   *     resulting strategy name is matched against {@link DuplicateStrategy} (e.g. {@code 'override-and-warn'}
   *     matches {@link DuplicateStrategy#OVERRIDE_AND_WARN OVERRIDE_AND_WARN})
   *   </li>
   * </ul>
   * <p>
   * A duplicate is either a message with an already known message code or a template with an already known template
   * name and a different message definition. This means that if the same message or template is encountered twice, it
   * is not considered a duplicate.
   *
   * @return  duplicate message strategy property, never {@code null}
   *
   * @see DuplicateStrategy
   * @see #getResolvedDuplicateStrategy()
   */
  @Internal
  public abstract Property<@NotNull Object> getDuplicateStrategy();


  /**
   * Returns the {@link #getDuplicateStrategy() duplicate strategy} converted into a {@link DuplicateStrategy} enum
   * constant.
   * <p>
   * This provider is the task input for the duplicate strategy. Equal strategies given in different formats (e.g.
   * {@code 'fail'}, {@code 'FAIL'} or {@link DuplicateStrategy#FAIL FAIL}) therefore result in the same input value.
   *
   * @return  provider for the resolved duplicate strategy, never {@code null}
   *
   * @throws InvalidUserDataException  on querying the provider, if the configured duplicate strategy is unknown
   *
   * @since 0.25.0
   */
  @Input
  public Provider<@NotNull DuplicateStrategy> getResolvedDuplicateStrategy() {
    return getDuplicateStrategy().map(MessageFormatMessagesExtension::toDuplicateStrategy);
  }


  /**
   * Includes messages that match the given regular expressions. The regular expressions are added to the
   * {@link #getIncludeRegexFilters() include filters}.
   *
   * @param regex  array of regular expressions, not {@code null}
   *
   * @see #getIncludeRegexFilters()
   */
  public void include(String... regex) {
    getIncludeRegexFilters().addAll(regex);
  }


  /**
   * Excludes messages that match the given regular expressions. The regular expressions are added to the
   * {@link #getExcludeRegexFilters() exclude filters}.
   *
   * @param regex  array of regular expressions, not {@code null}
   *
   * @see #getExcludeRegexFilters()
   */
  public void exclude(String... regex) {
    getExcludeRegexFilters().addAll(regex);
  }


  /**
   * Converts a configured duplicate strategy, which may be a {@link DuplicateStrategy} or its name, into an enum
   * constant.
   *
   * @param value  configured duplicate strategy, not {@code null}
   *
   * @return  duplicate strategy, never {@code null}
   *
   * @throws InvalidUserDataException  if {@code value} is not a known duplicate strategy
   */
  @Contract(pure = true)
  private static @NotNull DuplicateStrategy toDuplicateStrategy(@NotNull Object value)
  {
    if (value instanceof DuplicateStrategy duplicateStrategy)
      return duplicateStrategy;

    if (value instanceof GString)
      value = value.toString();

    if (value instanceof String string)
    {
      var ds = findEnumValue(string, DuplicateStrategy.class);
      if (ds.isPresent())
        return ds.get();
    }

    throw new InvalidUserDataException("Unknown duplicates strategy: " + value);
  }
}
