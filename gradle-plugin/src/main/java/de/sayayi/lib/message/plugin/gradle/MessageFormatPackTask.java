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

import de.sayayi.lib.message.MessageSupport.ConfigurableMessageSupport;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.adopter.AsmClassPathScannerAdopter;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import lombok.val;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.newOutputStream;


/**
 * @author Jeroen Gremmen
 */
public abstract class MessageFormatPackTask extends DefaultTask
{
  private final List<String> includeRegexFilter = new ArrayList<>();
  private final List<String> excludeRegexFilter = new ArrayList<>();

  @Input
  public abstract ConfigurableFileCollection getSources();

  @Input
  public abstract Property<DuplicatesStrategy> getDuplicatesStrategy();

  @Input
  public abstract Property<Boolean> getCompress();

  @OutputFile
  public abstract RegularFileProperty getPackFile();


  public void include(String... regex) {
    includeRegexFilter.addAll(Arrays.asList(regex));
  }


  public void exclude(String... regex) {
    excludeRegexFilter.addAll(Arrays.asList(regex));
  }


  @TaskAction
  public void pack()
  {
    val messageSupport = MessageSupportFactory
        .create(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE);

    configureDuplicatesStrategy(messageSupport);

    try {
      val adopter = new SourceSetScannerAdopter(messageSupport);

      for(val classFile: getSources().getFiles())
      {
        getLogger().debug("Scanning " + classFile.getAbsolutePath());
        adopter.parseClass(classFile);
      }
    } catch(IOException ex) {
      throw new GradleException("Failed to scan messages", ex);
    }

    val packFile = getPackFile().getAsFile().get();
    getProject().mkdir(packFile.getParentFile());

    try(val packOutputStream = newOutputStream(packFile.toPath())) {
      messageSupport.exportMessages(
          packOutputStream,
          getCompress().getOrElse(false),
          this::messageCodeFilter);
    } catch(IOException ex) {
      throw new GradleException("Failed to write packed messages", ex);
    }
  }


  private boolean messageCodeFilter(@NotNull String code)
  {
    boolean match = true;

    if (!includeRegexFilter.isEmpty())
    {
      match = false;

      for(val regex: includeRegexFilter)
        if (code.matches(regex))
        {
          match = true;
          break;
        }
    }

    for(val regex: excludeRegexFilter)
      if (code.matches(regex))
      {
        match = false;
        break;
      }

    return match;
  }


  private void configureDuplicatesStrategy(@NotNull ConfigurableMessageSupport messageSupport)
  {
    switch(getDuplicatesStrategy().get())
    {
      case FAIL:
        configureDuplicateFailStrategy(messageSupport);
        break;

      case OVERRIDE:
        configureDuplicateOverrideStrategy(messageSupport, false);
        break;

      case OVERRIDE_AND_WARN:
        configureDuplicateOverrideStrategy(messageSupport, true);
        break;

      case IGNORE:
        configureDuplicateIgnoreStrategy(messageSupport, false);
        break;

      case IGNORE_AND_WARN:
        configureDuplicateIgnoreStrategy(messageSupport, true);
        break;
    }
  }


  private void configureDuplicateFailStrategy(@NotNull ConfigurableMessageSupport messageSupport)
  {
    val accessor = messageSupport.getAccessor();

    messageSupport.setMessageHandler(code -> {
      if (accessor.hasMessageWithCode(code))
        throw new GradleException("Duplicate message code '" + code + "'");

      return true;
    });

    messageSupport.setTemplateHandler(name -> {
      if (accessor.hasTemplateWithName(name))
        throw new GradleException("Duplicate template name '" + name + "'");

      return true;
    });
  }


  private void configureDuplicateIgnoreStrategy(@NotNull ConfigurableMessageSupport messageSupport,
                                                boolean warn)
  {
    val accessor = messageSupport.getAccessor();

    messageSupport.setMessageHandler(code -> {
      if (accessor.hasMessageWithCode(code))
      {
        if (warn)
          getLogger().warn("Duplicate message code '" + code + "'");

        return false;
      }

      return true;
    });

    messageSupport.setTemplateHandler(name -> {
      if (accessor.hasTemplateWithName(name))
      {
        if (warn)
          getLogger().warn("Duplicate template name '" + name + "'");

        return false;
      }

      return true;
    });
  }


  private void configureDuplicateOverrideStrategy(
      @NotNull ConfigurableMessageSupport messageSupport, boolean warn)
  {
    val accessor = messageSupport.getAccessor();

    messageSupport.setMessageHandler(code -> {
      if (warn && accessor.hasMessageWithCode(code))
        getLogger().warn("Duplicate message code '" + code + "'");

      return true;
    });

    messageSupport.setTemplateHandler(name -> {
      if (warn && accessor.hasTemplateWithName(name))
        getLogger().warn("Duplicate template name '" + name + "'");

      return true;
    });
  }




  private static final class SourceSetScannerAdopter extends AsmClassPathScannerAdopter
  {
    public SourceSetScannerAdopter(@NotNull ConfigurableMessageSupport configurableMessageSupport) {
      super(configurableMessageSupport, Collections.emptySet(), null);
    }


    void parseClass(@NotNull File classFile) throws IOException
    {
      try(val classInputStream = newInputStream(classFile.toPath())) {
        parseClass(classInputStream);
      }
    }
  }
}
