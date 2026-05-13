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
package de.sayayi.lib.message.internal.part.parameter;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter.ClassifierContext;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter.SizeQueryable;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.internal.part.config.BaseConfigAccessor;
import de.sayayi.lib.message.part.MapKey;
import de.sayayi.lib.message.part.MessagePart;
import de.sayayi.lib.message.part.MessagePart.Config;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static de.sayayi.lib.message.formatter.parameter.ParameterFormatter.ClassifierContext.CLASSIFIER_NULL;
import static de.sayayi.lib.message.formatter.parameter.ParameterFormatter.NULL_TYPE;
import static de.sayayi.lib.message.part.MessagePart.Text.NULL;
import static de.sayayi.lib.message.part.TextPartFactory.addSpaces;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;


/**
 * Internal implementation of {@link ParameterFormatterContext} that provides parameter formatters with all context
 * information required for formatting a parameter value. This includes access to parameter values, configuration,
 * map messages and formatter delegation.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
final class ParameterFormatterContextImpl extends BaseConfigAccessor implements ParameterFormatterContext
{
  private final @NotNull MessagePart.Map map;
  private final @NotNull Parameters parameters;
  private final Object value;
  private final String format;
  private final @NotNull ParameterFormatter[] parameterFormatters;
  private int parameterFormatterIndex = 0;


  /**
   * Creates a new parameter formatter context.
   *
   * @param messageAccessor  message accessor, not {@code null}
   * @param parameters       formatting parameters, not {@code null}
   * @param value            parameter value to format, or {@code null}
   * @param type             value type, or {@code null} to determine automatically
   * @param format           formatter name, or {@code null}
   * @param config           parameter configuration, not {@code null}
   * @param map              parameter map, not {@code null}
   */
  ParameterFormatterContextImpl(@NotNull MessageAccessor messageAccessor, @NotNull Parameters parameters,
                                Object value, Class<?> type, String format, @NotNull Config config,
                                @NotNull MessagePart.Map map)
  {
    super(messageAccessor, config);

    this.map = map;
    this.parameters = parameters;
    this.value = value;
    this.format = format;

    if (type == null)
      type = value == null ? NULL_TYPE : value.getClass();

    parameterFormatters = messageAccessor.getFormatters(format, type, config);
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MessagePart.Map getMap() {
    return map;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MessageAccessor getMessageAccessor() {
    return messageAccessor;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull Locale getLocale() {
    return parameters.getLocale();
  }


  /** {@inheritDoc} */
  @Override
  public Object getParameterValue(@NotNull String parameter) {
    return parameters.getParameterValue(parameter);
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull Set<String> getParameterNames() {
    return parameters.getParameterNames();
  }


  /** {@inheritDoc} */
  @Override
  public boolean hasMapMessage(@NotNull MapKey.Type keyType) {
    return map.hasMessageWithKeyType(keyType);
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull Optional<Message.WithSpaces> getMapMessage(
      Object key, @NotNull Set<MapKey.Type> keyTypes, boolean includeDefault) {
    return ofNullable(map.getMessage(messageAccessor, key, getLocale(), keyTypes, includeDefault, config));
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull Text delegateToNextFormatter()
  {
    if (parameterFormatterIndex == parameterFormatters.length)
      throw new NoSuchElementException();

    return parameterFormatters[parameterFormatterIndex++].format(this, value);
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull Text format(Object value)
  {
    // propagate current format and parameter config to the next formatter
    return new ParameterFormatterContextImpl(messageAccessor, parameters, value, null, format, config, map)
        .delegateToNextFormatter();
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull Text format(Object value, @NotNull Class<?> type)
  {
    // propagate current format and parameter config to the next formatter
    return new ParameterFormatterContextImpl(messageAccessor, parameters, value, type, format, config, map)
        .delegateToNextFormatter();
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull Text format(Object value, Class<?> type, String format, Config config)
  {
    return new ParameterFormatterContextImpl(messageAccessor, parameters, value, type, format,
        config == null ? this.config : config, map).delegateToNextFormatter();
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull Text format(Message.WithSpaces message)
  {
    return message == null
        ? NULL
        : addSpaces(
            message.formatAsText(messageAccessor, parameters),
            message.isSpaceBefore(),
            message.isSpaceAfter());
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull OptionalLong size(Object value)
  {
    if (value != null)
    {
      OptionalLong result;

      for(var formatter: messageAccessor.getFormatters(value.getClass(), config))
        if (formatter instanceof SizeQueryable &&
            (result = ((SizeQueryable)formatter).size(this, value)).isPresent())
          return result;
    }

    return OptionalLong.empty();
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull Set<String> getClassifiers(Object value, Config config)
  {
    final var context = new InternalClassifierContext(config == null ? this.config : config);

    if (!context.updateClassifiers(value))
      context.addClassifier(CLASSIFIER_NULL);

    return context.getClassifiers();
  }




  /**
   * Internal implementation of {@link ClassifierContext} that collects classifier labels for a parameter value by
   * consulting registered formatters.
   */
  private final class InternalClassifierContext extends BaseConfigAccessor implements ClassifierContext
  {
    private final Set<String> classifiers = new LinkedHashSet<>();


    /**
     * Creates a new classifier context with the given configuration.
     *
     * @param config  the parameter configuration, not {@code null}
     */
    private InternalClassifierContext(@NotNull Config config) {
      super(ParameterFormatterContextImpl.this.messageAccessor, config);
    }


    /** {@inheritDoc} */
    @Override
    public void addClassifier(@NotNull String classifier) {
      classifiers.add(requireNonNull(classifier, "classifier must not be null"));
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull Set<String> getClassifiers() {
      return classifiers;
    }


    /** {@inheritDoc} */
    @Override
    public boolean updateClassifiers(Object value, @NotNull Config config)
    {
      if (value != null)
      {
        for(var formatter: messageAccessor.getFormatters(value.getClass(), config))
          if (formatter.updateClassifiers(this, value))
            break;
      }

      return !classifiers.isEmpty();
    }
  }
}
