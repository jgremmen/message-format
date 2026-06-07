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
import de.sayayi.lib.message.annotation.adopter.AnnotationAdopter;
import de.sayayi.lib.message.annotation.adopter.asm.ClassReader;
import de.sayayi.lib.message.exception.DuplicateMessageException;
import de.sayayi.lib.message.exception.DuplicateTemplateException;
import de.sayayi.lib.message.formatter.GenericFormatterService;
import groovy.lang.GString;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.util.PatternFilterable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static de.sayayi.lib.message.util.MessageUtil.isMessageFormatPack;
import static de.sayayi.plugin.gradle.message.DuplicateStrategy.IGNORE_AND_WARN;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.newOutputStream;
import static java.util.Locale.ROOT;
import static org.gradle.api.logging.LogLevel.ERROR;
import static org.gradle.api.logging.LogLevel.WARN;
import static org.gradle.api.tasks.PathSensitivity.RELATIVE;


/**
 * Gradle task for scanning messages and templates in classes and packing them into a single file which can be
 * imported into a {@link MessageSupport.ConfigurableMessageSupport}.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 *
 * @see MessageFormatPlugin
 * @see MessageFormatExtension
 */
@CacheableTask
public abstract class MessageFormatPackTask extends DefaultTask
{
  private static final Action<@NotNull PatternFilterable> CLASS_FILES =
      patternFilterable -> patternFilterable.include("**/*.class");

  private final MessageFormatMessagesExtension messages;
  private final MessageFormatTemplatesExtension templates;
  private final List<Action<@NotNull MessageAccessor>> actionList = new ArrayList<>();

  private final ThreadLocal<String> currentClassName = new ThreadLocal<>();


  /**
   * Gradle task constructor.
   *
   * @param objectFactory  Gradle object factory for creating managed instances
   */
  @Inject
  public MessageFormatPackTask(@NotNull ObjectFactory objectFactory)
  {
    messages = objectFactory.newInstance(MessageFormatMessagesExtension.class);
    messages.getDuplicateStrategy().convention(IGNORE_AND_WARN);

    templates = objectFactory.newInstance(MessageFormatTemplatesExtension.class);
    templates.getValidateReferences().convention(true);

    getCompress().convention(false);
  }


  /**
   * Returns the nested messages extension for configuring message inclusion/exclusion filters and
   * duplicate message handling strategy.
   *
   * @return  nested messages extension, never {@code null}
   *
   * @since 0.24.0
   */
  @Nested
  public MessageFormatMessagesExtension getMessages() {
    return messages;
  }


  /**
   * Configures the nested {@code messages} block.
   * <p>
   * Example usage:
   * <pre>
   *   messageFormatPack {
   *     messages {
   *       include 'xy'
   *       exclude 'r.*'
   *       duplicateStrategy = 'fail'
   *     }
   *   }
   * </pre>
   *
   * @param action  configuration action for the messages extension, not {@code null}
   *
   * @since 0.24.0
   */
  public void messages(@NotNull Action<? super MessageFormatMessagesExtension> action) {
    action.execute(messages);
  }


  /**
   * Returns the nested templates extension for configuring template validation and filtering.
   *
   * @return  nested templates extension, never {@code null}
   *
   * @since 0.24.0
   */
  @Nested
  public MessageFormatTemplatesExtension getTemplates() {
    return templates;
  }


  /**
   * Configures the nested {@code templates} block.
   * <p>
   * Example usage:
   * <pre>
   *   messageFormatPack {
   *     templates {
   *       validateReferences = true
   *       ignore 'tpl-.*'
   *     }
   *   }
   * </pre>
   *
   * @param action  configuration action for the templates extension, not {@code null}
   *
   * @since 0.24.0
   */
  public void templates(@NotNull Action<? super MessageFormatTemplatesExtension> action) {
    action.execute(templates);
  }


  /**
   * Parameter containing the destination directory where the packed message file is stored.
   *
   * @return  destination directory parameter, never {@code null}
   */
  @Internal("tracked via packFile")  // part of task output
  public abstract DirectoryProperty getDestinationDir();


  /**
   * Parameter containing the packed message file name.
   *
   * @return  packed message file name, never {@code null}
   */
  @Internal("tracked via packFile")
  public abstract Property<@NotNull String> getPackFilename();


