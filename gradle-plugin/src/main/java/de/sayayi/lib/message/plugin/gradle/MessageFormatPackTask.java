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

import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupport.ConfigurableMessageSupport;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.adopter.AsmAnnotationAdopter;
import de.sayayi.lib.message.exception.DuplicateMessageException;
import de.sayayi.lib.message.exception.DuplicateTemplateException;
import de.sayayi.lib.message.formatter.GenericFormatterService;
import lombok.val;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.util.PatternFilterable;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.plugin.gradle.DuplicatesStrategy.IGNORE_AND_WARN;
import static java.nio.file.Files.newOutputStream;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public abstract class MessageFormatPackTask extends DefaultTask
{
  private static final Action<PatternFilterable> CLASS_FILES =
      pf -> pf.include("**/*.class");

  private final List<String> includeRegexFilters = new ArrayList<>();
  private final List<String> excludeRegexFilters = new ArrayList<>();


  public MessageFormatPackTask()
  {
    getCompress().convention(false);
    getDuplicatesStrategy().convention(IGNORE_AND_WARN);
    getValidateReferencedTemplates().convention(true);
  }


  @Input
  public List<String> getIncludeRegexFilters() {
    return unmodifiableList(includeRegexFilters);
  }


  @Input
  public List<String> getExcludeRegexFilters() {
    return unmodifiableList(excludeRegexFilters);
  }


  @InputFiles
  public abstract ConfigurableFileCollection getSources();


  @Input
  public abstract Property<DuplicatesStrategy> getDuplicatesStrategy();


  @Input
  public abstract Property<Boolean> getValidateReferencedTemplates();


  @Input
  public abstract Property<Boolean> getCompress();


  @OutputFile
  public abstract RegularFileProperty getPackFile();


  public void include(String... regex) {
    includeRegexFilters.addAll(asList(regex));
  }


  public void exclude(String... regex) {
    excludeRegexFilters.addAll(asList(regex));
  }


  @TaskAction
  public void pack()
  {
    val messageSupport = MessageSupportFactory
        .create(new GenericFormatterService(), NO_CACHE_INSTANCE);

    configureDuplicatesStrategy(messageSupport);

    pack_scanMessages(messageSupport);
    pack_validateTemplates(messageSupport);
    pack_write(messageSupport);
  }


  private void pack_scanMessages(@NotNull ConfigurableMessageSupport messageSupport)
  {
    val logger = getLogger();

    logger.info("Scanning classes for messages and templates");

    try {
      val adopter = new AsmAnnotationAdopter(messageSupport);

      for (val classFile : getSources().getAsFileTree().matching(CLASS_FILES).getFiles()) {
        logger.debug("Scanning " + classFile.getAbsolutePath());
        adopter.adopt(classFile);
      }
    } catch(DuplicateMessageException | DuplicateTemplateException ex) {
      logger.error(ex.getLocalizedMessage(), ex);
      throw new GradleException("Failed to scan messages");
    } catch(Exception ex) {
      throw new GradleException("Failed to scan messages", ex);
    }
  }


  private void pack_validateTemplates(@NotNull MessageSupport messageSupport)
  {
    if (getValidateReferencedTemplates().get())
    {
      getLogger().debug("Validating referenced templates");

      val missingTemplateNames = new ArrayList<>(messageSupport.getAccessor()
          .findMissingTemplates(this::messageCodeFilter));
      val count = missingTemplateNames.size();

      switch(count)
      {
        case 0:
          break;

        case 1:
          throw new GradleException("Missing message template: " + missingTemplateNames.get(0));

        default:
          throw new GradleException("Missing message templates: " +
              String.join(", ", missingTemplateNames.subList(0, count - 1)) + " and " +
              missingTemplateNames.get(count - 1));
      }
    }
  }


  private void pack_write(@NotNull MessageSupport messageSupport)
  {
    val packFile = getPackFile().getAsFile().get();
    getLogger().debug("Writing message pack: " + packFile.getAbsolutePath());

    // create parent directory
    getProject().mkdir(packFile.getParentFile());

    try(val packOutputStream = newOutputStream(packFile.toPath())) {
      messageSupport.exportMessages(packOutputStream, getCompress().get(), this::messageCodeFilter);
    } catch(IOException ex) {
      throw new GradleException("Failed to write packed messages", ex);
    }
  }


  private boolean messageCodeFilter(@NotNull String code)
  {
    boolean match = true;

    if (!includeRegexFilters.isEmpty())
    {
      match = false;

      for(val regex: includeRegexFilters)
        if (code.matches(regex))
        {
          match = true;
          break;
        }
    }

    if (match)
      for(val regex: excludeRegexFilters)
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

    messageSupport.setMessageFilter(message -> {
      final String code = message.getCode();

      if (!accessor.hasMessageWithCode(code))
        return true;
      else if (!accessor.getMessageByCode(code).isSame(message))
        throw new DuplicateMessageException(code, "Duplicate message code '" + code + "'");

      return false;
    });

    messageSupport.setTemplateFilter((name, template) -> {
      if (!accessor.hasTemplateWithName(name))
        return true;
      else if (!accessor.getTemplateByName(name).isSame(template))
        throw new DuplicateTemplateException(name, "Duplicate template name '" + name + "'");

      return false;
    });
  }


  private void configureDuplicateIgnoreStrategy(@NotNull ConfigurableMessageSupport messageSupport,
                                                boolean warn)
  {
    val accessor = messageSupport.getAccessor();

    messageSupport.setMessageFilter(message -> {
      final String code = message.getCode();

      if (!accessor.hasMessageWithCode(code))
        return true;
      else if (warn && !accessor.getMessageByCode(code).isSame(message))
        getLogger().warn("Duplicate message code '" + code + "'");

      return false;
    });

    messageSupport.setTemplateFilter((name, template) -> {
      if (!accessor.hasTemplateWithName(name))
        return true;
      else if (warn && !accessor.getTemplateByName(name).isSame(template))
        getLogger().warn("Duplicate template name '" + name + "'");

      return false;
    });
  }


  private void configureDuplicateOverrideStrategy(
      @NotNull ConfigurableMessageSupport messageSupport, boolean warn)
  {
    val accessor = messageSupport.getAccessor();

    messageSupport.setMessageFilter(message -> {
      final String code = message.getCode();

      if (warn && accessor.hasMessageWithCode(code))
      {
        if (accessor.getMessageByCode(code).isSame(message))
          return false;

        getLogger().warn("Duplicate message code '" + code + "'");
      }

      return true;
    });

    messageSupport.setTemplateFilter((name, template) -> {
      if (warn && accessor.hasTemplateWithName(name))
      {
        if (accessor.getTemplateByName(name).isSame(template))
          return false;

        getLogger().warn("Duplicate template name '" + name + "'");
      }

      return true;
    });
  }
}
