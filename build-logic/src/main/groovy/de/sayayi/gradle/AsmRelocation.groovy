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

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.ModuleVisitor
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

import static groovy.io.FileType.FILES
import static org.objectweb.asm.Opcodes.ASM9


/**
 * Relocates the ASM library into another package, both the ASM classes themselves and the references to them in the
 * project classes.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
final class AsmRelocation
{
  /** Internal name prefix of the ASM packages. */
  private static final String ASM_PACKAGE_PREFIX = 'org/objectweb/asm'

  /** Module name of the ASM library. */
  private static final String ASM_MODULE = 'org.objectweb.asm'

  /** Remapper moving ASM classes to the relocated package. */
  private final Remapper remapper


  /**
   * Creates an ASM relocation.
   *
   * @param  relocatedPackage the package the ASM classes are relocated to, e.g. {@code com.example.asm}
   */
  AsmRelocation(String relocatedPackage) {
    remapper = new RelocatingRemapper(relocatedPackage.replace('.', '/'))
  }


  /**
   * Rewrites all class files in the given directory, so they reference the relocated ASM classes. The requirement of
   * the ASM module is removed from {@code module-info.class}.
   *
   * @param  classesDirectory the directory containing the project class files
   */
  void relocateReferences(File classesDirectory)
  {
    classesDirectory.eachFileRecurse(FILES) { classFile ->
      if (classFile.name == 'module-info.class')
      {
        def writer = new ClassWriter(0)

        new ClassReader(classFile.bytes).accept(new ClassVisitor(ASM9, writer) {
          @Override
          ModuleVisitor visitModule(String name, int access, String version)
          {
            return new ModuleVisitor(ASM9, super.visitModule(name, access, version)) {
              @Override
              void visitRequire(String module, int reqAccess, String reqVersion)
              {
                if (module != ASM_MODULE)
                  super.visitRequire(module, reqAccess, reqVersion)
              }
            }
          }
        }, 0)

        classFile.bytes = writer.toByteArray()
      }
      else if (classFile.name.endsWith('.class'))
      {
        def writer = new ClassWriter(0)

        new ClassReader(classFile.bytes).accept(new ClassRemapper(writer, remapper), 0)

        classFile.bytes = writer.toByteArray()
      }
    }
  }


  /**
   * Writes the relocated classes of the given ASM library jar to the output directory. {@code module-info.class} is
   * skipped.
   *
   * @param  jar             the ASM library jar
   * @param  outputDirectory the directory to write the relocated classes to
   */
  void relocateJar(File jar, File outputDirectory)
  {
    new ZipFile(jar).withCloseable { zip -> zip
        .entries()
        .findAll { !it.directory && it.name.endsWith('.class') && it.name != 'module-info.class' }
        .each { ZipEntry entry ->
          def writer = new ClassWriter(0)

          new ClassReader(zip.getInputStream(entry)).accept(new ClassRemapper(writer, remapper), 0)

          def outFile = new File(outputDirectory, remapper.map(entry.name.replace('.class', '')) + '.class')

          outFile.parentFile.mkdirs()
          outFile.bytes = writer.toByteArray()
        }
    }
  }




  /**
   * Remapper moving the ASM packages to the relocated package.
   */
  private static final class RelocatingRemapper extends Remapper
  {
    /** Internal name prefix of the relocated package. */
    private final String relocatedPackagePrefix


    RelocatingRemapper(String relocatedPackagePrefix)
    {
      //noinspection deprecation
      super(ASM9)

      this.relocatedPackagePrefix = relocatedPackagePrefix
    }


    @Override
    String map(String internalName)
    {
      return internalName.startsWith(ASM_PACKAGE_PREFIX)
          ? relocatedPackagePrefix + internalName.substring(ASM_PACKAGE_PREFIX.length())
          : internalName
    }
  }
}
