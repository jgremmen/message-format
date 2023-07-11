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

import lombok.val;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.jetbrains.annotations.NotNull;

import static de.sayayi.plugin.gradle.message.DuplicateMsgStrategy.IGNORE_AND_WARN;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public class MessageFormatPlugin implements Plugin<Project>
{
  public static final String EXTENSION = "messageFormat";


  @Override
  public void apply(Project project)
  {
    // provide java base plugin (for main/java)
    val plugins = project.getPlugins();
    if (!plugins.hasPlugin(JavaBasePlugin.class))
      project.apply(objectConfiguration -> objectConfiguration.plugin(JavaBasePlugin.class));

    // create extension and set conventions
    val extensions = project.getExtensions();
    val messageFormatExtension = extensions.create(EXTENSION, MessageFormatExtension.class);

    messageFormatExtension.getPackFilename().convention("message.pack");
    messageFormatExtension.getCompress().convention(false);
    messageFormatExtension.getDuplicateMsgStrategy().convention(IGNORE_AND_WARN);
    messageFormatExtension.getValidateReferencedTemplates().convention(true);

    val mainJavaSourceSet = extensions
        .getByType(JavaPluginExtension.class)
        .getSourceSets()
        .getByName("main");

    // sources = {.build/classes/java/main/}**/*.class
    messageFormatExtension.sourceSet(mainJavaSourceSet);

    registerPackTask(project, messageFormatExtension, mainJavaSourceSet);
  }


  private void registerPackTask(@NotNull Project project,
                                @NotNull MessageFormatExtension extension,
                                @NotNull SourceSet mainSourceSet)
  {
    val tasks = project.getTasks();

    tasks.register("messageFormatPack", MessageFormatPackTask.class, packTask -> {
      packTask.setGroup("build");
      packTask.setDescription("Scans and packs message format definitions.");

      packTask.getCompress().convention(extension.getCompress());
      packTask.getDuplicateMsgStrategy().convention(extension.getDuplicateMsgStrategy());
      packTask.getPackFilename().convention(extension.getPackFilename());
      packTask.getSources().from(extension.getSources());
      packTask.getValidateReferencedTemplates()
          .convention(extension.getValidateReferencedTemplates());

      packTask.include(extension.getIncludeRegexFilter().toArray(new String[0]));
      packTask.exclude(extension.getExcludeRegexFilter().toArray(new String[0]));

      packTask.dependsOn(mainSourceSet.getOutput());
    });
  }
}
