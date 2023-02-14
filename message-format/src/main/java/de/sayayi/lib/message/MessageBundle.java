/*
 * Copyright 2019 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import de.sayayi.lib.message.pack.Unpack;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.*;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;


/**
 * @author Jeroen Gremmen
 */
@SuppressWarnings("UnstableApiUsage")
public class MessageBundle
{
  private static final byte[] PACK_MAGIC = "MSGB\u0000\u0008".getBytes(US_ASCII);

  @Getter private final @NotNull MessageFactory messageFactory;

  private final @NotNull Map<String,Message.WithCode> messages;
  private final @NotNull Set<Class<?>> indexedClasses;


  public MessageBundle(@NotNull MessageFactory messageFactory)
  {
    this.messageFactory = messageFactory;

    messages = new HashMap<>();
    indexedClasses = new HashSet<>();
  }


  public MessageBundle(@NotNull MessageFactory messageFactory, @NotNull InputStream packStream) throws IOException
  {
    this(messageFactory);

    final byte[] signature = new byte[PACK_MAGIC.length];
    if (packStream.read(signature) != PACK_MAGIC.length || !Arrays.equals(signature, PACK_MAGIC))
      throw new IOException("pack stream has wrong signature");

    final Unpack unpack = new Unpack();

    try(final DataInputStream dataStream = new DataInputStream(new GZIPInputStream(packStream))) {
      for(int n = 0, size = dataStream.readUnsignedShort(); n < size; n++)
        add(unpack.loadMessageWithCode(dataStream));
    }
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


  public void pack(@NotNull OutputStream packStream) throws IOException
  {
    // write signature
    packStream.write(PACK_MAGIC);

    try(final DataOutputStream dataStream = new DataOutputStream(new GZIPOutputStream(packStream))) {
      dataStream.writeShort(messages.size());

      for(final Message.WithCode message: messages.values())
        Pack.pack(message, dataStream);
    }
  }
}
