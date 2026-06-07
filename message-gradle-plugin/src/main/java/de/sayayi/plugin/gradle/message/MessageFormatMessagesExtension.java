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

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


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
  private final List<String> includeRegexFilter = new ArrayList<>();
  private final List<String> excludeRegexFilter = new ArrayList<>();


  /**
   * Return a list of regular expressions which will be matched against each message code. If it matches, the message
   * will be included in the packed message file. If it doesn't match the message is skipped.
   * <p>
   * If the list is empty, all messages are included, unless they're explicitly excluded.
   *
   * @return  list of regular expressions for message inclusion, never {@code null}
   *
   * @see #getExcludeRegexFilters()
   */
  @Input
  public List<String> getIncludeRegexFilters() {
    return includeRegexFilter;
  }


  /**
   * Return a list of regular expressions which will be matched against each message code. If it matches, the message
   * will be excluded from the packed message file. If it doesn't match the message is included.
   *
   * @return  list of regular expressions for message exclusion, never {@code null}
   *
   * @see #getIncludeRegexFilters()
   */
  @Input
  public List<String> getExcludeRegexFilters() {
    return excludeRegexFilter;
  }


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
   */
  @Input
  public abstract Property<@NotNull Object> getDuplicateStrategy();


  /**
   * Include messages that match the given regular expressions.
   *
   * @param regex  array of regular expressions, not {@code null}
   *
   * @see #getIncludeRegexFilters()
   */
  public void include(String... regex) {
    includeRegexFilter.addAll(List.of(regex));
  }


  /**
   * Exclude messages that match the given regular expressions.
   *
   * @param regex  array of regular expressions, not {@code null}
   *
   * @see #getExcludeRegexFilters()
   */
  public void exclude(String... regex) {
    excludeRegexFilter.addAll(List.of(regex));
  }
}
