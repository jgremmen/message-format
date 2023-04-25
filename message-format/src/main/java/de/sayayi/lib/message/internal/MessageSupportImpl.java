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
import de.sayayi.lib.message.parameter.value.*;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Collections.unmodifiableSet;
import static java.util.Collections.unmodifiableSortedSet;
import static java.util.Locale.forLanguageTag;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;


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
  private final @NotNull MessageSupportAccessor accessor;

  private @NotNull Locale locale;
  private @NotNull MessageFilter messageFilter;
  private @NotNull TemplateFilter templateFilter;


  public MessageSupportImpl(@NotNull FormatterService formatterService,
                            @NotNull MessageFactory messageFactory)
  {
    this.formatterService =
        requireNonNull(formatterService, "formatterService must not be null");
    this.messageFactory =
        requireNonNull(messageFactory, "messageFactory must not be null");

    accessor = new Accessor();
    locale = Locale.getDefault();
    messageFilter = this::failOnDuplicateMessage;
    templateFilter = this::failOnDuplicateTemplate;
  }


  @Override
  public @NotNull MessageSupportAccessor getAccessor() {
    return accessor;
  }


  @Override
  public @NotNull ConfigurableMessageSupport setLocale(@NotNull Locale locale)
  {
    this.locale = requireNonNull(locale, "locale must not be null");
    return this;
  }


  @Override
  public @NotNull ConfigurableMessageSupport setLocale(@NotNull String locale) {
    return setLocale(forLanguageTag(locale));
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
  public void addMessage(@NotNull Message.WithCode message)
  {
    final String code = requireNonNull(message, "message must not be null").getCode();

    if (messageFilter.filter(message))
      messages.put(code, message);
  }


  @Override
  public void addTemplate(@NotNull String name, @NotNull Message template)
  {
    if (requireNonNull(name, "name must not be null").isEmpty())
      throw new IllegalArgumentException("name must not be empty");

    if (templateFilter.filter(name, template))
      templates.put(name, requireNonNull(template));
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
          addTemplate(requireNonNull(dataStream.readString()),
              packHelper.unpackMessageWithSpaces(dataStream));
      }
    }

    return this;
  }


  @Override
  public void exportMessages(@NotNull OutputStream stream, boolean compress,
                             Predicate<String> messageCodeFilter) throws IOException
  {
    if (messageCodeFilter == null)
      messageCodeFilter = c -> true;

    try(final PackOutputStream dataStream = new PackOutputStream(stream, compress)) {
      final List<String> messageCodes = messages.keySet()
          .stream()
          .filter(messageCodeFilter)
          .sorted()
          .collect(toList());
      final Set<String> templateNames = new TreeSet<>();

      // pack all filtered messages
      dataStream.writeUnsignedShort(messageCodes.size());
      for(final String code: messageCodes)
      {
        final Message message = messages.get(code);

        templateNames.addAll(message.getTemplateNames());
        PackHelper.pack(message, dataStream);
      }

      // pack all required templates (if available)
      dataStream.writeUnsignedShort(templateNames.size());
      for(final String templateName: templateNames)
      {
        final Message template = templates.get(templateName);
        if (template != null)
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

    return new Configurer<>(message);
  }


  @Override
  public @NotNull MessageConfigurer<Message> message(@NotNull String message) {
    return message(messageFactory.parseMessage(message));
  }


  @Override
  public <M extends Message> @NotNull MessageConfigurer<M> message(@NotNull M message) {
    return new Configurer<>(requireNonNull(message, "message must not be null"));
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
      if (ttm.isSame(template))
      {
        throw new DuplicateTemplateException(name,
            "different template with identical name '" + name + "' already exists");
      }

      return false;
    }

    return true;
  }




  private final class Configurer<M extends Message> implements MessageConfigurer<M>
  {
    private final @NotNull M message;
    private final SortedMap<String,Object> parameterValues;
    private @NotNull Locale locale;


    private Configurer(@NotNull M message)
    {
      this.message = message;

      parameterValues = new TreeMap<>();
      locale = MessageSupportImpl.this.locale;
    }


    @Override
    public @NotNull M getMessage() {
      return message;
    }


    @Override
    public @NotNull MessageConfigurer<M> clear()
    {
      parameterValues.clear();
      return this;
    }


    @Override
    public @NotNull MessageConfigurer<M> remove(@NotNull String parameter)
    {
      parameterValues.remove(requireNonNull(parameter, "parameter must not be null"));
      return this;
    }


    @Override
    public @NotNull MessageConfigurer<M> with(@NotNull String parameter, Object value)
    {
      if (requireNonNull(parameter, "parameter must not be null").isEmpty())
        throw new IllegalArgumentException("parameter must not be empty");

      parameterValues.put(parameter, value);
      return this;
    }


    @Override
    public @NotNull MessageConfigurer<M> locale(Locale locale)
    {
      this.locale = locale == null ? MessageSupportImpl.this.locale : locale;
      return this;
    }


    @Override
    public @NotNull MessageConfigurer<M> locale(String locale)
    {
      this.locale = locale == null ? MessageSupportImpl.this.locale : forLanguageTag(locale);
      return this;
    }


    @Override
    public @NotNull String format()
    {
      return message.format(getAccessor(), new Parameters() {
        @Override
        public @NotNull Locale getLocale() {
          return locale;
        }


        @Override
        public Object getParameterValue(@NotNull String parameter) {
          return parameterValues.get(parameter);
        }


        @Override
        public @NotNull SortedSet<String> getParameterNames() {
          return unmodifiableSortedSet(new TreeSet<>(parameterValues.keySet()));
        }
      });
    }


    @Override
    @SneakyThrows(Exception.class)
    public <T extends Exception> void throwFormatted(@NotNull Function<String,T> constructor) {
      throw constructor.apply(format());
    }
  }




  private final class Accessor implements MessageSupportAccessor
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
    public @NotNull List<ParameterFormatter> getFormatters(String format, @NotNull Class<?> type) {
      return formatterService.getFormatters(format, type);
    }
  }
}
