/*
 * Copyright 2023 Jeroen Gremmen
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

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.SourceSet;

import java.util.ArrayList;
import java.util.List;


/**
 * Gradle extension {@code messageFormat}
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public abstract class MessageFormatExtension
{
  private final List<String> includeRegexFilter = new ArrayList<>();
  private final List<String> excludeRegexFilter = new ArrayList<>();


  /**
   * Return a list of regular expressions which will be matched against each message code.
   * If it matches, the message will be included in the packed message file. If it doesn't match
   * the message is skipped.
   * <p>
   * If the list is empty, all messages are included, unless they're explicitly excluded.
   *
   * @return  list of regular expressions for message inclusion, never {@code null}
   *
   * @see #getExcludeRegexFilters()
   *
   * @since 0.8.0 (renamed in 0.9.1)
   */
  public List<String> getIncludeRegexFilters() {
    return includeRegexFilter;
  }


  /**
   * Return a list of regular expressions which will be matched against each message code.
   * If it matches, the message will be excluded from the packed message file. If it doesn't match
   * the message is included.
   *
   * @return  list of regular expressions for message exclusion, never {@code null}
   *
   * @see #getIncludeRegexFilters()
   *
   * @since 0.8.0 (renamed in 0.9.1)
   */
  public List<String> getExcludeRegexFilters() {
    return excludeRegexFilter;
  }


  /**
   * Return the pack filename property. The default value is {@code message.pack}
   *
   * @return  pack filename property, never {@code null}
   */
  public abstract Property<String> getPackFilename();


  /**
   * Compress property stating whether the message pack must be compressed or not.
   * The default value is {@code false}.
   *
   * @return  compress property, never {@code null}
   */
  public abstract Property<Boolean> getCompress();


  /**
   * Returns a collection of source files to scan for message and template annotations.
   * <p>
   * There's no restriction on what kind of files are in the collection. Only class
   * ({@code *.class}) files will be used for annotation scanning.
   * <p>
   * The default value is the output of the {@code main/java} source set, which contains all
   * compiled java classes.
   *
   * @return  class files to be scanned for messages and templates, never {@code null}
   *
   * @see #sourceSet(SourceSet)
   */
  public abstract ConfigurableFileCollection getSources();


  /**
   * Property containing the strategy to use in case a duplicate message code or template name
   * (with different message definition) is found. The default strategy is
   * {@link DuplicateMsgStrategy#IGNORE_AND_WARN IGNORE_AND_WARN}.
   * <p>
   * This property accepts various formats:
   * <ul>
   *   <li>
   *     {@link DuplicateMsgStrategy} enum value (e.g. {@link DuplicateMsgStrategy#FAIL FAIL})
   *   </li>
   *   <li>
   *     Duplicate strategy string. The string is converted to uppercase, dashes are translated to
   *     underscores and the resulting strategy name is matched against
   *     {@link DuplicateMsgStrategy} (e.g. {@code 'override-and-warn'} matches
   *     {@link DuplicateMsgStrategy#OVERRIDE_AND_WARN OVERRIDE_AND_WARN})
   *   </li>
   * </ul>
   * <p>
   * A duplicate is either a message with an already known message code or a template with
   * an already known template name and a different message definition. This means that if the
   * same message or template is encountered twice, it is not considered a duplicate.
   *
   * @return  duplicate message strategy property, never {@code null}
   *
   * @see DuplicateMsgStrategy
   */
  public abstract Property<Object> getDuplicateMsgStrategy();


  /**
   * Property containing a boolean stating whether to validate referenced templates. The
   * default value resolves to {@code true}.
   * <p>
   * If the property resolves to {@code true} the task will check whether all referenced
   * templates (including nested templates) are available and included in the packed message file.
   * <p>
   * If the property resolves to {@code false} no checks are performed. This may lead to a
   * situation where a message cannot be formatted if the referenced template is missing from
   * the message support.
   *
   * @return  validate referenced templates property, never {@code null}
   */
  public abstract Property<Boolean> getValidateReferencedTemplates();


  /**
   * Include messages that match the given regular expressions.
   *
   * @param regex  array of regular expressions, not {@code null}
   */
  public void include(String... regex) {
    includeRegexFilter.addAll(List.of(regex));
  }


  /**
   * Exclude messages that match the given regular expressions.
   *
   * @param regex  array of regular expressions, not {@code null}
   */
  public void exclude(String... regex) {
    excludeRegexFilter.addAll(List.of(regex));
  }


  /**
   * Add all outputs for the given {@code sourceSet} to the collection of sources.
   *
   * @param sourceSet  source set to include in message/template scanning
   *
   * @see #getSources()
   */
  public void sourceSet(SourceSet sourceSet) {
    getSources().from(sourceSet.getOutput());
  }
}
