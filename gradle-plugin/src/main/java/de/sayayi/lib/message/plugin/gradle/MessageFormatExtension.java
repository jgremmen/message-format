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

import lombok.Getter;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * @author Jeroen Gremmen
 */
public abstract class MessageFormatExtension
{
  @Getter private final List<String> includeRegexFilter = new ArrayList<>();
  @Getter private final List<String> excludeRegexFilter = new ArrayList<>();


  public abstract Property<String> getPackFilename();


  public abstract Property<Boolean> getCompress();


  public abstract ConfigurableFileCollection getSources();


  public abstract Property<DuplicatesStrategy> getDuplicatesStrategy();


  public abstract DirectoryProperty getDestinationDirectory();


  public void include(String... regex) {
    includeRegexFilter.addAll(Arrays.asList(regex));
  }


  public void exclude(String... regex) {
    excludeRegexFilter.addAll(Arrays.asList(regex));
  }
}
