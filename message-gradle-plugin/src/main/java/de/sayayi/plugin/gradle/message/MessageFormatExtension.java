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

import org.gradle.api.Action;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.SourceSet;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;


/**
 * Gradle extension {@code messageFormat} for configuring the {@link MessageFormatPackTask messageFormatPack} task.
 * <p>
 * This extension allows configuring the pack filename, compression, template validation, source sets to scan and
 * message inclusion/exclusion filters via the nested {@code messages} block.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 *
 * @see MessageFormatPlugin
 * @see MessageFormatPackTask
 */
public abstract class MessageFormatExtension
{
  private final MessageFormatMessagesExtension messages;
  private final MessageFormatTemplatesExtension templates;


  /**
   * Creates the extension and instantiates the nested {@code messages} extension.
   *
   * @param objectFactory  Gradle object factory for creating managed instances
   */
  @Inject
  public MessageFormatExtension(@NotNull ObjectFactory objectFactory)
  {
    messages = objectFactory.newInstance(MessageFormatMessagesExtension.class);
    templates = objectFactory.newInstance(MessageFormatTemplatesExtension.class);
  }


  /**
   * Returns the nested messages extension for configuring message inclusion/exclusion filters and
   * duplicate message handling strategy.
   *
   * @return  nested messages extension, never {@code null}
   *
   * @since 0.24.0
   */
  public MessageFormatMessagesExtension getMessages() {
    return messages;
  }


  /**
   * Configures the nested {@code messages} block.
   * <p>
   * Example usage:
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
   * @param action  configuration action for the messages extension, not {@code null}
   *
   * @since 0.24.0
   */
  public void messages(@NotNull Action<? super MessageFormatMessagesExtension> action) {
    action.execute(messages);
  }


  /**
   * Returns the nested templates extension for configuring template validation and filtering.
   *
   * @return  nested templates extension, never {@code null}
   *
   * @since 0.24.0
   */
  @Nested
  public MessageFormatTemplatesExtension getTemplates() {
    return templates;
  }


  /**
   * Configures the nested {@code templates} block.
   * <p>
   * Example usage:
   * <pre>
   *   messageFormat {
   *     templates {
   *       validateReferences = true
   *       ignore 'tpl-.*'
   *     }
   *   }
   * </pre>
   *
   * @param action  configuration action for the templates extension, not {@code null}
   *
   * @since 0.24.0
   */
  public void templates(@NotNull Action<? super MessageFormatTemplatesExtension> action) {
    action.execute(templates);
  }


  /**
   * Return the pack filename property. The default value is {@code messages.mfp}.
   *
   * @return  pack filename property, never {@code null}
   *
   * @since 0.8.0
   */
  public abstract Property<@NotNull String> getPackFilename();


  /**
   * Compress property stating whether the message pack must be compressed or not. The default value is {@code false}.
   *
   * @return  compress property, never {@code null}
   */
  public abstract Property<@NotNull Boolean> getCompress();


  /**
   * Returns a collection of source files to scan for message and template annotations.
   * <p>
   * There's no restriction on what kind of files are in the collection. Only class ({@code *.class}) files will be
   * used for annotation scanning.
   * <p>
   * The default value is the output of the {@code main/java} source set, which contains all compiled java classes.
   *
   * @return  class files to be scanned for messages and templates, never {@code null}
   *
   * @see #sourceSet(SourceSet)
   */
  public abstract ConfigurableFileCollection getSources();


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