  /**
   * Returns a collection of source files to scan for message and template annotations.
   * <p>
   * There's no restriction on what kind of files are in the collection. This task will only use and scan class
   * ({@code *.class}) files.
   *
   * @return  collection of source files to scan for messages and templates, never {@code null}
   *
   * @see #sourceSet(SourceSet)
   */
  @InputFiles
  @IgnoreEmptyDirectories
  @PathSensitive(RELATIVE)
  public abstract ConfigurableFileCollection getSources();


  /**
   * Add all outputs for the given {@code sourceSet} to the collection of sources.
   *
   * @param sourceSet  source set to include in message/template scanning
   *
   * @see #getSources()
   */
  public void sourceSet(SourceSet sourceSet) {
    getSources().from(sourceSet.getOutput());
  }


  /**
   * Return compress message pack property. The default for this property is {@code false}.
   *
   * @return  compress property, never {@code null}
   */
  @Input
  public abstract Property<@NotNull Boolean> getCompress();


  /**
   * Returns the target message pack file location.
   *
   * @return  message pack file, never {@code null}
   */
  @OutputFile
  public RegularFile getPackFile() {
    return getDestinationDir().file(getPackFilename()).get();
  }


  /**
   * Provide the task with an action that allows for querying the scanned messages and templates. This action is
   * invoked just before the messages and templates are written to the packed message file.
   * <p>
   * Here's an example of how to use this action.<br>
   * Let's say all message codes start with a prefix
   * {@code MSG-} followed by a 4-digit number (e.g. {@code MSG-0318}). The action could then be used to output the
   * next 10 available message codes:
   * <pre>
   *   action {
   *       def codes = getMessageCodes()
   *       def unusedCodes = new ArrayList&lt;String&gt;()
   *
   *       for(int n = 1; unusedCodes.size() &lt; 10; n++)
   *       {
   *         def code = String.format("MSG-%04d", n)
   *
   *         if (!codes.contains(code))
   *           unusedCodes.add(code)
   *       }
   *
   *       println 'Available message codes:'
   *       println String.join(" ", unusedCodes)
   *   }
   * </pre>
   * If multiple actions are provided, they are executed in the order of definition.
   *
   * @param action  custom action, not {@code null}
   *
   * @since 0.8.0
   */
  public void action(Action<@NotNull MessageAccessor> action)
  {
    if (action == null)
      throw new InvalidUserDataException("Action must not be null!");

    actionList.add(action);
  }


  /**
   * Scans the configured source files for message and template annotations, validates referenced templates, executes
   * registered actions and writes the packed message file.
   *
   * @throws GradleException  if scanning, validation or writing fails
   */
  @TaskAction
  public void pack()
  {
    final var messageSupport = MessageSupportFactory.create(new GenericFormatterService());

    configureDuplicatesStrategy(messageSupport);

    pack_scanMessages(messageSupport);
    pack_validateTemplates(messageSupport);
    pack_action(messageSupport);
    pack_write(messageSupport);
  }


  private void pack_scanMessages(@NotNull ConfigurableMessageSupport messageSupport)
  {
    final var logger = getLogger();

    logger.info("Scanning classes for messages and templates");

    try {
      var adopter = new AnnotationAdopter(messageSupport);
      var trace = logger.isTraceEnabled();

      getSources()
          .getAsFileTree()
          .matching(CLASS_FILES)
          .getFiles()
          .stream()
          .map(File::toPath)
          .forEach(classPath -> {
            var className = getClassName(classPath);

            if (trace)
              logger.trace("Scanning class {}, path {}", className, classPath);
            else
              logger.debug("Scanning class {}", className);

            currentClassName.set(className);
            adopter.adopt(classPath);
          });
    } catch(Exception ex) {
      throw new GradleException("Failed to scan messages", ex);
    }
  }


