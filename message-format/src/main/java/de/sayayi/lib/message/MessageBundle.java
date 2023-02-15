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
import de.sayayi.lib.message.internal.EmptyMessageWithCode;
import de.sayayi.lib.message.internal.LocalizedMessageBundleWithCode;
import de.sayayi.lib.message.internal.MessageDelegateWithCode;
import de.sayayi.lib.message.pack.PackHelper;
import de.sayayi.lib.message.pack.PackInputStream;
import de.sayayi.lib.message.pack.PackOutputStream;
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


  MessageBundle(@NotNull MessageFactory messageFactory,
                @NotNull Map<String,Map<Locale,Message>> localizedMessagesByCode)
  {
    this(messageFactory);

    localizedMessagesByCode.forEach((code,localizedMessages) -> {
      switch(localizedMessages.size())
      {
        case 0:
          add(new EmptyMessageWithCode(code));
          break;

        case 1:
          add(new MessageDelegateWithCode(code, localizedMessages.values().iterator().next()));
          break;

        default:
          add(new LocalizedMessageBundleWithCode(code, localizedMessages));
          break;
      }
    });
  }


  /**
   * Returns all codes contained in this message bundle.
   *
   * @return  set with all message codes, never {@code null}
   *
   * @see #getByCode(String)
   */
  @Contract(value = "-> new", pure = true)
  @Unmodifiable
  public @NotNull Set<String> getCodes() {
    return unmodifiableSet(messages.keySet());
  }


  /**
   * Returns a message identified by the given {@code code}.
   *
   * @param code  message code to get, not {@code null}
   *
   * @return  message for the given {@code code} or {@code null} if this bundle contains no such message
   *
   * @see #hasMessageWithCode(String)
   */
  @Contract(pure = true)
  public Message.WithCode getByCode(@NotNull String code) {
    return messages.get(code);
  }


  /**
   * Tells if this bundle contains a message with {@code code}.
   *
   * @param code  message code to check, or {@code null}
   *
   * @return  {@code true} if {@code code} is not {@code null} and the bundle contains a message with this code,
   *          {@code false} otherwise
   */
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
    final PackHelper packHelper = new PackHelper();

    for(final InputStream packStream: packStreams)
    {
      try(final PackInputStream dataStream = new PackInputStream(packStream)) {
        for(int n = 0, size = dataStream.readUnsignedShort(); n < size; n++)
          add(packHelper.unpackMessageWithCode(dataStream));
      }
    }
  }


  /**
   * Add all messages (optionally filtering them using a {@code messageCodeFilter} from {@code messageBundle}
   * to this bundle.
   *
   * @param messageBundle      message bundle from which messages are added, not {@code null}
   * @param messageCodeFilter  optional predicate for selecting message codes. If {@code null} all messages from
   *                           the given {@code messageBundle} will be selected
   *
   * @since 0.8.0
   */
  public void add(@NotNull MessageBundle messageBundle, Predicate<String> messageCodeFilter)
  {
    messageBundle.messages.values()
        .stream()
        .filter(m -> messageCodeFilter == null || messageCodeFilter.test(m.getCode()))
        .forEach(this::add);
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
   * <p>
   *   Pack all messages from this bundle into a compact binary representation. This way message bundles can
   *   be prepared once and loaded very quickly by adding the packed messages to another message bundle at runtime.
   * </p>
   *
   * @param stream  pack output stream, not {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   *
   * @see #add(InputStream...)
   * @see #add(Enumeration)
   */
  public void pack(@NotNull OutputStream stream) throws IOException {
    pack(stream, true, null);
  }


  /**
   * <p>
   *   Pack all messages (optionally filtering them using a {@code messageCodeFilter} from this bundle into a
   *   compact binary representation. This way message bundles can be prepared once and loaded very quickly
   *   by adding the packed messages to another message bundle at runtime.
   * </p>
   * <p>
   *   Parameter {@code compress} switches GZip on/off, reducing the packed size even more. For smaller bundles
   *   the compression may not be substantial as the binary representation does some extensive bit-packing already.
   * </p>
   *
   * @param stream             pack output stream, not {@code null}
   * @param compress           {@code true} compress pack, {@code false} do not compress pack
   * @param messageCodeFilter  optional predicate for selecting message codes. If {@code null} all messages from
   *                           this bundle will be selected
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public void pack(@NotNull OutputStream stream, boolean compress, Predicate<String> messageCodeFilter)
      throws IOException
  {
    if (messageCodeFilter == null)
      messageCodeFilter = c -> true;

    try(final PackOutputStream dataStream = new PackOutputStream(stream, compress)) {
      final List<String> codes = messages.keySet().stream().filter(messageCodeFilter).sorted().collect(toList());

      dataStream.writeUnsignedShort(codes.size());
      for(final String code: codes)
        PackHelper.pack(messages.get(code), dataStream);
    }
  }
}
