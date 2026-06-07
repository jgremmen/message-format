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
 * Nested extension for configuring template validation and filtering.
 * <p>
 * This extension is accessible via the {@code templates} block inside the {@code messageFormat} extension:
 * <pre>
 *   messageFormat {
 *     templates {
 *       validateReferences = true
 *       ignore 'tpl-.*'
 *     }
 *   }
 * </pre>
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 *
 * @see MessageFormatExtension
 */
public abstract class MessageFormatTemplatesExtension
{
  private final List<String> ignoreRegexFilter = new ArrayList<>();


  /**
   * Return a list of regular expressions which will be matched against each template name. If it matches, the
   * template will be ignored during validation of referenced templates.
   *
   * @return  list of regular expressions for template name filtering, never {@code null}
   *
   * @see #ignore(String...)
   */
  @Input
  public List<String> getIgnoreRegexFilters() {
    return ignoreRegexFilter;
  }


  /**
   * Property containing a boolean stating whether to validate referenced templates. The default value resolves to
   * {@code true}.
   * <p>
   * If the property resolves to {@code true} the task will check whether all referenced templates (including nested
   * templates) are available and included in the packed message file.
   * <p>
   * If the property resolves to {@code false} no checks are performed. This may lead to a situation where a message
   * cannot be formatted if the referenced template is missing from the message support.
   *
   * @return  validate referenced templates property, never {@code null}
   */
  @Input
  public abstract Property<@NotNull Boolean> getValidateReferences();


  /**
   * Ignore templates whose name matches the given regular expressions during template validation.
   *
   * @param regex  array of regular expressions matching template names, not {@code null}
   *
   * @see #getIgnoreRegexFilters()
   */
  public void ignore(String... regex) {
    ignoreRegexFilter.addAll(List.of(regex));
  }
}
