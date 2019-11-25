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
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * @author Jeroen Gremmen
 */
public class MessageBundle
{
  private final Map<String,Message.WithCode> messages;
  private final Set<Class<?>> indexedClasses;


  @SuppressWarnings("WeakerAccess")
  public MessageBundle()
  {
    messages = new HashMap<String,Message.WithCode>();
    indexedClasses = new HashSet<Class<?>>();
  }


  public MessageBundle(@NotNull Class<?> classWithMessages)
  {
    this();
    add(classWithMessages);
  }


  public Message.WithCode getByCode(@NotNull String code) {
    return messages.get(code);
  }


  @SuppressWarnings({"WeakerAccess", "squid:S2583"})
  public void add(@NotNull Message.WithCode message)
  {
    //noinspection ConstantConditions
    if (message == null)
      throw new NullPointerException("message must not be null");

    String code = message.getCode();
    if (messages.containsKey(code))
      throw new MessageException("message with code " + code + " already exists in message bundle");

    messages.put(code, message);
  }


  @SuppressWarnings("WeakerAccess")
  public void add(@NotNull Class<?> classWithMessages)
  {
    for(Class<?> clazz = classWithMessages; clazz != null && clazz != Object.class; clazz = clazz.getSuperclass())
      if (!indexedClasses.contains(clazz))
      {
        for(Class<?> ifClass: clazz.getInterfaces())
          add(ifClass);

        for(Method method: clazz.getDeclaredMethods())
          add0(method);

        add0(clazz);
      }

    indexedClasses.add(classWithMessages);
  }


  private void add0(AnnotatedElement annotatedElement)
  {
    for(Message.WithCode message: MessageFactory.parseAnnotations(annotatedElement))
      add(message);
  }
}