  private void pack_validateTemplates(@NotNull MessageSupport messageSupport)
  {
    final var templates = getTemplates();

    if (templates.getValidateReferences().get())
    {
      final var logger = getLogger();

      logger.debug("Validating referenced templates");

      final var missingTemplateNames = new ArrayList<>(messageSupport
          .getMessageAccessor()
          .findMissingTemplates(this::messageCodeFilter));
      final var ignoreRegexFilters = templates.getIgnoreRegexFilters();

      for(var iterator = missingTemplateNames.iterator(); iterator.hasNext();)
      {
        final var templateName = iterator.next();

        for(var ignoreRegex: ignoreRegexFilters)
          if (ignoreRegex.matches(templateName))
          {
            logger.debug("Ignore missing template '{}'", templateName);
            iterator.remove();
            break;
          }
      }

      final var count = missingTemplateNames.size();

      switch(count)
      {
        case 0:
          break;

        case 1:
          throw new GradleException("Missing message template: " + missingTemplateNames.getFirst());

        default:
          throw new GradleException("Missing message templates: " +
              String.join(", ", missingTemplateNames.subList(0, count - 1)) + " and " +
              missingTemplateNames.get(count - 1));
      }
    }
  }


  private void pack_action(@NotNull MessageSupport messageSupport)
  {
    final var messageAccessor = messageSupport.getMessageAccessor();

    actionList.forEach(action -> action.execute(messageAccessor));
  }


  private void pack_write(@NotNull MessageSupport messageSupport)
  {
    var packFile = getPackFile().getAsFile().toPath();
    getLogger().info("Writing message pack: {}", packFile);

    try {
      try(var packOutputStream = newOutputStream(packFile)) {
        messageSupport.exportMessages(packOutputStream, getCompress().get(), this::messageCodeFilter);
      }

      if (!isMessageFormatPack(packFile))
        throw new IOException("Message pack file missing or corrupt");
    } catch(Exception ex) {
      throw new GradleException("Failed to write message pack", ex);
    }
  }


  private boolean messageCodeFilter(@NotNull String code)
  {
    var includeRegexFilters = messages.getIncludeRegexFilters();
    var excludeRegexFilters = messages.getExcludeRegexFilters();
    var match = includeRegexFilters.isEmpty();

    if (!match)
      for(var regex: includeRegexFilters)
        if (code.matches(regex))
        {
          match = true;
          break;
        }

    if (match)
      for(var regex: excludeRegexFilters)
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
  private @NotNull DuplicateStrategy configureDuplicatesStrategy_toEnum()
  {
    var value = messages.getDuplicateStrategy().get();

    if (value instanceof DuplicateStrategy)
      return (DuplicateStrategy)value;

    if (value instanceof GString)
      value = ((GString)value).toString();

    if (value instanceof String)
    {
      var valueAsIs = ((String)value).toUpperCase(ROOT);
      var valueUnderscore = valueAsIs.replace('-', '_');

      for(var ds: DuplicateStrategy.values())
        if (ds.name().equals(valueAsIs) ||
            ds.name().equals(valueUnderscore))
          return ds;
    }

    throw new InvalidUserDataException("Unknown duplicates strategy: " + value);
  }


  private void configureDuplicateFailStrategy(@NotNull ConfigurableMessageSupport messageSupport)
  {
    var messageAccessor = messageSupport.getMessageAccessor();

    messageSupport.setMessageFilter(message -> {
      var code = message.getCode();

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


  private void configureDuplicateIgnoreStrategy(@NotNull ConfigurableMessageSupport messageSupport, boolean warn)
  {
    var messageAccessor = messageSupport.getMessageAccessor();

    messageSupport.setMessageFilter(message -> {
      var code = message.getCode();

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


  private void configureDuplicateOverrideStrategy(@NotNull ConfigurableMessageSupport messageSupport, boolean warn)
  {
    var messageAccessor = messageSupport.getMessageAccessor();

    messageSupport.setMessageFilter(message -> {
      var code = message.getCode();

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
    var msg = "Duplicate message code '" + code + "' in class " + currentClassName.get();

    getLogger().log(level, msg);

    return msg;
  }


  private @NotNull String logDuplicateTemplate(@NotNull LogLevel level, @NotNull String name)
  {
    var msg = "Duplicate template name '" + name + "' in class " + currentClassName.get();

    getLogger().log(level, msg);

    return msg;
  }


  @Contract(pure = true)
  private @NotNull String getClassName(@NotNull Path classPath)
  {
    try {
      return new ClassReader(newInputStream(classPath)).getClassName().replace('/', '.');
    } catch(IOException ex) {
      throw new GradleException("Failed to read class name from " + classPath, ex);
    }
  }
}
