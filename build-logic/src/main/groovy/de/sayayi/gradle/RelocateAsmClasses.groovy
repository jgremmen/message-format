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

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction


/**
 * Writes the classes of the ASM library jars, relocated to another package, to an output directory.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
abstract class RelocateAsmClasses extends DefaultTask
{
  /**
   * Returns the ASM library jars to relocate.
   *
   * @return  the ASM library jars
   */
  @Classpath
  abstract ConfigurableFileCollection getAsmJars()


  /**
   * Returns the package the ASM classes are relocated to.
   *
   * @return  the relocated package, e.g. {@code com.example.asm}
   */
  @Input
  abstract Property<String> getRelocatedPackage()


  /**
   * Returns the directory the relocated classes are written to.
   *
   * @return  the output directory
   */
  @OutputDirectory
  abstract DirectoryProperty getOutputDirectory()


  /** Relocates the ASM classes. */
  @TaskAction
  void relocate()
  {
    def outputDir = outputDirectory.get().asFile

    outputDir.deleteDir()
    outputDir.mkdirs()

    logger.info("Relocating ASM library classes to ${outputDir.absolutePath}...")

    def relocation = new AsmRelocation(relocatedPackage.get())

    asmJars.each { jar -> relocation.relocateJar(jar, outputDir) }
  }
}
