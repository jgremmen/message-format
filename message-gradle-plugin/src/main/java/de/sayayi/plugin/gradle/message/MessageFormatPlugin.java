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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.jetbrains.annotations.NotNull;

import static de.sayayi.plugin.gradle.message.DuplicateMsgStrategy.IGNORE_AND_WARN;
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;


/**
 * Gradle plugin that provides the {@code messageFormat} extension and registers the
 * {@link MessageFormatPackTask messageFormatPack} task for scanning and packing message format definitions found in
 * compiled classes.
 * <p>
 * The plugin automatically applies the {@link JavaBasePlugin} if not already present and configures the
 * {@link MessageFormatExtension} with sensible defaults. The {@code main} source set is used as the default source for
 * class scanning.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 *
 * @see MessageFormatExtension
 * @see MessageFormatPackTask
 */
@SuppressWarnings("unused")
public class MessageFormatPlugin implements Plugin<@NotNull Project>
{
  /** Plugin extension name: {@value} */
  public static final String EXTENSION = "messageFormat";


  /**
   * Applies the plugin to the given {@code project} by registering the {@link MessageFormatExtension messageFormat}
   * extension and the {@link MessageFormatPackTask messageFormatPack} task.
   *
   * @param project  the Gradle project to apply this plugin to, never {@code null}
   */
  @Override
  public void apply(@NotNull Project project)
  {
    // provide java base plugin (for main/java)
    final var plugins = project.getPlugins();
    if (!plugins.hasPlugin(JavaBasePlugin.class))
      project.apply(objectConfiguration -> objectConfiguration.plugin(JavaBasePlugin.class));

    // create extension and set conventions
    final var extensions = project.getExtensions();
    final var messageFormatExtension = extensions.create(EXTENSION, MessageFormatExtension.class);

    messageFormatExtension.getPackFilename().convention("messages.mfp");
    messageFormatExtension.getCompress().convention(false);
    messageFormatExtension.getDuplicateMsgStrategy().convention(IGNORE_AND_WARN);
    messageFormatExtension.getValidateReferencedTemplates().convention(true);

    final var mainJavaSourceSet = extensions
        .getByType(JavaPluginExtension.class)
        .getSourceSets()
        .getByName(MAIN_SOURCE_SET_NAME);

    // sources = {.build/classes/java/main/}**/*.class
    messageFormatExtension.sourceSet(mainJavaSourceSet);

    registerPackTask(project, messageFormatExtension, mainJavaSourceSet);
  }


  /**
   * Registers the {@code messageFormatPack} task and wires it to the extension properties
   * and the main source set output.
   *
   * @param project        the Gradle project, never {@code null}
   * @param extension      the message format extension providing the configuration, never {@code null}
   * @param mainSourceSet  the main source set whose output the task depends on, never {@code null}
   */
  private void registerPackTask(@NotNull Project project,
                                @NotNull MessageFormatExtension extension,
                                @NotNull SourceSet mainSourceSet)
  {
    final var tasks = project.getTasks();
    final var layout = project.getLayout();

    tasks.register("messageFormatPack", MessageFormatPackTask.class, packTask -> {
      packTask.setGroup("build");
      packTask.setDescription("Scans and packs message format definitions.");

      // sources
      packTask.getSources().from(extension.getSources());

      // pack file
      packTask.getDestinationDir().convention(layout.getBuildDirectory().dir(packTask.getName()));
      packTask.getPackFilename().convention(extension.getPackFilename());

      // settings
      packTask.getCompress().convention(extension.getCompress());
      packTask.getDuplicateMsgStrategy().convention(extension.getDuplicateMsgStrategy());
      packTask.getValidateReferencedTemplates().convention(extension.getValidateReferencedTemplates());

      packTask.include(extension.getIncludeRegexFilters().toArray(String[]::new));
      packTask.exclude(extension.getExcludeRegexFilters().toArray(String[]::new));

      packTask.dependsOn(mainSourceSet.getOutput());
    });
  }
}
