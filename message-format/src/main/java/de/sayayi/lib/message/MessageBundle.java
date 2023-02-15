/*
 * Copyright 2019 Jeroen Gremmen
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
package de.sayayi.lib.message;

import de.sayayi.lib.message.exception.MessageException;
import de.sayayi.lib.message.internal.LocalizedMessageBundleWithCode;
import de.sayayi.lib.message.pack.Pack;
import de.sayayi.lib.message.pack.PackInputStream;
import de.sayayi.lib.message.pack.PackOutputStream;
import de.sayayi.lib.message.pack.Unpack;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.function.Predicate;

import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;


/**
 * @author Jeroen Gremmen
 */
@SuppressWarnings("UnstableApiUsage")
public class MessageBundle
{
  @Getter private final @NotNull MessageFactory messageFactory;

  private final @NotNull Map<String,Message.WithCode> messages;
  private final @NotNull Set<Class<?>> indexedClasses;


  public MessageBundle(@NotNull MessageFactory messageFactory)
  {
    this.messageFactory = messageFactory;

    messages = new HashMap<>();
    indexedClasses = new HashSet<>();
  }


  public MessageBundle(@NotNull MessageFactory messageFactory, @NotNull Class<?> classWithMessages)
  {
    this(messageFactory);

    add(classWithMessages);
  }


  MessageBundle(@NotNull MessageFactory messageFactory,
                @NotNull Map<String,Map<Locale,Message>> localizedMessagesByCode)
  {
    this(messageFactory);

    localizedMessagesByCode.forEach(
        (code,localizedMessages) -> add(new LocalizedMessageBundleWithCode(code, localizedMessages)));
  }


  /**
   * Returns all codes contained in this message bundle.
   *
   * @return  set with all message codes, never {@code null}
   */
  @Contract(value = "-> new", pure = true)
  @Unmodifiable
  public @NotNull Set<String> getCodes() {
    return unmodifiableSet(messages.keySet());
  }


  @Contract(pure = true)
  public Message.WithCode getByCode(@NotNull String code) {
    return messages.get(code);
  }


  @Contract(value = "null -> false", pure = true)
  public boolean hasMessageWithCode(String code) {
    return code != null && messages.containsKey(code);
  }


  /**
   * Convenience method for adding multiple pack resources.
   *
   * @param packResources  enumeration of pack resources, not {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   *
   * @see #add(InputStream...)
   */
  @Contract(mutates = "this")
  public void add(@NotNull Enumeration<URL> packResources) throws IOException
  {
    final List<InputStream> packStreams = new ArrayList<>();

    while(packResources.hasMoreElements())
      packStreams.add(packResources.nextElement().openStream());

    add(packStreams.toArray(new InputStream[0]));
  }


  /**
   * Convenience method for adding a single pack.
   *
   * @param packStream  pack stream, not {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @see #add(InputStream...)
   *
   * @since 0.8.0
   */
  @Contract(mutates = "this")
  public void add(@NotNull InputStream packStream) throws IOException {
    add(new InputStream[]{ packStream });
  }


  /**
   * <p>
   *   Add multiple packs to this message bundle.
   * </p>
   * <p>
   *   When adding multiple packs, this method is preferred as it shares map key/values, message parts
   *   and messages for all packs.
   * </p>
   *
   * @param packStreams  array of pack streams, not {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  @Contract(mutates = "this")
  public void add(@NotNull InputStream... packStreams) throws IOException
  {
    final Unpack unpack = new Unpack();

    for(final InputStream packStream: packStreams)
    {
      try(final PackInputStream dataStream = new PackInputStream(packStream)) {
        for(int n = 0, size = dataStream.readUnsignedShort(); n < size; n++)
          add(unpack.loadMessageWithCode(dataStream));
      }
    }
  }


  @Contract(mutates = "this")
  @SuppressWarnings("WeakerAccess")
  public void add(@NotNull Message.WithCode message)
  {
    final String code = requireNonNull(message, "message must not be null").getCode();
    if (hasMessageWithCode(code))
      throw new MessageException("message with code " + code + " already exists in message bundle");

    messages.put(code, message);
  }


  @Contract(mutates = "this")
  @SuppressWarnings("WeakerAccess")
  public void add(@NotNull Class<?> classWithMessages)
  {
    for(Class<?> clazz = classWithMessages; clazz != null && clazz != Object.class; clazz = clazz.getSuperclass())
      if (!indexedClasses.contains(clazz))
      {
        for(final Class<?> ifClass: clazz.getInterfaces())
          add(ifClass);

        for(final Method method: clazz.getDeclaredMethods())
          add0(method);

        add0(clazz);

        indexedClasses.add(clazz);
      }
  }


  private void add0(@NotNull AnnotatedElement annotatedElement) {
    messageFactory.parseAnnotations(annotatedElement).forEach(this::add);
  }


  /**
   *
   * @param packStream  pack output stream, not {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public void pack(@NotNull OutputStream packStream) throws IOException {
    pack(packStream, true, null);
  }


  /**
   *
   * @param packStream         pack output stream, not {@code null}
   * @param compress           {@code true} compress pack, {@code false} do not compress pack
   * @param messageCodeFilter  optional predicate for selecting message codes
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public void pack(@NotNull OutputStream packStream, boolean compress, Predicate<String> messageCodeFilter)
      throws IOException
  {
    if (messageCodeFilter == null)
      messageCodeFilter = c -> true;

    try(final PackOutputStream dataStream = new PackOutputStream(packStream, compress)) {
      final List<String> codes = messages.keySet().stream().filter(messageCodeFilter).sorted().collect(toList());

      dataStream.writeUnsignedShort(codes.size());
      for(final String code: codes)
        Pack.pack(messages.get(code), dataStream);
    }
  }
}
