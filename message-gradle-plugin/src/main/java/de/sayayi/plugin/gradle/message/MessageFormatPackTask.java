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
import de.sayayi.lib.message.adopter.asm.AsmAnnotationAdopter;
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
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.util.PatternFilterable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.plugin.gradle.message.DuplicateMsgStrategy.IGNORE_AND_WARN;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.newOutputStream;
import static java.util.Collections.unmodifiableList;
import static java.util.Locale.ROOT;
import static org.gradle.api.logging.LogLevel.ERROR;
import static org.gradle.api.logging.LogLevel.WARN;
import static org.gradle.api.tasks.PathSensitivity.RELATIVE;


/**
 * Gradle task for scanning messages and templates in classes and packing them into a single
 * file which can be imported into a {@link MessageSupport.ConfigurableMessageSupport}.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@CacheableTask
public abstract class MessageFormatPackTask extends DefaultTask
{
  private static final Action<PatternFilterable> CLASS_FILES =
      patternFilterable -> patternFilterable.include("**/*.class");

  private final List<String> includeRegexFilters = new ArrayList<>();
  private final List<String> excludeRegexFilters = new ArrayList<>();
  private Action<MessageAccessor> action = null;

  private final ThreadLocal<String> currentClassName = new ThreadLocal<>();


  /**
   * Gradle task constructor.
   */
  public MessageFormatPackTask()
  {
    getCompress().convention(false);
    getDuplicateMsgStrategy().convention(IGNORE_AND_WARN);
    getValidateReferencedTemplates().convention(true);
  }


  /**
   * Parameter containing the destination directory where the packed message file is stored.
   *
   * @return  destination directory parameter
   */
  @Internal("tracked via packFile")  // part of task output
  public abstract DirectoryProperty getDestinationDir();


  /**
   * Parameter containing the packed message file name.
   *
   * @return  packed message file name
   */
  @Internal("tracked via packFile")
  public abstract Property<String> getPackFilename();


  /**
   * Return a list of regular expressions which will be matched against each message code.
   * If it matches, the message will be included in the packed message file. If it doesn't match
   * the message is skipped.
   * <p>
   * If the list is empty, all messages are included, unless they're explicitly excluded.
   *
   * @return  list of regular expressions for message inclusion, never {@code null}
   *
   * @see #getExcludeRegexFilters()
   */
  @Input
  public List<String> getIncludeRegexFilters() {
    return unmodifiableList(includeRegexFilters);
  }


  /**
   * Return a list of regular expressions which will be matched against each message code.
   * If it matches, the message will be excluded from the packed message file. If it doesn't match
   * the message is included.
   *
   * @return  list of regular expressions for message exclusion, never {@code null}
   *
   * @see #getIncludeRegexFilters()
   */
  @Input
  public List<String> getExcludeRegexFilters() {
    return unmodifiableList(excludeRegexFilters);
  }


  /**
   * Returns a collection of source files to scan for message and template annotations.
   * <p>
   * There's no restriction on what kind of files are in the collection. This task will only
   * use and scan class ({@code *.class}) files.
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
   * Property containing the strategy to use in case a duplicate message code or template name
   * (with different message definition) is found. The default strategy is
   * {@link DuplicateMsgStrategy#IGNORE_AND_WARN IGNORE_AND_WARN}.
   * <p>
   * This property accepts various formats:
   * <ul>
   *   <li>
   *     {@link DuplicateMsgStrategy} enum value (e.g. {@link DuplicateMsgStrategy#FAIL FAIL})
   *   </li>
   *   <li>
   *     Strategy string. The string is converted to uppercase, dashes are translated to
   *     underscores and the resulting strategy name is matched against
   *     {@link DuplicateMsgStrategy} (e.g. {@code 'override-and-warn'} matches
   *     {@link DuplicateMsgStrategy#OVERRIDE_AND_WARN OVERRIDE_AND_WARN})
   *   </li>
   * </ul>
   * <p>
   * A duplicate is either a message with an already known message code or a template with
   * an already known template name and a different message definition. This means that if the
   * same message or template is encountered twice, it is not considered a duplicate.
   *
   * @return  duplicate message strategy property, never {@code null}
   *
   * @see DuplicateMsgStrategy
   */
  @Input
  public abstract Property<Object> getDuplicateMsgStrategy();


  /**
   * Property containing a boolean stating whether to validate referenced templates. The
   * default value resolves to {@code true}.
   * <p>
   * If the property resolves to {@code true} the task will check whether all referenced
   * templates (including nested templates) are available and included in the packed message file.
   * <p>
   * If the property resolves to {@code false} no checks are performed. This may lead to a
   * situation where a message cannot be formatted if the referenced template is missing from
   * the message support.
   *
   * @return  validate referenced templates property, never {@code null}
   */
  @Input
  public abstract Property<Boolean> getValidateReferencedTemplates();


  /**
   * Return compress message pack property. The default for this property is {@code false}.
   *
   * @return  compress property, never {@code null}
   */
  @Input
  public abstract Property<Boolean> getCompress();


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
   * Include messages matching the {@code regex}.
   *
   * @param regex  regex matching message codes
   */
  public void include(String... regex) {
    includeRegexFilters.addAll(List.of(regex));
  }


  /**
   * Exclude messages matching the {@code regex}.
   *
   * @param regex  regex matching message codes
   */
  public void exclude(String... regex) {
    excludeRegexFilters.addAll(List.of(regex));
  }


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
   * Provide the task with an action that allows for querying the scanned messages and templates.
   * This action is invoked just before the messages and templates are written to the packed message file.
   * <p>
   * Here's an example of how to use this action.<br>
   * Let's say all message codes start with a prefix
   * {@code MSG-} followed by a 4-digit number (e.g. {@code MSG-0318}). The action could then be
   * used to output the next 10 available message codes:
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
   *
   * @param action  custom action, not {@code null}
   */
  public void action(Action<MessageAccessor> action)
  {
    if (action == null)
      throw new InvalidUserDataException("Action must not be null!");

    if (this.action != null)
      getLogger().warn("Previous message format pack action will be overwritten!");

    this.action = action;
  }


  /**
   * Task action.
   */
  @TaskAction
  public void pack()
  {
    var messageSupport = MessageSupportFactory
        .create(new GenericFormatterService(), NO_CACHE_INSTANCE);

    configureDuplicatesStrategy(messageSupport);

    pack_scanMessages(messageSupport);
    pack_validateTemplates(messageSupport);
    pack_action(messageSupport);
    pack_write(messageSupport);
  }


  private void pack_scanMessages(@NotNull ConfigurableMessageSupport messageSupport)
  {
    var logger = getLogger();

    logger.info("Scanning classes for messages and templates");

    try {
      var adopter = new AsmAnnotationAdopter(messageSupport);
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
    if (getValidateReferencedTemplates().get())
    {
      getLogger().debug("Validating referenced templates");

      var missingTemplateNames = new ArrayList<>(messageSupport
          .getMessageAccessor()
          .findMissingTemplates(this::messageCodeFilter));
      var count = missingTemplateNames.size();

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
    var packFile = getPackFile().getAsFile().toPath();
    getLogger().info("Writing message pack: {}", packFile);

    try(var packOutputStream = newOutputStream(packFile)) {
      messageSupport.exportMessages(packOutputStream, getCompress().get(), this::messageCodeFilter);
    } catch(IOException ex) {
      throw new GradleException("Failed to write message pack", ex);
    }
  }


  private boolean messageCodeFilter(@NotNull String code)
  {
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
  private @NotNull DuplicateMsgStrategy configureDuplicatesStrategy_toEnum()
  {
    var value = getDuplicateMsgStrategy().get();

    if (value instanceof DuplicateMsgStrategy)
      return (DuplicateMsgStrategy)value;

    if (value instanceof GString)
      value = ((GString)value).toString();

    if (value instanceof String)
    {
      var valueAsIs = ((String)value).toUpperCase(ROOT);
      var valueUnderscore = valueAsIs.replace('-', '_');

      for(var ds: DuplicateMsgStrategy.values())
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
