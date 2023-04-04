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
  private @NotNull Predicate<String> messageHandler;
  private @NotNull Predicate<String> templateHandler;


  public MessageSupportImpl(@NotNull FormatterService formatterService,
                            @NotNull MessageFactory messageFactory)
  {
    this.formatterService = requireNonNull(formatterService);
    this.messageFactory = requireNonNull(messageFactory);

    accessor = new Accessor();
    locale = Locale.getDefault();
    messageHandler = this::failOnDuplicateMessage;
    templateHandler = this::failOnDuplicateTemplate;
  }


  @Override
  public @NotNull MessageSupportAccessor getAccessor() {
    return accessor;
  }


  @Override
  public @NotNull ConfigurableMessageSupport setLocale(@NotNull Locale locale)
  {
    this.locale = requireNonNull(locale);
    return this;
  }


  @Override
  public @NotNull ConfigurableMessageSupport setLocale(@NotNull String locale) {
    return setLocale(forLanguageTag(locale));
  }


  @Override
  public @NotNull ConfigurableMessageSupport setDefaultParameterConfig(@NotNull String name, boolean value)
  {
    defaultParameterConfig.put(name, value ? ConfigValueBool.TRUE : ConfigValueBool.FALSE);
    return this;
  }


  @Override
  public @NotNull ConfigurableMessageSupport setDefaultParameterConfig(@NotNull String name, long value)
  {
    defaultParameterConfig.put(name, new ConfigValueNumber(value));
    return this;
  }


  @Override
  public @NotNull ConfigurableMessageSupport setDefaultParameterConfig(@NotNull String name, @NotNull String value)
  {
    defaultParameterConfig.put(name, new ConfigValueString(value));
    return this;
  }


  @Override
  public @NotNull ConfigurableMessageSupport setDefaultParameterConfig(@NotNull String name,
                                                                       @NotNull Message.WithSpaces value)
  {
    defaultParameterConfig.put(name, new ConfigValueMessage(value));
    return this;
  }


  @Override
  public @NotNull ConfigurableMessageSupport setMessageHandler(@NotNull Predicate<String> messageHandler)
  {
    this.messageHandler = requireNonNull(messageHandler);
    return this;
  }


  @Override
  public @NotNull ConfigurableMessageSupport setTemplateHandler(@NotNull Predicate<String> templateHandler)
  {
    this.templateHandler = requireNonNull(templateHandler);
    return this;
  }


  @Override
  public void addMessage(@NotNull Message.WithCode message)
  {
    final String code = requireNonNull(message).getCode();
    if (messageHandler.test(code))
      messages.put(code, message);
  }


  @Override
  public void addTemplate(@NotNull String name, @NotNull Message template)
  {
    if (requireNonNull(name).isEmpty())
      throw new IllegalArgumentException("name must not be empty");

    if (templateHandler.test(name))
      templates.put(name, requireNonNull(template));
  }


  @Override
  public void importMessages(@NotNull Enumeration<URL> packResources) throws IOException
  {
    final List<InputStream> packStreams = new ArrayList<>();

    while(packResources.hasMoreElements())
      packStreams.add(packResources.nextElement().openStream());

    importMessages(packStreams.toArray(new InputStream[0]));
  }


  @Override
  public void importMessages(@NotNull InputStream... packStreams) throws IOException
  {
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
  }


  @Override
  public void exportMessages(@NotNull OutputStream stream, boolean compress, Predicate<String> messageCodeFilter)
      throws IOException
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
    final Message.WithCode message = messages.get(requireNonNull(code));
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
    return new Configurer<>(requireNonNull(message));
  }


  protected boolean failOnDuplicateMessage(@NotNull String code)
  {
    if (messages.containsKey(code))
      throw new DuplicateMessageException(code, "Message with code '" + code + "' already exists");

    return true;
  }


  protected boolean failOnDuplicateTemplate(@NotNull String name)
  {
    if (templates.containsKey(name))
      throw new DuplicateTemplateException(name, "Template with name '" + name + "' already exists");

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
      parameterValues.remove(requireNonNull(parameter));
      return this;
    }


    @Override
    public @NotNull MessageConfigurer<M> with(@NotNull String parameter, Object value)
    {
      if (requireNonNull(parameter).isEmpty())
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
    @SneakyThrows
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
