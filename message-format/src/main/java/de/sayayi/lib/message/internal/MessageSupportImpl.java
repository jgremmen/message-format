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
package de.sayayi.lib.message.internal;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.exception.DuplicateMessageException;
import de.sayayi.lib.message.exception.DuplicateTemplateException;
import de.sayayi.lib.message.formatter.FormatterService;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter;
import de.sayayi.lib.message.formatter.post.PostFormatter;
import de.sayayi.lib.message.internal.pack.PackSupport;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueBool;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueMessage;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueNumber;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueString;
import de.sayayi.lib.message.part.MessagePart;
import de.sayayi.lib.message.part.TypedValue;
import de.sayayi.lib.message.util.SortedStringMap;
import de.sayayi.lib.message.util.SupplierDelegate;
import de.sayayi.lib.pack.PackOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static de.sayayi.lib.message.internal.pack.PackSupport.PACK_CONFIG;
import static de.sayayi.lib.message.internal.pack.PackSupport.VERSION;
import static de.sayayi.lib.message.util.MessageUtil.*;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;


/**
 * Default implementation of {@link ConfigurableMessageSupport}.
 * <p>
 * This class manages a set of messages (identified by code), templates (identified by name) and default configuration
 * values. It provides the fluent {@link MessageConfigurer} API for preparing and formatting messages.
 * <p>
 * Duplicate messages and templates are handled by configurable filters. By default, adding a message or template with
 * a code or name that already exists will throw a {@link DuplicateMessageException} or
 * {@link DuplicateTemplateException} respectively.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class MessageSupportImpl implements MessageSupport.ConfigurableMessageSupport
{
  private final @NotNull FormatterService formatterService;
  private final @NotNull MessageFactory messageFactory;
  private final @NotNull Map<String,TypedValue<?>> defaultConfig = new TreeMap<>();
  private final @NotNull Map<String,Message.WithCode> messages = new TreeMap<>();
  private final @NotNull Map<String,Message> templates = new TreeMap<>();
  private final @NotNull MessageAccessor messageAccessor;

  private @NotNull Locale locale;
  private @NotNull MessageFilter messageFilter;
  private @NotNull TemplateFilter templateFilter;


  /**
   * Creates a new message support instance with the given formatter service and message factory.
   *
   * @param formatterService  formatter service providing parameter and post formatters, not {@code null}
   * @param messageFactory    factory for parsing and creating messages, not {@code null}
   */
  public MessageSupportImpl(@NotNull FormatterService formatterService, @NotNull MessageFactory messageFactory)
  {
    this.formatterService = requireNonNull(formatterService, "formatterService must not be null");
    this.messageFactory = requireNonNull(messageFactory, "messageFactory must not be null");

    messageAccessor = new Accessor();
    locale = Locale.getDefault();
    messageFilter = this::failOnDuplicateMessage;
    templateFilter = this::failOnDuplicateTemplate;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MessageAccessor getMessageAccessor() {
    return messageAccessor;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull ConfigurableMessageSupport setLocale(@NotNull Locale locale)
  {
    this.locale = requireNonNull(locale, "locale must not be null");
    return this;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull ConfigurableMessageSupport setDefaultConfig(@NotNull String name, boolean value)
  {
    defaultConfig.put(
        validateName(name, "config name"),
        value ? TypedValueBool.TRUE : TypedValueBool.FALSE);
    return this;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull ConfigurableMessageSupport setDefaultConfig(@NotNull String name, long value)
  {
    defaultConfig.put(validateName(name, "config name"), new TypedValueNumber(value));
    return this;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull ConfigurableMessageSupport setDefaultConfig(@NotNull String name, @NotNull String value)
  {
    defaultConfig.put(validateName(name, "config name"), new TypedValueString(value));
    return this;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull ConfigurableMessageSupport setDefaultConfig(@NotNull String name, @NotNull Message.WithSpaces value)
  {
    defaultConfig.put(validateName(name, "config name"), new TypedValueMessage(value));
    return this;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull ConfigurableMessageSupport setMessageFilter(@NotNull MessageFilter messageFilter)
  {
    this.messageFilter = requireNonNull(messageFilter, "messageFilter must not be null");
    return this;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull ConfigurableMessageSupport setTemplateFilter(@NotNull TemplateFilter templateFilter)
  {
    this.templateFilter = requireNonNull(templateFilter, "templateFilter must not be null");
    return this;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull ConfigurableMessageSupport addMessage(@NotNull Message.WithCode message)
  {
    if (messageFilter.filter(requireNonNull(message, "message must not be null")))
      messages.put(message.getCode(), message);

    return this;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull ConfigurableMessageSupport addTemplate(@NotNull String name, @NotNull Message template)
  {
    if (!isKebabCaseName(validateName(name, "template name")))
      throw new IllegalArgumentException("template name '" + name + "' must match the kebab-case naming convention");

    if (templateFilter.filter(name, template))
      templates.put(name, requireNonNull(template));

    return this;
  }


  /** {@inheritDoc} */
  @Override
  public void exportMessages(@NotNull OutputStream stream, boolean compress, Predicate<String> messageCodeFilter)
      throws IOException
  {
    try(var dataStream = new PackOutputStream(PACK_CONFIG, VERSION, compress, stream)) {
      final var messageCodes = new TreeSet<>(messages.keySet());
      final var templateNames = new TreeSet<String>();

      // filter message codes
      if (messageCodeFilter != null)
        messageCodes.removeIf(messageCodeFilter.negate());

      // pack all filtered messages
      dataStream.writeUnsignedShort(messageCodes.size());
      for(var code: messageCodes)
      {
        final var message = messages.get(code);

        templateNames.addAll(message.getTemplateNames());
        PackSupport.pack(message, dataStream);
      }

      // pack all required templates
      templateNames.removeIf(templateName -> !templates.containsKey(templateName));
      dataStream.writeUnsignedShort(templateNames.size());
      for(var templateName: templateNames)
      {
        dataStream.writeString(templateName);
        PackSupport.pack(templates.get(templateName), dataStream);
      }
    }
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MessageConfigurer<Message.WithCode> code(@NotNull String code)
  {
    var message = messages.get(validateName(code, "message code"));
    if (message == null)
      throw new IllegalArgumentException("unknown message code '" + code + '\'');

    return new Configurer<>(() -> message);
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MessageConfigurer<Message> message(@NotNull String message) {
    return new Configurer<>(SupplierDelegate.of(() -> messageFactory.parseMessage(message)));
  }


  /** {@inheritDoc} */
  @Override
  public <M extends Message> @NotNull MessageConfigurer<M> message(@NotNull M message)
  {
    requireNonNull(message, "message must not be null");

    return new Configurer<>(() -> message);
  }


  /**
   * Default message filter that rejects duplicate messages with different content.
   * <p>
   * If a message with the same code already exists and has the same content, the new message is silently ignored.
   * If the content differs, a {@link DuplicateMessageException} is thrown.
   *
   * @param message  message to check, not {@code null}
   *
   * @return  {@code true} if the message should be added, {@code false} if it already exists with the same content
   *
   * @throws DuplicateMessageException  if a different message with the same code already exists
   */
  private boolean failOnDuplicateMessage(@NotNull Message.WithCode message)
  {
    final var code = message.getCode();
    final var tm = messages.get(code);

    if (tm != null)
    {
      if (!tm.isSame(message))
      {
        throw new DuplicateMessageException(code,
            "different message with identical code '" + code + "' already exists");
      }

      return false;
    }

    return true;
  }


  /**
   * Default template filter that rejects duplicate templates with different content.
   * <p>
   * If a template with the same name already exists and has the same content, the new template is silently ignored.
   * If the content differs, a {@link DuplicateTemplateException} is thrown.
   *
   * @param name      template name, not {@code null}
   * @param template  template to check, not {@code null}
   *
   * @return  {@code true} if the template should be added, {@code false} if it already exists with the same content
   *
   * @throws DuplicateTemplateException  if a different template with the same name already exists
   */
  private boolean failOnDuplicateTemplate(@NotNull String name, @NotNull Message template)
  {
    var ttm = templates.get(name);
    if (ttm != null)
    {
      if (!ttm.isSame(template))
      {
        throw new DuplicateTemplateException(name,
            "different template with identical name '" + name + "' already exists");
      }

      return false;
    }

    return true;
  }




  /**
   * Internal {@link MessageConfigurer} implementation that holds the message, locale and parameter values for
   * a single formatting operation.
   *
   * @param <M>  the message type this configurer operates on
   */
  public final class Configurer<M extends Message> implements MessageConfigurer<M>
  {
    private final @NotNull Supplier<M> message;
    @NotNull Locale locale;
    @NotNull Map<String,Object> parameters;


    Configurer(@NotNull Supplier<M> message)
    {
      this.message = message;

      locale = MessageSupportImpl.this.locale;
      parameters = new SortedStringMap<>();
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull M getMessage() {
      return requireNonNull(message.get(), "message must not be null");
    }


    /** {@inheritDoc} */
    @Override
    public @Unmodifiable @NotNull Map<String,Object> getParameters() {
      return parameters;
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull MessageConfigurer<M> clear()
    {
      parameters.clear();

      return this;
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull MessageConfigurer<M> remove(@NotNull String parameter)
    {
      if (!requireNonNull(parameter, "parameter must not be null").isEmpty())
        parameters.remove(parameter);

      return this;
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull MessageConfigurer<M> with(@NotNull String parameter, Object value)
    {
      if (!isKebabOrLowerCamelCaseName(validateName(parameter, "parameter name")))
      {
        throw new IllegalArgumentException("parameter name '" + parameter +
            "' must match the camel- or kebab-case naming convention");
      }

      parameters.put(parameter, value);

      return this;
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull MessageConfigurer<M> locale(Locale locale)
    {
      this.locale = locale == null ? MessageSupportImpl.this.locale : locale;
      return this;
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull String format() {
      return getMessage().format(messageAccessor, new MessageParameters(this));
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull Supplier<String> formatSupplier()
    {
      // as formatting is deferred, make sure we're using a copy of the parameters
      var parameters = new MessageParameters(this);

      return SupplierDelegate.of(() -> getMessage().format(messageAccessor, parameters));
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull <X extends Exception> X formattedException(
        @NotNull ExceptionConstructorWithCause<X> constructor, Throwable cause) {
      return constructor.construct(format(), cause);
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull <X extends Exception> X formattedException(@NotNull ExceptionConstructor<X> constructor) {
      return constructor.construct(format());
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull <X extends Exception> Supplier<X> formattedExceptionSupplier(
        @NotNull ExceptionConstructorWithCause<X> constructor, Throwable cause)
    {
      // as formatting is deferred, make sure we're using a copy of the parameters
      final var parameters = new MessageParameters(this);

      return SupplierDelegate.of(() -> constructor.construct(getMessage().format(messageAccessor, parameters), cause));
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull <X extends Exception> Supplier<X> formattedExceptionSupplier(
        @NotNull ExceptionConstructor<X> constructor)
    {
      // as formatting is deferred, make sure we're using a copy of the parameters
      final var parameters = new MessageParameters(this);

      return () -> constructor.construct(getMessage().format(messageAccessor, parameters));
    }
  }




  /**
   * Internal {@link MessageAccessor} implementation providing read-only access to the messages, templates,
   * formatters and default configuration managed by the enclosing {@link MessageSupportImpl}.
   */
  public final class Accessor implements MessageAccessor
  {
    /** {@inheritDoc} */
    @Override
    public @NotNull MessageFactory getMessageFactory() {
      return messageFactory;
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull Locale getLocale() {
      return locale;
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull @UnmodifiableView Set<String> getMessageCodes() {
      return unmodifiableSet(messages.keySet());
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull @UnmodifiableView Set<String> getTemplateNames() {
      return unmodifiableSet(templates.keySet());
    }


    /** {@inheritDoc} */
    @Override
    public Message getTemplateByName(@NotNull String name) {
      return templates.get(name);
    }


    /** {@inheritDoc} */
    @Override
    public boolean hasMessageWithCode(String code) {
      return code != null && messages.containsKey(code);
    }


    /** {@inheritDoc} */
    @Override
    public Message.WithCode getMessageByCode(@NotNull String code) {
      return messages.get(code);
    }


    /** {@inheritDoc} */
    @Override
    public boolean hasTemplateWithName(String name) {
      return name != null && templates.containsKey(name);
    }


    /** {@inheritDoc} */
    @Override
    public TypedValue<?> getDefaultConfig(@NotNull String name) {
      return defaultConfig.get(name);
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull ParameterFormatter[] getFormatters(String format, @NotNull Class<?> type,
                                                       MessagePart.Config config) {
      return formatterService.getFormatters(format, type, config);
    }


    /** {@inheritDoc} */
    @Override
    public PostFormatter getPostFormatter(@NotNull String postFormatterName) {
      return formatterService.getPostFormatters().get(postFormatterName);
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull Set<String> findMissingTemplates(Predicate<String> messageCodeFilter)
    {
      return messages
          .values()
          .stream()
          .filter(message -> messageCodeFilter == null || messageCodeFilter.test(message.getCode()))
          .flatMap(message -> message.getTemplateNames().stream())
          .distinct()
          .filter(templateName -> !templates.containsKey(templateName))
          .collect(toCollection(TreeSet::new));
    }
  }
}
