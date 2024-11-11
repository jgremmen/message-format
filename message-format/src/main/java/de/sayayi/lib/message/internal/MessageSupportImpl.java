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
import de.sayayi.lib.message.util.SupplierDelegate;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.*;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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


  public MessageSupportImpl(@NotNull FormatterService formatterService, @NotNull MessageFactory messageFactory)
  {
    this.formatterService = requireNonNull(formatterService, "formatterService must not be null");
    this.messageFactory = requireNonNull(messageFactory, "messageFactory must not be null");

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
  public @NotNull ConfigurableMessageSupport setDefaultParameterConfig(@NotNull String name, boolean value)
  {
    if (requireNonNull(name, "name must not be null").isEmpty())
      throw new IllegalArgumentException("name must not be empty");

    defaultParameterConfig.put(name, value ? ConfigValueBool.TRUE : ConfigValueBool.FALSE);
    return this;
  }


  @Override
  public @NotNull ConfigurableMessageSupport setDefaultParameterConfig(@NotNull String name, long value)
  {
    if (requireNonNull(name, "name must not be null").isEmpty())
      throw new IllegalArgumentException("name must not be empty");

    defaultParameterConfig.put(name, new ConfigValueNumber(value));
    return this;
  }


  @Override
  public @NotNull ConfigurableMessageSupport setDefaultParameterConfig(@NotNull String name, @NotNull String value)
  {
    if (requireNonNull(name, "name must not be null").isEmpty())
      throw new IllegalArgumentException("name must not be empty");

    defaultParameterConfig.put(name, new ConfigValueString(value));
    return this;
  }


  @Override
  public @NotNull ConfigurableMessageSupport setDefaultParameterConfig(@NotNull String name,
                                                                       @NotNull Message.WithSpaces value)
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
  public @NotNull ConfigurableMessageSupport setTemplateFilter(@NotNull TemplateFilter templateFilter)
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
  public @NotNull ConfigurableMessageSupport addTemplate(@NotNull String name, @NotNull Message template)
  {
    if (requireNonNull(name, "name must not be null").isEmpty())
      throw new IllegalArgumentException("name must not be empty");

    if (templateFilter.filter(name, template))
      templates.put(name, requireNonNull(template));

    return this;
  }


  @Override
  public @NotNull ConfigurableMessageSupport importMessages(@NotNull Enumeration<URL> packResources) throws IOException
  {
    requireNonNull(packResources, "packResources must not be null");

    final List<InputStream> packStreams = new ArrayList<>();

    while(packResources.hasMoreElements())
      packStreams.add(packResources.nextElement().openStream());

    return importMessages(packStreams.toArray(InputStream[]::new));
  }


  @Override
  public @NotNull ConfigurableMessageSupport importMessages(@NotNull InputStream... packStreams) throws IOException
  {
    requireNonNull(packStreams, "packStreams must not be null");

    final PackHelper packHelper = new PackHelper();

    for(var packStream: packStreams)
    {
      try(final PackInputStream dataStream = new PackInputStream(packStream)) {
        // messages
        for(int n = 0, size = dataStream.readUnsignedShort(); n < size; n++)
          addMessage(packHelper.unpackMessageWithCode(dataStream));

        // templates
        for(int n = 0, size = dataStream.readUnsignedShort(); n < size; n++)
          addTemplate(requireNonNull(dataStream.readString()), packHelper.unpackMessageWithSpaces(dataStream));
      }
    }

    return this;
  }


  @Override
  public void exportMessages(@NotNull OutputStream stream, boolean compress, Predicate<String> messageCodeFilter)
      throws IOException
  {
    try(var dataStream = new PackOutputStream(stream, compress)) {
      final Set<String> messageCodes = new TreeSet<>(messages.keySet());
      final Set<String> templateNames = new TreeSet<>();

      // filter message codes
      if (messageCodeFilter != null)
        messageCodes.removeIf(messageCodeFilter.negate());

      // pack all filtered messages
      dataStream.writeUnsignedShort(messageCodes.size());
      for(var code: messageCodes)
      {
        final Message message = messages.get(code);

        templateNames.addAll(message.getTemplateNames());
        PackHelper.pack(message, dataStream);
      }

      // pack all required templates
      templateNames.removeIf(templateName -> !templates.containsKey(templateName));
      dataStream.writeUnsignedShort(templateNames.size());
      for(var templateName: templateNames)
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
    return new Configurer<>(SupplierDelegate.of(() -> messageFactory.parseMessage(message)));
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
    public @NotNull Map<String,Object> getParameters() {
      return new ParameterMap(this);
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
            final int offset = mid * 2;
            arraycopy(parameters, offset + 2, parameters, offset, --parameterCount * 2 - offset);
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
        arraycopy(parameters, offset, parameters, offset + 2, parameterCount++ * 2 - offset);

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

      return SupplierDelegate.of(() -> getMessage().format(messageAccessor, parameters));
    }


    @Override
    public @NotNull <X extends Exception> X formattedException(
        @NotNull ExceptionConstructorWithCause<X> constructor, Throwable cause) {
      return constructor.construct(format(), cause);
    }


    @Override
    public @NotNull <X extends Exception> X formattedException(@NotNull ExceptionConstructor<X> constructor) {
      return constructor.construct(format());
    }


    @Override
    public @NotNull <X extends Exception> Supplier<X> formattedExceptionSupplier(
        @NotNull ExceptionConstructorWithCause<X> constructor, Throwable cause)
    {
      // as formatting is deferred, make sure we're using a copy of the parameters
      final Parameters parameters = new MessageParameters(this);

      return SupplierDelegate.of(() -> constructor.construct(getMessage().format(messageAccessor, parameters), cause));
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




  private static final class ParameterMap extends AbstractMap<String,Object> implements Serializable, Cloneable
  {
    private final @NotNull Object[] parameters;


    private ParameterMap(@NotNull Configurer<?> configurer) {
      parameters = copyOf(configurer.parameters, configurer.parameterCount * 2);
    }


    @Override
    public int size() {
      return parameters.length / 2;
    }


    @Override
    public boolean isEmpty() {
      return parameters.length == 0;
    }


    @Override
    public boolean containsKey(Object key)
    {
      if (key instanceof String)
        for(int offset = 0, length = parameters.length; offset < length; offset += 2)
          if (parameters[offset].equals(key))
            return true;

      return false;
    }


    @Override
    public boolean containsValue(Object value)
    {
      for(int offset = 1, length = parameters.length; offset < length; offset += 2)
        if (Objects.equals(parameters[offset], value))
          return true;

      return false;
    }


    @Override
    public Object get(Object key) {
      return getOrDefault(key, null);
    }


    @Override
    public Object getOrDefault(Object key, Object defaultValue)
    {
      if (key instanceof String)
        for(int low = 0, high = parameters.length - 2; low <= high;)
        {
          final int mid = ((low + high) >>> 1) & 0xfffe;
          final int cmp = ((String)key).compareTo((String)parameters[mid]);

          if (cmp < 0)
            high = mid - 2;
          else if (cmp > 0)
            low = mid + 2;
          else
            return parameters[mid + 1];
        }

      return defaultValue;
    }


    @Override
    public Object remove(Object key) {
      throw new UnsupportedOperationException("remove");
    }


    @Override
    public void clear() {
      throw new UnsupportedOperationException("clear");
    }


    @Override
    public @NotNull Set<Entry<String,Object>> entrySet() {
      return new ParameterEntrySet(parameters);
    }


    @Override
    public void forEach(BiConsumer<? super String,? super Object> action)
    {
      for(int offset = 0, length = parameters.length; offset < length; offset += 2)
        action.accept((String)parameters[offset], parameters[offset + 1]);
    }


    @Override
    public @NotNull Map<String,Object> clone()
    {
      try {
        return (ParameterMap)super.clone();
      } catch(CloneNotSupportedException ex) {
        throw new RuntimeException(ex);  // will never happen
      }
    }


    @Override
    public boolean equals(Object o)
    {
      if (this != o)
      {
        if (!(o instanceof Map))
          return false;

        final Map<?,?> that = (Map<?,?>)o;
        if (size() != that.size())
          return false;

        for(int offset = 0, length = parameters.length; offset < length; offset += 2)
        {
          final Object key = parameters[offset];
          final Object value = parameters[offset + 1];
          final Object thatValue = that.get(key);

          //noinspection ConstantValue
          if (value == null)
          {
            if (thatValue != null || !that.containsKey(key))
              return false;
          }
          else if (!value.equals(thatValue))
            return false;
        }
      }

      return true;
    }


    @Override
    @SuppressWarnings("ConstantValue")
    public int hashCode()
    {
      int hash = 0;
      Object value;

      // respect map hashcode contract!
      for(int offset = 0, length = parameters.length; offset < length; offset += 2)
        hash += parameters[offset].hashCode() ^ ((value = parameters[offset + 1]) == null ? 0 : value.hashCode());

      return hash;
    }


    @Override
    public String toString()
    {
      final int length = parameters.length;
      if (length == 0)
        return "{}";

      final StringBuilder sb = new StringBuilder("{");

      for(int offset = 0; offset < length; offset += 2)
      {
        if (offset > 0)
          sb.append(", ");

        sb.append(parameters[offset]).append('=').append(parameters[offset + 1]);
      }

      return sb.append("}").toString();
    }
  }




  private static final class ParameterEntrySet extends AbstractSet<Entry<String,Object>>
      implements Serializable, Cloneable
  {
    private final @NotNull Object[] parameters;


    private ParameterEntrySet(@NotNull Object[] parameters) {
      this.parameters = parameters;
    }


    @Override
    public int size() {
      return parameters.length / 2;
    }


    @Override
    public boolean isEmpty() {
      return parameters.length == 0;
    }


    @Override
    public @NotNull Iterator<Entry<String,Object>> iterator()
    {
      return new Iterator<>() {
        int offset = 0;

        @Override
        public boolean hasNext() {
          return offset < parameters.length;
        }

        @Override
        public Entry<String,Object> next()
        {
          if (!hasNext())
            throw new NoSuchElementException();

          final Entry<String,Object> entry =
              new SimpleImmutableEntry<>((String)parameters[offset], parameters[offset + 1]);
          offset += 2;

          return entry;
        }
      };
    }


    @Override
    public Spliterator<Entry<String,Object>> spliterator()
    {
      return new Spliterator<>() {
        int offset = 0;

        @Override
        public boolean tryAdvance(Consumer<? super Entry<String,Object>> action)
        {
          if (offset == parameters.length)
            return false;

          action.accept(new SimpleImmutableEntry<>((String)parameters[offset], parameters[offset + 1]));
          offset += 2;

          return true;
        }

        @Override
        public Spliterator<Entry<String,Object>> trySplit() {
          return null;
        }

        @Override
        public long estimateSize() {
          return parameters.length / 2;
        }

        @Override
        public Comparator<? super Entry<String,Object>> getComparator() {
          return null;
        }

        @Override
        public int characteristics() {
          return ORDERED | DISTINCT | NONNULL | SIZED | IMMUTABLE;
        }
      };
    }


    @Override
    public void clear() {
      throw new UnsupportedOperationException("clear");
    }


    @Override
    public boolean remove(Object o) {
      throw new UnsupportedOperationException("remove");
    }


    @Override
    public boolean removeIf(Predicate<? super Entry<String,Object>> filter) {
      throw new UnsupportedOperationException("removeIf");
    }


    @Override
    public void forEach(@NotNull Consumer<? super Entry<String,Object>> action)
    {
      for(int offset = 0, length = parameters.length; offset < length; offset += 2)
        action.accept(new SimpleImmutableEntry<>((String)parameters[offset], parameters[offset + 1]));
    }


    @Override
    public @NotNull Set<Entry<String,Object>> clone() throws CloneNotSupportedException {
      return (ParameterEntrySet)super.clone();
    }


    @Override
    public String toString()
    {
      final int length = parameters.length;
      if (length == 0)
        return "[]";

      final StringBuilder sb = new StringBuilder("[");

      for(int offset = 0; offset < length; offset += 2)
      {
        if (offset > 0)
          sb.append(", ");

        sb.append((String)parameters[offset]).append('=').append(parameters[offset + 1]);
      }

      return sb.append("]").toString();
    }
  }
}
