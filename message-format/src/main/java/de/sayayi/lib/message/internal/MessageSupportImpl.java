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
import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.exception.DuplicateMessageException;
import de.sayayi.lib.message.exception.DuplicateTemplateException;
import de.sayayi.lib.message.formatter.FormatterService;
import de.sayayi.lib.message.formatter.ParameterFormatter;
import de.sayayi.lib.message.pack.PackHelper;
import de.sayayi.lib.message.pack.PackInputStream;
import de.sayayi.lib.message.pack.PackOutputStream;
import de.sayayi.lib.message.part.parameter.value.*;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.lang.System.arraycopy;
import static java.util.Arrays.copyOf;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public class MessageSupportImpl implements MessageSupport.ConfigurableMessageSupport
{
  private final @NotNull FormatterService formatterService;
  private final @NotNull MessageFactory messageFactory;
  private final @NotNull Map<String,ConfigValue> defaultParameterConfig = new TreeMap<>();
  private final @NotNull Map<String,Message.WithCode> messages = new TreeMap<>();
  private final @NotNull Map<String,Message> templates = new TreeMap<>();
  private final @NotNull MessageAccessor messageAccessor;

  private @NotNull Locale locale;
  private @NotNull MessageFilter messageFilter;
  private @NotNull TemplateFilter templateFilter;


  public MessageSupportImpl(@NotNull FormatterService formatterService,
                            @NotNull MessageFactory messageFactory)
  {
    this.formatterService = requireNonNull(formatterService,
        "formatterService must not be null");
    this.messageFactory = requireNonNull(messageFactory,
        "messageFactory must not be null");

    messageAccessor = new Accessor();
    locale = Locale.getDefault();
    messageFilter = this::failOnDuplicateMessage;
    templateFilter = this::failOnDuplicateTemplate;
  }


  @Override
  public @NotNull MessageAccessor getMessageAccessor() {
    return messageAccessor;
  }


  @Override
  public @NotNull ConfigurableMessageSupport setLocale(@NotNull Locale locale)
  {
    this.locale = requireNonNull(locale, "locale must not be null");
    return this;
  }


  @Override
  public @NotNull ConfigurableMessageSupport setDefaultParameterConfig(@NotNull String name,
                                                                       boolean value)
  {
    if (requireNonNull(name, "name must not be null").isEmpty())
      throw new IllegalArgumentException("name must not be empty");

    defaultParameterConfig.put(name, value ? ConfigValueBool.TRUE : ConfigValueBool.FALSE);
    return this;
  }


  @Override
  public @NotNull ConfigurableMessageSupport setDefaultParameterConfig(@NotNull String name,
                                                                       long value)
  {
    if (requireNonNull(name, "name must not be null").isEmpty())
      throw new IllegalArgumentException("name must not be empty");

    defaultParameterConfig.put(name, new ConfigValueNumber(value));
    return this;
  }


  @Override
  public @NotNull ConfigurableMessageSupport setDefaultParameterConfig(@NotNull String name,
                                                                       @NotNull String value)
  {
    if (requireNonNull(name, "name must not be null").isEmpty())
      throw new IllegalArgumentException("name must not be empty");

    defaultParameterConfig.put(name, new ConfigValueString(value));
    return this;
  }


  @Override
  public @NotNull ConfigurableMessageSupport setDefaultParameterConfig(
      @NotNull String name, @NotNull Message.WithSpaces value)
  {
    if (requireNonNull(name, "name must not be null").isEmpty())
      throw new IllegalArgumentException("name must not be empty");

    defaultParameterConfig.put(name, new ConfigValueMessage(value));
    return this;
  }


  @Override
  public @NotNull ConfigurableMessageSupport setMessageFilter(@NotNull MessageFilter messageFilter)
  {
    this.messageFilter = requireNonNull(messageFilter, "messageFilter must not be null");
    return this;
  }


  @Override
  public @NotNull ConfigurableMessageSupport setTemplateFilter(
      @NotNull TemplateFilter templateFilter)
  {
    this.templateFilter = requireNonNull(templateFilter, "templateFilter must not be null");
    return this;
  }


  @Override
  public @NotNull ConfigurableMessageSupport addMessage(@NotNull Message.WithCode message)
  {
    if (messageFilter.filter(requireNonNull(message, "message must not be null")))
      messages.put(message.getCode(), message);

    return this;
  }


  @Override
  public @NotNull ConfigurableMessageSupport addTemplate(@NotNull String name,
                                                         @NotNull Message template)
  {
    if (requireNonNull(name, "name must not be null").isEmpty())
      throw new IllegalArgumentException("name must not be empty");

    if (templateFilter.filter(name, template))
      templates.put(name, requireNonNull(template));

    return this;
  }


  @Override
  public @NotNull ConfigurableMessageSupport importMessages(@NotNull Enumeration<URL> packResources)
      throws IOException
  {
    requireNonNull(packResources, "packResources must not be null");

    final List<InputStream> packStreams = new ArrayList<>();

    while(packResources.hasMoreElements())
      packStreams.add(packResources.nextElement().openStream());

    return importMessages(packStreams.toArray(new InputStream[0]));
  }


  @Override
  public @NotNull ConfigurableMessageSupport importMessages(@NotNull InputStream... packStreams)
      throws IOException
  {
    requireNonNull(packStreams, "packStreams must not be null");

    final PackHelper packHelper = new PackHelper();

    for(final InputStream packStream: packStreams)
    {
      try(final PackInputStream dataStream = new PackInputStream(packStream)) {
        // messages
        for(int n = 0, size = dataStream.readUnsignedShort(); n < size; n++)
          addMessage(packHelper.unpackMessageWithCode(dataStream));

        // templates
        for(int n = 0, size = dataStream.readUnsignedShort(); n < size; n++)
        {
          addTemplate(requireNonNull(dataStream.readString()),
              packHelper.unpackMessageWithSpaces(dataStream));
        }
      }
    }

    return this;
  }


  @Override
  public void exportMessages(@NotNull OutputStream stream, boolean compress,
                             Predicate<String> messageCodeFilter) throws IOException
  {
    try(final PackOutputStream dataStream = new PackOutputStream(stream, compress)) {
      final Set<String> messageCodes = new TreeSet<>(messages.keySet());
      final Set<String> templateNames = new TreeSet<>();

      // filter message codes
      if (messageCodeFilter != null)
        messageCodes.removeIf(code -> !messageCodeFilter.test(code));

      // pack all filtered messages
      dataStream.writeUnsignedShort(messageCodes.size());
      for(final String code: messageCodes)
      {
        final Message message = messages.get(code);

        templateNames.addAll(message.getTemplateNames());
        PackHelper.pack(message, dataStream);
      }

      // pack all required templates
      templateNames.removeIf(templateName -> !templates.containsKey(templateName));
      dataStream.writeUnsignedShort(templateNames.size());
      for(final String templateName: templateNames)
      {
        dataStream.writeString(templateName);
        PackHelper.pack(templates.get(templateName), dataStream);
      }
    }
  }


  @Override
  public @NotNull MessageConfigurer<Message.WithCode> code(@NotNull String code)
  {
    if (requireNonNull(code, "code must not be null").isEmpty())
      throw new IllegalArgumentException("code must not be empty");

    final Message.WithCode message = messages.get(code);
    if (message == null)
      throw new IllegalArgumentException("unknown message code '" + code + "'");

    return new Configurer<>(() -> message);
  }


  @Override
  public @NotNull MessageConfigurer<Message> message(@NotNull String message) {
    return new Configurer<>(() -> messageFactory.parseMessage(message));
  }


  @Override
  public <M extends Message> @NotNull MessageConfigurer<M> message(@NotNull M message)
  {
    requireNonNull(message, "message must not be null");

    return new Configurer<>(() -> message);
  }


  protected boolean failOnDuplicateMessage(@NotNull Message.WithCode message)
  {
    final String code = message.getCode();
    final Message tm = messages.get(code);

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


  protected boolean failOnDuplicateTemplate(@NotNull String name, @NotNull Message template)
  {
    final Message ttm = templates.get(name);

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




  final class Configurer<M extends Message> implements MessageConfigurer<M>
  {
    private final @NotNull Supplier<M> message;
    @NotNull Locale locale;
    @NotNull Object[] parameters;
    int parameterCount;


    private Configurer(@NotNull Supplier<M> message)
    {
      this.message = message;

      locale = MessageSupportImpl.this.locale;
      parameters = new Object[16];
    }


    @Override
    public @NotNull M getMessage() {
      return requireNonNull(message.get(), "message must not be null");
    }


    @Override
    public @NotNull MessageConfigurer<M> clear()
    {
      parameterCount = 0;
      return this;
    }


    @Override
    public @NotNull MessageConfigurer<M> remove(@NotNull String parameter)
    {
      if (!requireNonNull(parameter, "parameter must not be null").isEmpty())
        for(int low = 0, high = parameterCount - 1; low <= high;)
        {
          final int mid = (low + high) >>> 1;
          final int cmp = parameter.compareTo((String)parameters[mid * 2]);

          if (cmp < 0)
            high = mid - 1;
          else if (cmp > 0)
            low = mid + 1;
          else
          {
            final int mid2 = mid * 2;
            arraycopy(parameters, mid2 + 2, parameters, mid2,
                --parameterCount * 2 - mid2);
            break;
          }
        }

      return this;
    }


    @Override
    public @NotNull MessageConfigurer<M> with(@NotNull String parameter, Object value)
    {
      if (requireNonNull(parameter, "parameter must not be null").isEmpty())
        throw new IllegalArgumentException("parameter must not be empty");

      setValue: {
        int low = 0;

        for(int high = parameterCount - 1; low <= high;)
        {
          final int mid = (low + high) >>> 1;
          final int cmp = parameter.compareTo((String)parameters[mid * 2]);

          if (cmp < 0)
            high = mid - 1;
          else if (cmp > 0)
            low = mid + 1;
          else
          {
            parameters[mid * 2 + 1] = value;  // overwrite current value
            break setValue;
          }
        }

        if (parameterCount * 2 == parameters.length)
          parameters = copyOf(parameters, parameterCount * 2 + 16);

        final int offset = low * 2;
        arraycopy(parameters, offset, parameters, offset + 2,
            parameterCount++ * 2 - offset);

        parameters[offset] = parameter;
        parameters[offset + 1] = value;
      }

      return this;
    }


    @Override
    public @NotNull MessageConfigurer<M> locale(Locale locale)
    {
      this.locale = locale == null ? MessageSupportImpl.this.locale : locale;
      return this;
    }


    @Override
    public @NotNull String format() {
      return getMessage().format(messageAccessor, new MessageParameters(this));
    }


    @Override
    public @NotNull Supplier<String> formatSupplier()
    {
      // as formatting is deferred, make sure we're using a copy of the parameters
      final Parameters parameters = new MessageParameters(this);

      return () -> getMessage().format(messageAccessor, parameters);
    }


    @Override
    @SneakyThrows(Exception.class)
    public <X extends Exception> void throwFormatted(
        @NotNull ExceptionConstructorWithCause<X> constructor, Throwable cause) {
      throw constructor.construct(format(), cause);
    }


    @Override
    @SneakyThrows(Exception.class)
    public <X extends Exception> void throwFormatted(@NotNull ExceptionConstructor<X> constructor) {
      throw constructor.construct(format());
    }


    @Override
    public @NotNull <X extends Exception> Supplier<X> formattedExceptionSupplier(
        @NotNull ExceptionConstructorWithCause<X> constructor, Throwable cause)
    {
      // as formatting is deferred, make sure we're using a copy of the parameters
      final Parameters parameters = new MessageParameters(this);

      return () -> constructor.construct(getMessage().format(messageAccessor, parameters), cause);
    }


    @Override
    public @NotNull <X extends Exception> Supplier<X> formattedExceptionSupplier(
        @NotNull ExceptionConstructor<X> constructor)
    {
      // as formatting is deferred, make sure we're using a copy of the parameters
      final Parameters parameters = new MessageParameters(this);

      return () -> constructor.construct(getMessage().format(messageAccessor, parameters));
    }
  }




  private final class Accessor implements MessageAccessor
  {
    @Override
    public @NotNull MessageFactory getMessageFactory() {
      return messageFactory;
    }


    @Override
    public @NotNull Locale getLocale() {
      return locale;
    }


    @Override
    public @NotNull Set<String> getMessageCodes() {
      return unmodifiableSet(messages.keySet());
    }


    @Override
    public @NotNull Set<String> getTemplateNames() {
      return unmodifiableSet(templates.keySet());
    }


    @Override
    public Message getTemplateByName(@NotNull String name) {
      return templates.get(name);
    }


    @Override
    public boolean hasMessageWithCode(String code) {
      return code != null && messages.containsKey(code);
    }


    @Override
    public Message.WithCode getMessageByCode(@NotNull String code) {
      return messages.get(code);
    }


    @Override
    public boolean hasTemplateWithName(String name) {
      return name != null && templates.containsKey(name);
    }


    @Override
    public ConfigValue getDefaultParameterConfig(@NotNull String name) {
      return defaultParameterConfig.get(name);
    }


    @Override
    public @NotNull ParameterFormatter[] getFormatters(String format, @NotNull Class<?> type) {
      return formatterService.getFormatters(format, type);
    }


    @Override
    public @NotNull Set<String> findMissingTemplates(Predicate<String> messageCodeFilter)
    {
      return messages.values()
          .stream()
          .filter(message -> messageCodeFilter == null || messageCodeFilter.test(message.getCode()))
          .flatMap(message -> message.getTemplateNames().stream())
          .distinct()
          .filter(templateName -> !templates.containsKey(templateName))
          .collect(toCollection(TreeSet::new));
    }
  }
}
