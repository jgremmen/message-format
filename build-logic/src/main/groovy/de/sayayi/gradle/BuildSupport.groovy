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
package de.sayayi.gradle

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider


/**
 * Shared build helpers.
 *
 * @author Jeroen Gremmen
 * @since 0.24.1
 */
final class BuildSupport
{
  /** Prevents instantiation of this utility class. */
  private BuildSupport() {
  }


  /**
   * Returns whether the current project version is a snapshot build.
   *
   * @param  project the project whose version is checked
   *
   * @return {@code true} if the version ends with {@code -SNAPSHOT}
   */
  static boolean isSnapshot(Project project) {
    (project.version as String).endsWith('-SNAPSHOT')
  }


  /**
   * Removes the trailing {@code -SNAPSHOT} suffix from the project version.
   *
   * @param  project the project whose version should be normalized
   *
   * @return  the release version without the snapshot suffix, or the original version for non-snapshot builds
   */
  static String releaseVersion(Project project) {
    def version = project.version as String
    isSnapshot(project) ? version[0..-10] : version
  }


  /**
   * Registers the standard release task for a project.
   * <p>
   * The generated task name is derived from the project name and publishes the project
   * local repository artifacts as part of the release workflow.
   *
   * @param  project the project that owns the release task
   *
   * @return  the registered release task
   */
  static TaskProvider<Task> registerReleaseTask(Project project) {
    def projectName = project.name
    def taskName = projectName
        .split('-')
        .collect { it.capitalize() }
        .join('')
        .with { "release${it}Artifacts" }
    def snapshot = isSnapshot(project)

    project.tasks.register(taskName) {
      group = 'release'
      description = "Release ${projectName} ${snapshot ? 'snapshot ' : ''}artifacts to Maven Central Repository."

      inputs.property('snapshot', snapshot)

      dependsOn 'publishToProjectLocalRepository'
    }
  }


  /**
   * Applies an action to the release task when the project is a snapshot build.
   *
   * @param  project the project whose release task is configured
   * @param  action the action to apply to the release task
   */
  static void configureSnapshotRelease(Project project, Closure<? extends Task> action) {
    if (isSnapshot(project))
      project.extensions.extraProperties.get('releaseTask').configure(action)
  }
}
