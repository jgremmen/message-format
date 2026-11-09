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

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider


/**
 * Task action rewriting the compiled project classes, so they reference the relocated ASM classes. It is added to the
 * compile task with {@code doLast}.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
final class RelocateAsmReferences implements Action<Task>
{
  /** Directory containing the compiled project classes. */
  private final Provider<Directory> classesDirectory

  /** Package the ASM classes are relocated to. */
  private final String relocatedPackage


  /**
   * Creates the task action.
   *
   * @param  classesDirectory the directory containing the compiled project classes
   * @param  relocatedPackage the package the ASM classes are relocated to, e.g. {@code com.example.asm}
   */
  RelocateAsmReferences(Provider<Directory> classesDirectory, String relocatedPackage)
  {
    this.classesDirectory = classesDirectory
    this.relocatedPackage = relocatedPackage
  }


  @Override
  void execute(Task task)
  {
    task.logger.info('Rewriting project class references to relocated ASM classes...')

    new AsmRelocation(relocatedPackage).relocateReferences(classesDirectory.get().asFile)
  }
}
