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
package de.sayayi.lib.message.plugin.gradle;

import lombok.val;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlatformPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.jetbrains.annotations.NotNull;

import static de.sayayi.lib.message.plugin.gradle.DuplicatesStrategy.IGNORE_AND_WARN;
import static java.lang.Boolean.FALSE;


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
    // provide java plugin
    val plugins = project.getPlugins();
    if (!plugins.hasPlugin(JavaBasePlugin.class) && !plugins.hasPlugin(JavaPlatformPlugin.class))
      project.apply(objectConfiguration -> objectConfiguration.plugin(JavaBasePlugin.class));

    val extensions = project.getExtensions();
    val messageFormatExtension = extensions.create(EXTENSION, MessageFormatExtension.class);

    messageFormatExtension.getPackFilename().convention("message.pack");
    messageFormatExtension.getCompress().convention(FALSE);
    messageFormatExtension.getDuplicatesStrategy().convention(IGNORE_AND_WARN);

    val mainSourceSet = extensions.getByType(JavaPluginExtension.class)
        .getSourceSets().getByName("main");

    messageFormatExtension.getDestinationDirectory().convention(
        mainSourceSet.getResources().getDestinationDirectory().dir("META-INF"));
    messageFormatExtension.getSources().from(mainSourceSet.getOutput());

    registerPackTask(project, messageFormatExtension, mainSourceSet);
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
      packTask.getDuplicatesStrategy().convention(extension.getDuplicatesStrategy());
      packTask.getSources().from(extension.getSources());
      packTask.getPackFile().convention(project.provider(() ->
          extension.getDestinationDirectory().file(extension.getPackFilename()).get()));
      packTask.include(extension.getIncludeRegexFilter().toArray(new String[0]));
      packTask.exclude(extension.getExcludeRegexFilter().toArray(new String[0]));

      packTask.dependsOn(mainSourceSet.getOutput());
    });
  }
}
