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
package de.sayayi.plugin.gradle.message;

import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupport.ConfigurableMessageSupport;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.adopter.AsmAnnotationAdopter;
import de.sayayi.lib.message.exception.DuplicateMessageException;
import de.sayayi.lib.message.exception.DuplicateTemplateException;
import de.sayayi.lib.message.formatter.GenericFormatterService;
import lombok.val;
import lombok.var;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.util.PatternFilterable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.plugin.gradle.message.DuplicateMsgStrategy.IGNORE_AND_WARN;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.newOutputStream;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Locale.ROOT;
import static org.gradle.api.logging.LogLevel.ERROR;
import static org.gradle.api.logging.LogLevel.WARN;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public abstract class MessageFormatPackTask extends DefaultTask
{
  private static final Action<PatternFilterable> CLASS_FILES =
      patternFilterable -> patternFilterable.include("**/*.class");

  private final List<String> includeRegexFilters = new ArrayList<>();
  private final List<String> excludeRegexFilters = new ArrayList<>();
  private Action<MessageAccessor> action = null;

  private final ThreadLocal<String> currentClassName = new ThreadLocal<>();


  public MessageFormatPackTask()
  {
    getCompress().convention(false);
    getDuplicateMsgStrategy().convention(IGNORE_AND_WARN);
    getValidateReferencedTemplates().convention(true);
  }


  @Input
  public abstract Property<String> getPackFilename();


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
  public abstract Property<Object> getDuplicateMsgStrategy();


  @Input
  public abstract Property<Boolean> getValidateReferencedTemplates();


  @Input
  public abstract Property<Boolean> getCompress();


  @OutputFile
  public RegularFile getPackFile()
  {
    val project = getProject();

    return project.getLayout()
        .getBuildDirectory()
        .dir(project.provider(this::getName)).get()
        .file(getPackFilename()).get();
  }


  public void include(String... regex) {
    includeRegexFilters.addAll(asList(regex));
  }


  public void exclude(String... regex) {
    excludeRegexFilters.addAll(asList(regex));
  }


  public void sourceSet(SourceSet sourceSet) {
    getSources().from(sourceSet.getOutput());
  }


  public void action(Action<MessageAccessor> action)
  {
    if (action == null)
      throw new InvalidUserDataException("Action must not be null!");

    this.action = action;
  }


  @TaskAction
  public void pack()
  {
    val messageSupport = MessageSupportFactory
        .create(new GenericFormatterService(), NO_CACHE_INSTANCE);

    configureDuplicatesStrategy(messageSupport);

    pack_scanMessages(messageSupport);
    pack_validateTemplates(messageSupport);
    pack_action(messageSupport);
    pack_write(messageSupport);
  }


  private void pack_scanMessages(@NotNull ConfigurableMessageSupport messageSupport)
  {
    val logger = getLogger();

    logger.info("Scanning classes for messages and templates");

    try {
      val adopter = new AsmAnnotationAdopter(messageSupport);

      for(val classFile: getSources().getAsFileTree().matching(CLASS_FILES).getFiles())
      {
        logger.debug("Scanning " + classFile.getAbsolutePath());
        currentClassName.set(getClassName(classFile));
        adopter.adopt(classFile);
      }
    } catch(Exception ex) {
      throw new GradleException("Failed to scan messages", ex);
    }
  }


  private void pack_validateTemplates(@NotNull MessageSupport messageSupport)
  {
    if (getValidateReferencedTemplates().get())
    {
      getLogger().debug("Validating referenced templates");

      val missingTemplateNames = new ArrayList<>(messageSupport.getMessageAccessor()
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


  private void pack_action(@NotNull MessageSupport messageSupport)
  {
    if (action != null)
      action.execute(messageSupport.getMessageAccessor());
  }


  private void pack_write(@NotNull MessageSupport messageSupport)
  {
    val packFile = getPackFile().getAsFile();
    getLogger().debug("Writing message pack: " + packFile.getAbsolutePath());

    // create parent directory
    getProject().mkdir(packFile.getParentFile());

    try(val packOutputStream = newOutputStream(packFile.toPath())) {
      messageSupport.exportMessages(packOutputStream, getCompress().get(), this::messageCodeFilter);
    } catch(IOException ex) {
      throw new GradleException("Failed to write message pack", ex);
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
    switch(configureDuplicatesStrategy_toEnum())
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


  @Contract(pure = true)
  private @NotNull DuplicateMsgStrategy configureDuplicatesStrategy_toEnum()
  {
    var value = getDuplicateMsgStrategy().get();

    if (value instanceof DuplicateMsgStrategy)
      return (DuplicateMsgStrategy)value;

    if (value instanceof String)
    {
      val valueAsIs = ((String)value).toUpperCase(ROOT);
      val valueUnderscore = valueAsIs.replace('-', '_');

      for(val ds: DuplicateMsgStrategy.values())
        if (ds.name().equals(valueAsIs) ||
            ds.name().equals(valueUnderscore))
          return ds;
    }

    throw new GradleException("Unknown duplicates strategy: " + value);
  }


  private void configureDuplicateFailStrategy(@NotNull ConfigurableMessageSupport messageSupport)
  {
    val messageAccessor = messageSupport.getMessageAccessor();

    messageSupport.setMessageFilter(message -> {
      final String code = message.getCode();

      if (!messageAccessor.hasMessageWithCode(code))
        return true;
      else if (!messageAccessor.getMessageByCode(code).isSame(message))
        throw new DuplicateMessageException(code, logDuplicateMessage(ERROR, code));

      return false;
    });

    messageSupport.setTemplateFilter((name, template) -> {
      if (!messageAccessor.hasTemplateWithName(name))
        return true;
      else if (!messageAccessor.getTemplateByName(name).isSame(template))
        throw new DuplicateTemplateException(name, logDuplicateTemplate(ERROR, name));

      return false;
    });
  }


  private void configureDuplicateIgnoreStrategy(@NotNull ConfigurableMessageSupport messageSupport,
                                                boolean warn)
  {
    val messageAccessor = messageSupport.getMessageAccessor();

    messageSupport.setMessageFilter(message -> {
      final String code = message.getCode();

      if (!messageAccessor.hasMessageWithCode(code))
        return true;
      else if (warn && !messageAccessor.getMessageByCode(code).isSame(message))
        logDuplicateMessage(WARN, code);

      return false;
    });

    messageSupport.setTemplateFilter((name, template) -> {
      if (!messageAccessor.hasTemplateWithName(name))
        return true;
      else if (warn && !messageAccessor.getTemplateByName(name).isSame(template))
        logDuplicateTemplate(WARN, name);

      return false;
    });
  }


  private void configureDuplicateOverrideStrategy(
      @NotNull ConfigurableMessageSupport messageSupport, boolean warn)
  {
    val messageAccessor = messageSupport.getMessageAccessor();

    messageSupport.setMessageFilter(message -> {
      final String code = message.getCode();

      if (warn && messageAccessor.hasMessageWithCode(code))
      {
        if (messageAccessor.getMessageByCode(code).isSame(message))
          return false;

        logDuplicateMessage(WARN, code);
      }

      return true;
    });

    messageSupport.setTemplateFilter((name, template) -> {
      if (warn && messageAccessor.hasTemplateWithName(name))
      {
        if (messageAccessor.getTemplateByName(name).isSame(template))
          return false;

        logDuplicateTemplate(WARN, name);
      }

      return true;
    });
  }


  private @NotNull String logDuplicateMessage(@NotNull LogLevel level, @NotNull String code)
  {
    val msg = "Duplicate message code '" + code + "' in class " + currentClassName.get();

    getLogger().log(level, msg);

    return msg;
  }


  private @NotNull String logDuplicateTemplate(@NotNull LogLevel level, @NotNull String name)
  {
    val msg = "Duplicate template name '" + name + "' in class " + currentClassName.get();

    getLogger().log(level, msg);

    return msg;
  }


  @Contract(pure = true)
  private @NotNull String getClassName(@NotNull File classFile)
  {
    try {
      return new ClassReader(newInputStream(classFile.toPath()))
          .getClassName()
          .replace('/', '.');
    } catch(IOException ex) {
      throw new GradleException("Failed to read class name from " + classFile, ex);
    }
  }
}
