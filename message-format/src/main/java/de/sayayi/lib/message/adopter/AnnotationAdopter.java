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
package de.sayayi.lib.message.adopter;

import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.MessageSupport.ConfigurableMessageSupport;
import de.sayayi.lib.message.MessageSupport.MessagePublisher;
import de.sayayi.lib.message.annotation.*;
import de.sayayi.lib.message.internal.EmptyMessage;
import de.sayayi.lib.message.internal.EmptyMessageWithCode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.*;

import static java.util.Locale.forLanguageTag;


/**
 * The annotation adopter provides various methods for publishing messages defined by annotations
 * like {@code @MessageDef} and {@code @TemplateDef}.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@SuppressWarnings({"DuplicatedCode", "UnstableApiUsage"})
public class AnnotationAdopter extends AbstractMessageAdopter
{
  private final Set<Class<?>> indexedClasses = new HashSet<>();


  public AnnotationAdopter(@NotNull ConfigurableMessageSupport configurableMessageSupport) {
    super(configurableMessageSupport);
  }


  public AnnotationAdopter(@NotNull MessageFactory messageFactory,
                           @NotNull MessagePublisher publisher) {
    super(messageFactory, publisher);
  }


  @Contract(pure = true)
  public void adopt(@NotNull AnnotatedElement element)
  {
    MessageDef messageDef = element.getAnnotation(MessageDef.class);
    if (messageDef != null)
      adopt(messageDef);

    MessageDefs messageDefs = element.getAnnotation(MessageDefs.class);
    if (messageDefs != null)
      Arrays.stream(messageDefs.value()).forEach(this::adopt);

    TemplateDef templateDef = element.getAnnotation(TemplateDef.class);
    if (templateDef != null)
      adopt(templateDef);

    TemplateDefs templateDefs = element.getAnnotation(TemplateDefs.class);
    if (templateDefs != null)
      Arrays.stream(templateDefs.value()).forEach(this::adopt);
  }


  /**
   * Publish message defined in the given {@link MessageDef} annotation.
   *
   * @param messageDef  {@code MessageDef} annotation, not {@code null}
   */
  public void adopt(@NotNull MessageDef messageDef)
  {
    final Text[] texts = messageDef.texts();
    final String code = messageDef.code();

    if (texts.length == 0)
    {
      final String text = messageDef.text();

      messagePublisher.addMessage(text.isEmpty()
          ? new EmptyMessageWithCode(code)
          : messageFactory.parseMessage(code, text));
    }
    else
    {
      final Map<Locale,String> localizedTexts = new LinkedHashMap<>();

      for(final Text text: texts)
      {
        final Locale locale = forLanguageTag(text.locale());
        final String value = text.locale().isEmpty() && text.text().isEmpty() ? text.value() : text.text();

        if (!localizedTexts.containsKey(locale))
          localizedTexts.put(locale, value);
      }

      messagePublisher.addMessage(messageFactory.parseMessage(code, localizedTexts));
    }
  }


  /**
   * Publish template defined in the given {@link TemplateDef} annotation.
   *
   * @param templateDef  {@code TemplateDef} annotation, not {@code null}
   */
  public void adopt(@NotNull TemplateDef templateDef)
  {
    final Text[] texts = templateDef.texts();
    final String name = templateDef.name();

    if (texts.length == 0)
    {
      final String text = templateDef.text();

      messagePublisher.addTemplate(name, text.isEmpty()
          ? EmptyMessage.INSTANCE
          : messageFactory.parseTemplate(text));
    }

    final Map<Locale,String> localizedTexts = new LinkedHashMap<>();

    for(final Text text: texts)
    {
      final Locale locale = forLanguageTag(text.locale());
      final String value = text.locale().isEmpty() && text.text().isEmpty() ? text.value() : text.text();

      if (!localizedTexts.containsKey(locale))
        localizedTexts.put(locale, value);
    }

    messagePublisher.addTemplate(name, messageFactory.parseTemplate(localizedTexts));
  }


  @Contract(mutates = "this")
  public void adoptAll(@NotNull Class<?> type)
  {
    for(Class<?> clazz = type; clazz != null && clazz != Object.class; clazz = clazz.getSuperclass())
      if (!indexedClasses.contains(clazz))
      {
        for(final Class<?> ifClass: clazz.getInterfaces())
          adoptAll(ifClass);

        for(final Method method: clazz.getDeclaredMethods())
          adopt(method);

        adopt(clazz);

        indexedClasses.add(clazz);
      }
  }
}
