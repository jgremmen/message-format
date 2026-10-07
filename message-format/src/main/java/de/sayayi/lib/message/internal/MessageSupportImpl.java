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
import de.sayayi.lib.message.template.NamedTemplate;
import de.sayayi.lib.message.template.Template;
import de.sayayi.lib.message.util.SortedStringMap;
import de.sayayi.lib.message.util.SupplierDelegate;
import de.sayayi.lib.pack.PackOutputStream;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static de.sayayi.lib.message.internal.pack.PackSupport.VERSION;
import static de.sayayi.lib.message.pack.PackConstants.PACK_CONFIG;
import static de.sayayi.lib.message.util.MessageUtil.*;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;


/**
 * Core implementation of {@link MessageSupport.ConfigurableMessageSupport}.
 * <p>
 * This class stores registered messages, templates and default configuration values for a message support instance.
 * It exposes that state through the nested {@link Accessor} and creates {@link Configurer} instances for individual
 * formatting operations.
 * <p>
 * Duplicate messages and templates are handled by configurable filters. By default, adding a message or template with
 * an existing code or name throws a {@link DuplicateMessageException} or {@link DuplicateTemplateException}.
 * <p>
 * This class is thread-safe. All mutable state (locale, default configuration, messages, templates and filters) is
 * guarded by a read/write lock, allowing concurrent read access while serializing modifications.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@ApiStatus.Internal
public final class MessageSupportImpl implements MessageSupport.ConfigurableMessageSupport
{
  /** Guards concurrent access to the mutable state (default config, messages, templates, locale, filters). */
  private final @NotNull ReadWriteLock lock = new ReentrantReadWriteLock();

  /** Formatter registry used to resolve parameter and post formatters while formatting messages. */
  private final @NotNull FormatterService formatterService;

  /** Factory used to parse literal message definitions and create typed message representations. */
  private final @NotNull MessageFactory messageFactory;

  /** Default configuration values exposed through the message accessor to message parts during formatting. */
  private final @NotNull Map<String,TypedValue<?>> defaultConfig = new TreeMap<>();

  /** Registered messages keyed by their unique message code. */
  private final @NotNull Map<String,Message.WithCode> messages = new TreeMap<>();

  /** Registered templates keyed by their kebab-case template name. */
  private final @NotNull Map<String,Template> templates = new TreeMap<>();

  /** Shared accessor instance exposing the current support state to messages and related infrastructure. */
  private final @NotNull MessageAccessor messageAccessor;

  /** Default locale used for new formatting operations unless a configurer overrides it. */
  private @NotNull Locale locale;

  /** Strategy deciding whether a newly added message should be accepted when its code already exists. */
  private @NotNull MessageFilter messageFilter;

  /** Strategy deciding whether a newly added template should be accepted when its name already exists. */
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
    requireNonNull(locale, "locale must not be null");

    final var writeLock = lock.writeLock();

    writeLock.lock();
    try {
      this.locale = locale;
    } finally {
      writeLock.unlock();
    }

    return this;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull ConfigurableMessageSupport setDefaultConfig(@NotNull String name, boolean value)
  {
    validateName(name, "config name");

    final var writeLock = lock.writeLock();

    writeLock.lock();
    try {
      defaultConfig.put(name, value ? TypedValueBool.TRUE : TypedValueBool.FALSE);
    } finally {
      writeLock.unlock();
    }

    return this;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull ConfigurableMessageSupport setDefaultConfig(@NotNull String name, long value)
  {
    validateName(name, "config name");

    final var writeLock = lock.writeLock();

    writeLock.lock();
    try {
      defaultConfig.put(name, new TypedValueNumber(value));
    } finally {
      writeLock.unlock();
    }

    return this;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull ConfigurableMessageSupport setDefaultConfig(@NotNull String name, @NotNull String value)
  {
    validateName(name, "config name");

    final var writeLock = lock.writeLock();

    writeLock.lock();
    try {
      defaultConfig.put(name, new TypedValueString(messageFactory, value));
    } finally {
      writeLock.unlock();
    }

    return this;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull ConfigurableMessageSupport setDefaultConfig(@NotNull String name, @NotNull Message.WithSpaces value)
  {
    validateName(name, "config name");

    final var writeLock = lock.writeLock();

    writeLock.lock();
    try {
      defaultConfig.put(name, new TypedValueMessage(value));
    } finally {
      writeLock.unlock();
    }

    return this;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull ConfigurableMessageSupport setMessageFilter(@NotNull MessageFilter messageFilter)
  {
    requireNonNull(messageFilter, "messageFilter must not be null");

    final var writeLock = lock.writeLock();

    writeLock.lock();
    try {
      this.messageFilter = messageFilter;
    } finally {
      writeLock.unlock();
    }

    return this;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull ConfigurableMessageSupport setTemplateFilter(@NotNull TemplateFilter templateFilter)
  {
    requireNonNull(templateFilter, "templateFilter must not be null");

    final var writeLock = lock.writeLock();

    writeLock.lock();
    try {
      this.templateFilter = templateFilter;
    } finally {
      writeLock.unlock();
    }

    return this;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull ConfigurableMessageSupport addMessage(@NotNull Message.WithCode message)
  {
    requireNonNull(message, "message must not be null");

    final var writeLock = lock.writeLock();

    writeLock.lock();
    try {
      if (messageFilter.filter(message))
        messages.put(message.getCode(), message);
    } finally {
      writeLock.unlock();
    }

    return this;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull ConfigurableMessageSupport addTemplate(@NotNull String name, @NotNull Template template)
  {
    if (!isKebabCaseName(validateName(name, "template name")))
      throw new IllegalArgumentException("template name '" + name + "' must match the kebab-case naming convention");

    requireNonNull(template);

    final var writeLock = lock.writeLock();

    writeLock.lock();
    try {
      if (templateFilter.filter(name, template))
        templates.put(name, template);
    } finally {
      writeLock.unlock();
    }

    return this;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull ConfigurableMessageSupport registerTemplatesFromService(ClassLoader classLoader)
  {
    ServiceLoader
        .load(NamedTemplate.class, classLoader)
        .forEach(template -> addTemplate(template.getName(), template));

    return this;
  }


  /** {@inheritDoc} */
  @Override
  public void exportMessages(@NotNull OutputStream stream, boolean compress,
                             Predicate<String> messageCodeFilter, Predicate<String> templateNameFilter)
      throws IOException
  {
    final var readLock = lock.readLock();
    final Map<String,Message.WithCode> messageSnapshot;
    final Map<String,Template> templateSnapshot;

    readLock.lock();
    try {
      messageSnapshot = new TreeMap<>(messages);
      templateSnapshot = new TreeMap<>(templates);
    } finally {
      readLock.unlock();
    }

    try(var dataStream = new PackOutputStream(PACK_CONFIG, VERSION, compress, stream)) {
      final var messageCodes = new TreeSet<>(messageSnapshot.keySet());
      final var templateNames = new TreeSet<String>();

      // filter message codes
      if (messageCodeFilter != null)
        messageCodes.removeIf(messageCodeFilter.negate());

      // pack all filtered messages
      dataStream.writeUnsignedShort(messageCodes.size());
      for(var code: messageCodes)
      {
        final var message = messageSnapshot.get(code);

        templateNames.addAll(message.getTemplateNames());
        PackSupport.pack(message, dataStream);
      }

      // pack all required templates
      templateNames.removeIf(templateName -> !(templateSnapshot.get(templateName) instanceof MessageTemplate));
      if (templateNameFilter != null)
        templateNames.removeIf(templateNameFilter.negate());

      dataStream.writeUnsignedShort(templateNames.size());
      for(var templateName: templateNames)
      {
        dataStream.writeString(templateName);
        PackSupport.pack(((MessageTemplate)templateSnapshot.get(templateName)).getMessage(), dataStream);
      }
    }
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MessageConfigurer<Message.WithCode> code(@NotNull String code)
  {
    validateName(code, "message code");

    final var readLock = lock.readLock();
    final Message.WithCode message;

    readLock.lock();
    try {
      message = messages.get(code);
    } finally {
      readLock.unlock();
    }

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
   * Returns the current default locale, guarded by this instance's lock.
   *
   * @return  current default locale, never {@code null}
   */
  private @NotNull Locale getLocale()
  {
    final var readLock = lock.readLock();

    readLock.lock();
    try {
      return locale;
    } finally {
      readLock.unlock();
    }
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
  private boolean failOnDuplicateTemplate(@NotNull String name, @NotNull Template template)
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
   * <p>
   * Instances are lightweight and intended for use by a single thread; they are not thread-safe and should not be
   * shared between formatting operations.
   *
   * @param <M>  the message type this configurer operates on
   */
  public final class Configurer<M extends Message> implements MessageConfigurer<M>
  {
    /** Supplies the message instance to configure and format. */
    private final @NotNull Supplier<M> message;

    /** Locale applied to the current formatting operation. */
    @NotNull Locale locale;

    /** Parameter values collected for the current formatting operation. */
    @NotNull Map<String,Object> parameters;


    /**
     * Creates a configurer for a specific message supplier.
     *
     * @param message  supplier returning the message to configure, not {@code null}
     */
    Configurer(@NotNull Supplier<M> message)
    {
      this.message = message;

      locale = MessageSupportImpl.this.getLocale();
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
      this.locale = locale == null ? MessageSupportImpl.this.getLocale() : locale;
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
   * <p>
   * This class is thread-safe; all accesses to the enclosing instance's state are guarded by its read/write lock.
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
      return MessageSupportImpl.this.getLocale();
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull @UnmodifiableView Set<String> getMessageCodes()
    {
      final var readLock = lock.readLock();

      readLock.lock();
      try {
        return unmodifiableSet(new TreeSet<>(messages.keySet()));
      } finally {
        readLock.unlock();
      }
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull @UnmodifiableView Set<String> getTemplateNames()
    {
      final var readLock = lock.readLock();

      readLock.lock();
      try {
        return unmodifiableSet(new TreeSet<>(templates.keySet()));
      } finally {
        readLock.unlock();
      }
    }


    /** {@inheritDoc} */
    @Override
    public Template getTemplateByName(@NotNull String name)
    {
      final var readLock = lock.readLock();

      readLock.lock();
      try {
        return templates.get(name);
      } finally {
        readLock.unlock();
      }
    }


    /** {@inheritDoc} */
    @Override
    public boolean hasMessageWithCode(String code)
    {
      if (code == null)
        return false;

      final var readLock = lock.readLock();

      readLock.lock();
      try {
        return messages.containsKey(code);
      } finally {
        readLock.unlock();
      }
    }


    /** {@inheritDoc} */
    @Override
    public Message.WithCode getMessageByCode(@NotNull String code)
    {
      final var readLock = lock.readLock();

      readLock.lock();
      try {
        return messages.get(code);
      } finally {
        readLock.unlock();
      }
    }


    /** {@inheritDoc} */
    @Override
    public boolean hasTemplateWithName(String name)
    {
      if (name == null)
        return false;

      final var readLock = lock.readLock();

      readLock.lock();
      try {
        return templates.containsKey(name);
      } finally {
        readLock.unlock();
      }
    }


    /** {@inheritDoc} */
    @Override
    public TypedValue<?> getDefaultConfig(@NotNull String name)
    {
      final var readLock = lock.readLock();

      readLock.lock();
      try {
        return defaultConfig.get(name);
      } finally {
        readLock.unlock();
      }
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
      final var readLock = lock.readLock();
      final List<Message.WithCode> messageSnapshot;
      final Set<String> templateNames;

      readLock.lock();
      try {
        messageSnapshot = new ArrayList<>(messages.values());
        templateNames = new HashSet<>(templates.keySet());
      } finally {
        readLock.unlock();
      }

      return messageSnapshot
          .stream()
          .filter(message -> messageCodeFilter == null || messageCodeFilter.test(message.getCode()))
          .flatMap(message -> message.getTemplateNames().stream())
          .distinct()
          .filter(templateName -> !templateNames.contains(templateName))
          .collect(toCollection(TreeSet::new));
    }
  }
}
