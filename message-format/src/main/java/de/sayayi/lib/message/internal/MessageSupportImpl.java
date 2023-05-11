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
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
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
  public void addMessage(@NotNull Message.WithCode message)
  {
    if (messageFilter.filter(requireNonNull(message, "message must not be null")))
      messages.put(message.getCode(), message);
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
        PackHelper.pack(templates.get(templateName), dataStream);
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
    private final @NotNull Supplier<M> message;
    private @NotNull Locale locale;
    private @NotNull Object[] parameters;
    private int parameterCount;


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
      return getMessage().format(messageAccessor, new Params(this));
    }


    @Override
    public @NotNull Supplier<String> formatSupplier() {
      return this::format;
    }


    @Override
    @SneakyThrows(Exception.class)
    public <T extends Exception> void throwFormatted(@NotNull Function<String,T> constructor) {
      throw constructor.apply(format());
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
    public @NotNull List<ParameterFormatter> getFormatters(String format, @NotNull Class<?> type) {
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




  private static final class Params implements Parameters
  {
    private final Configurer<?> configurer;
    private final Object[] parameters;


    private Params(@NotNull Configurer<?> configurer) {
      parameters = (this.configurer = configurer).parameters;
    }


    @Override
    public @NotNull Locale getLocale() {
      return configurer.locale;
    }


    @Override
    public Object getParameterValue(@NotNull String parameter)
    {
      for(int low = 0, high = configurer.parameterCount - 1; low <= high;)
      {
        final int mid = (low + high) >>> 1;
        final int cmp = parameter.compareTo((String)parameters[mid * 2]);

        if (cmp < 0)
          high = mid - 1;
        else if (cmp > 0)
          low = mid + 1;
        else
          return parameters[mid * 2 + 1];
      }

      return null;
    }


    @Override
    public @NotNull SortedSet<String> getParameterNames() {
      return new ParameterNameSet(configurer);
    }


    @Override
    public String toString()
    {
      final StringBuilder s = new StringBuilder("Parameters(locale='")
          .append(configurer.locale).append("',{");

      for(int n = 0, l = configurer.parameterCount * 2; n < l; n += 2)
      {
        if (n > 0)
          s.append(',');

        s.append(parameters[n]).append("=").append(parameters[n + 1]);
      }

      return s.append("})").toString();
    }
  }




  private static final class ParameterNameSet extends AbstractSet<String>
      implements SortedSet<String>
  {
    private final Configurer<?> configurer;


    private ParameterNameSet(@NotNull Configurer<?> configurer) {
      this.configurer = configurer;
    }


    @Override
    public int size() {
      return configurer.parameterCount;
    }


    @Override
    public @Nullable Comparator<String> comparator() {
      return null;
    }


    @Override
    public @NotNull SortedSet<String> subSet(String fromElement, String toElement) {
      throw new UnsupportedOperationException("subSet");
    }


    @Override
    public @NotNull SortedSet<String> headSet(String toElement) {
      throw new UnsupportedOperationException("headSet");
    }


    @Override
    public @NotNull SortedSet<String> tailSet(String fromElement) {
      throw new UnsupportedOperationException("tailSet");
    }


    @Override
    public String first() {
      return configurer.parameterCount == 0 ? null : (String)configurer.parameters[0];
    }


    @Override
    public String last()
    {
      return configurer.parameterCount == 0
          ? null : (String)configurer.parameters[(configurer.parameterCount - 1) * 2];
    }


    @Override
    public void clear() {
      throw new UnsupportedOperationException("clear");
    }


    @Override
    public @NotNull Iterator<String> iterator() {
      return new ParameterNameIterator(configurer);
    }


    @Override
    public @NotNull Spliterator<String> spliterator() {
      return new ParameterNameSpliterator(configurer);
    }
  }




  private static final class ParameterNameIterator implements Iterator<String>
  {
    private final Object[] parameters;
    private final int parameterCount;
    private int n;


    private ParameterNameIterator(@NotNull Configurer<?> configurer)
    {
      parameters = configurer.parameters;
      parameterCount = configurer.parameterCount;
    }


    @Override
    public boolean hasNext() {
      return n < parameterCount;
    }


    @Override
    public String next()
    {
      if (!hasNext())
        throw new NoSuchElementException();

      return (String)parameters[n++ * 2];
    }
  }




  private static final class ParameterNameSpliterator implements Spliterator<String>
  {
    private final Object[] parameters;
    private final int parameterCount;
    private int n;


    private ParameterNameSpliterator(@NotNull Configurer<?> configurer)
    {
      parameters = configurer.parameters;
      parameterCount = configurer.parameterCount;
    }


    @Override
    public boolean tryAdvance(Consumer<? super String> action)
    {
      if (n < parameterCount)
      {
        action.accept((String)parameters[n++ * 2]);
        return true;
      }

      return false;
    }


    @Override
    public Spliterator<String> trySplit() {
      return null;
    }


    @Override
    public long estimateSize() {
      return parameterCount - n;
    }


    @Override
    public int characteristics() {
      return ORDERED | DISTINCT | IMMUTABLE | NONNULL | SORTED | SIZED;
    }


    @Override
    public Comparator<? super String> getComparator() {
      return null;
    }
  }
}
