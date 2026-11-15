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

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property


/**
 * Project specific information for the {@code META-INF/NOTICE} file included in the jars.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 *
 * @see GenerateNotice
 */
abstract class NoticeExtension
{
  /**
   * Returns the year in which the project was started. It is the first year of the copyright notice and the inception
   * year of the published POM.
   *
   * @return  inception year, e.g. {@code 2019}
   */
  abstract Property<String> getInceptionYear()


  /**
   * Returns an optional file, whose content is appended to the notice, e.g. notices for bundled third party libraries.
   * The file is a template, which may refer to the {@linkplain #getAppendixProperties() appendix properties} using
   * {@code ${name}}.
   *
   * @return  appendix template file
   */
  abstract RegularFileProperty getAppendix()


  /**
   * Returns the properties available in the {@linkplain #getAppendix() appendix} template.
   *
   * @return  appendix template properties
   */
  abstract MapProperty<String,String> getAppendixProperties()
}
