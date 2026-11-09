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

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.process.CommandLineArgumentProvider


/**
 * Provides a {@code -javaagent} JVM argument for each agent jar.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
abstract class JavaAgentArgumentProvider implements CommandLineArgumentProvider
{
  /**
   * Returns the java agent jars.
   *
   * @return  the java agent jars
   */
  @Classpath
  abstract ConfigurableFileCollection getAgentJars()


  @Override
  Iterable<String> asArguments() {
    agentJars.files.collect { "-javaagent:${it.absolutePath}".toString() }
  }
}
