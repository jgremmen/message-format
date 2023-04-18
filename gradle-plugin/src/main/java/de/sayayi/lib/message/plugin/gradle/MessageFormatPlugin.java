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
import org.gradle.api.plugins.JavaPlugin;


/**
 * @author Jeroen Gremmen
 */
public class MessageFormatPlugin implements Plugin<Project>
{
  public static final String MESSAGE_FORMAT_EXTENSION = "messageFormat";


  @Override
  public void apply(Project project)
  {
    // provide java plugin
    if (!project.getPlugins().hasPlugin(JavaPlugin.class))
      project.apply(objectConfiguration -> objectConfiguration.plugin(JavaPlugin.class));

    val messageFormatExtension = project.getExtensions()
        .create(MESSAGE_FORMAT_EXTENSION, MessageFormatExtension.class);
  }
}
