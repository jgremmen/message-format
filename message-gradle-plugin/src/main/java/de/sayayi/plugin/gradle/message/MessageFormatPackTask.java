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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static de.sayayi.lib.message.util.MessageUtil.isMessageFormatPack;
import static de.sayayi.plugin.gradle.message.DuplicateStrategy.IGNORE_AND_WARN;
import static java.nio.file.Files.newOutputStream;
import static java.nio.file.Files.readAllBytes;
import static org.gradle.api.logging.LogLevel.ERROR;
import static org.gradle.api.logging.LogLevel.WARN;
import static org.gradle.api.tasks.PathSensitivity.RELATIVE;


/**
 * Gradle task for scanning messages and templates in classes and packing the messages, together with the templates they
 * reference, into a single file which can be imported into a {@link MessageSupport.ConfigurableMessageSupport}.
 * Templates not referenced by any packed message are not included.
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
  /** Filter selecting class files only. */
  private static final Action<@NotNull PatternFilterable> CLASS_FILES =
      patternFilterable -> patternFilterable.include("**/*.class");

  /** Nested messages configuration. */
  private final MessageFormatMessagesExtension messages;

  /** Nested templates configuration. */
  private final MessageFormatTemplatesExtension templates;

  /** Custom actions, executed in order of definition before the message pack is written. */
  private final List<Action<@NotNull MessageAccessor>> actionList = new ArrayList<>();

  /** Name of the class currently being scanned, used for reporting duplicates. */
  private final AtomicReference<String> currentClassName = new AtomicReference<>();


  /**
   * Creates a new message pack task. Duplicate messages and templates are ignored with a warning, template references
   * are validated and the message pack is not compressed by default.
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
   * Returns the nested messages extension for configuring message inclusion/exclusion filters and duplicate message
   * handling strategy.
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
   *
   * @since 0.8.1
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
   * Adds all outputs for the given {@code sourceSet} to the collection of sources.
   *
   * @param sourceSet  source set to include in message/template scanning
   *
   * @see #getSources()
   */
  public void sourceSet(SourceSet sourceSet) {
    getSources().from(sourceSet.getOutput());
  }


  /**
   * Returns compress message pack property. The default for this property is {@code false}.
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
   * Provides the task with an action that allows for querying the scanned messages and templates. This action is
   * invoked just before the messages and templates are written to the packed message file.
   * <p>
   * Here's an example of how to use this action.<br>
   * Let's say all message codes start with a prefix {@code MSG-} followed by a 4-digit number (e.g. {@code MSG-0318}).
   * The action could then be used to output the next 10 available message codes:
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
    final var messageCodeFilter = createMessageCodeFilter();
    final var ignoreTemplatePatterns = compilePatterns(getTemplates().getIgnoreRegexFilters().get(), "ignore");
    final var messageSupport = MessageSupportFactory.createGeneric();

    configureDuplicatesStrategy(messageSupport);

    pack_scanMessages(messageSupport);
    pack_validateTemplates(messageSupport, messageCodeFilter, ignoreTemplatePatterns);
    pack_action(messageSupport);
    pack_write(messageSupport, messageCodeFilter);
  }


  /**
   * Scans all class files in the configured sources and adds the messages and templates found to
   * {@code messageSupport}.
   *
   * @param messageSupport  message support receiving the messages and templates, not {@code null}
   *
   * @throws GradleException  if scanning fails
   */
  private void pack_scanMessages(@NotNull ConfigurableMessageSupport messageSupport)
  {
    final var logger = getLogger();

    logger.info("Scanning classes for messages and templates");

    try {
      final var adopter = new AnnotationAdopter(messageSupport);
      final var trace = logger.isTraceEnabled();

      getSources()
          .getAsFileTree()
          .matching(CLASS_FILES)
          .getFiles()
          .stream()
          .map(File::toPath)
          .forEach(classPath -> {
            final var classFileContent = readClassFile(classPath);
            final var className = new ClassReader(classFileContent).getClassName().replace('/', '.');

            if (trace)
              logger.trace("Scanning class {}, path {}", className, classPath);
            else
              logger.debug("Scanning class {}", className);

            currentClassName.set(className);
            adopter.adopt(classFileContent);
          });
    } catch(Exception ex) {
      throw new GradleException("Failed to scan messages", ex);
    } finally {
      currentClassName.set(null);
    }
  }


  /**
   * Validates that all templates referenced by the selected messages are available, unless template validation is
   * disabled. Missing templates matching one of the ignore patterns are not reported.
   *
   * @param messageSupport          message support containing the scanned messages and templates, not {@code null}
   * @param messageCodeFilter       filter selecting the messages to validate, not {@code null}
   * @param ignoreTemplatePatterns  patterns of missing template names to ignore, not {@code null}
   *
   * @throws GradleException  if referenced templates are missing
   */
  private void pack_validateTemplates(@NotNull MessageSupport messageSupport,
                                      @NotNull Predicate<String> messageCodeFilter,
                                      @NotNull List<Pattern> ignoreTemplatePatterns)
  {
    final var templates = getTemplates();

    if (templates.getValidateReferences().get())
    {
      final var logger = getLogger();

      logger.debug("Validating referenced templates");

      final var missingTemplateNames = new ArrayList<>(messageSupport
          .getMessageAccessor()
          .findMissingTemplates(messageCodeFilter));

      for(var iterator = missingTemplateNames.iterator(); iterator.hasNext();)
      {
        final var templateName = iterator.next();

        for(var ignorePattern: ignoreTemplatePatterns)
          if (ignorePattern.matcher(templateName).matches())
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


  /**
   * Executes all registered custom actions.
   *
   * @param messageSupport  message support containing the scanned messages and templates, not {@code null}
   */
  private void pack_action(@NotNull MessageSupport messageSupport)
  {
    final var messageAccessor = messageSupport.getMessageAccessor();

    actionList.forEach(action -> action.execute(messageAccessor));
  }


  /**
   * Writes the selected messages and their templates to the message pack file and verifies the result.
   *
   * @param messageSupport     message support containing the scanned messages and templates, not {@code null}
   * @param messageCodeFilter  filter selecting the messages to write, not {@code null}
   *
   * @throws GradleException  if the message pack cannot be written
   */
  private void pack_write(@NotNull MessageSupport messageSupport, @NotNull Predicate<String> messageCodeFilter)
  {
    final var packFile = getPackFile().getAsFile().toPath();
    getLogger().info("Writing message pack: {}", packFile);

    try {
      try(var packOutputStream = newOutputStream(packFile)) {
        messageSupport.exportMessages(packOutputStream, getCompress().get(), messageCodeFilter, null);
      }

      if (!isMessageFormatPack(packFile))
        throw new IOException("Message pack file missing or corrupt");
    } catch(Exception ex) {
      throw new GradleException("Failed to write message pack", ex);
    }
  }


  /**
   * Creates a filter that tells whether the message with a given code is selected by the include and exclude patterns.
   * If no include patterns are configured, all messages are included; exclude patterns take precedence over include
   * patterns. The patterns are compiled once.
   *
   * @return  message code filter, never {@code null}
   *
   * @throws InvalidUserDataException  if an include or exclude pattern is not a valid regular expression
   *
   * @since 0.25.0
   */
  @Contract(pure = true)
  private @NotNull Predicate<String> createMessageCodeFilter()
  {
    final var includePatterns = compilePatterns(messages.getIncludeRegexFilters().get(), "include");
    final var excludePatterns = compilePatterns(messages.getExcludeRegexFilters().get(), "exclude");

    return code -> {
      var match = includePatterns.isEmpty();

      if (!match)
        for(var pattern: includePatterns)
          if (pattern.matcher(code).matches())
          {
            match = true;
            break;
          }

      if (match)
        for(var pattern: excludePatterns)
          if (pattern.matcher(code).matches())
          {
            match = false;
            break;
          }

      return match;
    };
  }


  /**
   * Compiles the given regular expressions.
   *
   * @param regexList  regular expressions, not {@code null}
   * @param kind       kind of the filter ({@code include}, {@code exclude} or {@code ignore}), used in the error message
   *
   * @return  compiled patterns, never {@code null}
   *
   * @throws InvalidUserDataException  if a regular expression is not valid
   *
   * @since 0.25.0
   */
  @Contract(pure = true)
  private static @NotNull List<Pattern> compilePatterns(@NotNull List<String> regexList, @NotNull String kind)
  {
    final var patterns = new ArrayList<Pattern>(regexList.size());

    for(var regex: regexList)
    {
      try {
        patterns.add(Pattern.compile(regex));
      } catch(PatternSyntaxException ex) {
        throw new InvalidUserDataException("Invalid " + kind + " regular expression '" + regex + "': " +
            ex.getDescription(), ex);
      }
    }

    return patterns;
  }


  /**
   * Configures the message and template filters of {@code messageSupport} according to the duplicate strategy.
   *
   * @param messageSupport  message support to configure, not {@code null}
   *
   * @throws InvalidUserDataException  if the configured duplicate strategy is unknown
   */
  private void configureDuplicatesStrategy(@NotNull ConfigurableMessageSupport messageSupport)
  {
    switch(messages.getResolvedDuplicateStrategy().get())
    {
      case FAIL -> configureDuplicateFailStrategy(messageSupport);
      case OVERRIDE -> configureDuplicateOverrideStrategy(messageSupport, false);
      case OVERRIDE_AND_WARN -> configureDuplicateOverrideStrategy(messageSupport, true);
      case IGNORE -> configureDuplicateIgnoreStrategy(messageSupport, false);
      case IGNORE_AND_WARN -> configureDuplicateIgnoreStrategy(messageSupport, true);
    }
  }


  /**
   * Configures filters that fail on duplicate messages or templates with different content and ignore identical
   * duplicates.
   *
   * @param messageSupport  message support to configure, not {@code null}
   */
  private void configureDuplicateFailStrategy(@NotNull ConfigurableMessageSupport messageSupport)
  {
    final var messageAccessor = messageSupport.getMessageAccessor();

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


  /**
   * Configures filters that keep the first message or template and ignore any duplicates.
   *
   * @param messageSupport  message support to configure, not {@code null}
   * @param warn            {@code true} to log a warning for duplicates with different content, {@code false} otherwise
   */
  private void configureDuplicateIgnoreStrategy(@NotNull ConfigurableMessageSupport messageSupport, boolean warn)
  {
    final var messageAccessor = messageSupport.getMessageAccessor();

    messageSupport.setMessageFilter(message -> {
      final var code = message.getCode();

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


  /**
   * Configures filters that replace an existing message or template with a duplicate.
   *
   * @param messageSupport  message support to configure, not {@code null}
   * @param warn            {@code true} to log a warning for duplicates with different content (identical duplicates
   *                        are ignored in this case), {@code false} otherwise
   */
  private void configureDuplicateOverrideStrategy(@NotNull ConfigurableMessageSupport messageSupport, boolean warn)
  {
    var messageAccessor = messageSupport.getMessageAccessor();

    messageSupport.setMessageFilter(message -> {
      final var code = message.getCode();

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


  /**
   * Logs a duplicate message code, including the name of the class currently being scanned.
   *
   * @param level  log level, not {@code null}
   * @param code   duplicate message code, not {@code null}
   *
   * @return  logged message, never {@code null}
   */
  private @NotNull String logDuplicateMessage(@NotNull LogLevel level, @NotNull String code)
  {
    final var msg = "Duplicate message code '" + code + "' in class " + currentClassName.get();

    getLogger().log(level, msg);

    return msg;
  }


  /**
   * Logs a duplicate template name, including the name of the class currently being scanned.
   *
   * @param level  log level, not {@code null}
   * @param name   duplicate template name, not {@code null}
   *
   * @return  logged message, never {@code null}
   */
  private @NotNull String logDuplicateTemplate(@NotNull LogLevel level, @NotNull String name)
  {
    final var msg = "Duplicate template name '" + name + "' in class " + currentClassName.get();

    getLogger().log(level, msg);

    return msg;
  }


  /**
   * Reads the content of a class file.
   *
   * @param classPath  path of the class file, not {@code null}
   *
   * @return  class file content, never {@code null}
   *
   * @throws GradleException  if the class file cannot be read
   *
   * @since 0.25.0
   */
  @Contract(pure = true)
  private static byte @NotNull [] readClassFile(@NotNull Path classPath)
  {
    try {
      return readAllBytes(classPath);
    } catch(IOException ex) {
      throw new GradleException("Failed to read class file " + classPath, ex);
    }
  }
}
