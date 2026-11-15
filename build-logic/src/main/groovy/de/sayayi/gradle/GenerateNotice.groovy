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

import groovy.text.SimpleTemplateEngine
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

import static org.gradle.api.tasks.PathSensitivity.NONE


/**
 * Generates the {@code META-INF/NOTICE} file for the jars of a project. The notice consists of the product name and the
 * copyright notice, followed by an optional, project specific appendix.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 *
 * @see NoticeExtension
 */
abstract class GenerateNotice extends DefaultTask
{
  /** Holder of the copyright. */
  private static final String COPYRIGHT_HOLDER = 'Jeroen Gremmen'


  /**
   * Returns the product name, which is the first line of the notice.
   *
   * @return  product name
   */
  @Input
  abstract Property<String> getProductName()


  /**
   * Returns the year in which the project was started.
   *
   * @return  inception year
   */
  @Input
  abstract Property<String> getInceptionYear()


  /**
   * Returns the year of the current version, which is the last year of the copyright notice.
   *
   * @return  copyright year
   */
  @Input
  abstract Property<String> getCopyrightYear()


  /**
   * Returns the optional appendix template, see {@link NoticeExtension#getAppendix()}.
   *
   * @return  appendix template file
   */
  @InputFile
  @Optional
  @PathSensitive(NONE)
  abstract RegularFileProperty getAppendix()


  /**
   * Returns the properties available in the appendix template.
   *
   * @return  appendix template properties
   */
  @Input
  abstract MapProperty<String,String> getAppendixProperties()


  /**
   * Returns the generated notice file.
   *
   * @return  notice file
   */
  @OutputFile
  abstract RegularFileProperty getNoticeFile()


  /** Writes the notice file. */
  @TaskAction
  void generate()
  {
    def firstYear = inceptionYear.get()
    def lastYear = copyrightYear.get()
    def notice = new StringBuilder()
        .append(productName.get()).append('\n')
        .append("Copyright ${firstYear == lastYear ? firstYear : firstYear + '-' + lastYear} ${COPYRIGHT_HOLDER}\n")

    if (appendix.present)
    {
      def template = appendix.get().asFile.getText('UTF-8')

      // the template engine modifies the binding, so it requires a mutable map
      notice
          .append('\n')
          .append(new SimpleTemplateEngine()
              .createTemplate(template)
              .make(new HashMap<>(appendixProperties.get()))
              .toString())
    }

    noticeFile.get().asFile.setText(notice.toString(), 'UTF-8')
  }
}
