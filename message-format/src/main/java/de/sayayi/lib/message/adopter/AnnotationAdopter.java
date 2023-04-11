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
@SuppressWarnings({"DuplicatedCode", "UnstableApiUsage", "UnusedReturnValue"})
public class AnnotationAdopter extends AbstractMessageAdopter
{
  private final Set<Class<?>> indexedClasses = new HashSet<>();


  /**
   * Create an annotation adopter for the given {@code configurableMessageSupport}.
   *
   * @param configurableMessageSupport  configurable message support, not {@code null}
   */
  public AnnotationAdopter(@NotNull ConfigurableMessageSupport configurableMessageSupport) {
    super(configurableMessageSupport);
  }


  /**
   * Create an annotation adopter for the given {@code messageFactory} and {@code publisher}.
   *
   * @param messageFactory  message factory, not {@code null}
   * @param publisher       message publisher, not {@code null}
   */
  public AnnotationAdopter(@NotNull MessageFactory messageFactory,
                           @NotNull MessagePublisher publisher) {
    super(messageFactory, publisher);
  }


  /**
   * Publish the message or template defined on the annotated element.
   * <p>
   * This method recognizes {@link MessageDef} and {@link TemplateDef} annotations.
   *
   * @param element  annotated element (eg. method, field, class), not {@code null}
   *
   * @return  this annotation adopter instance, never {@code null}
   */
  @Contract(pure = true)
  public @NotNull AnnotationAdopter adopt(@NotNull AnnotatedElement element)
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

    return this;
  }


  /**
   * Publish the message defined in the given {@link MessageDef} annotation.
   *
   * @param messageDef  {@code MessageDef} annotation, not {@code null}
   *
   * @return  this annotation adopter instance, never {@code null}
   */
  public @NotNull AnnotationAdopter adopt(@NotNull MessageDef messageDef)
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
        final String value = text.locale().isEmpty() &&
            text.text().isEmpty() ? text.value() : text.text();

        if (!localizedTexts.containsKey(locale))
          localizedTexts.put(locale, value);
      }

      messagePublisher.addMessage(messageFactory.parseMessage(code, localizedTexts));
    }

    return this;
  }


  /**
   * Publish the template defined in the given {@link TemplateDef} annotation.
   *
   * @param templateDef  {@code TemplateDef} annotation, not {@code null}
   *
   * @return  this annotation adopter instance, never {@code null}
   */
  public @NotNull AnnotationAdopter adopt(@NotNull TemplateDef templateDef)
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
      final String value = text.locale().isEmpty() &&
          text.text().isEmpty() ? text.value() : text.text();

      if (!localizedTexts.containsKey(locale))
        localizedTexts.put(locale, value);
    }

    messagePublisher.addTemplate(name, messageFactory.parseTemplate(localizedTexts));

    return this;
  }


  /**
   * Adopt all the annotations ({@link MessageDef}, {@link TemplateDef}) found in the given class.
   * <p>
   * This method analyses the given {@code type} as well as all super classes and interfaces.
   * It will look at the class and method level for message annotations.
   *
   * @param type  type to adopt messages from, not {@code null}
   *
   * @return  this annotation adopter instance, never {@code null}
   *
   * @see #adopt(AnnotatedElement)
   */
  @Contract(mutates = "this")
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public @NotNull AnnotationAdopter adoptAll(@NotNull Class<?> type)
  {
    for(Class<?> clazz = type; clazz != null && clazz != Object.class;
        clazz = clazz.getSuperclass())
      if (!indexedClasses.contains(clazz))
      {
        for(final Class<?> ifClass: clazz.getInterfaces())
          adoptAll(ifClass);

        for(final Method method: clazz.getDeclaredMethods())
          adopt(method);

        adopt(clazz);

        indexedClasses.add(clazz);
      }

    return this;
  }
}
