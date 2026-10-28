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
import de.sayayi.lib.message.exception.DuplicateMessageException;
import de.sayayi.lib.message.exception.DuplicateTemplateException;
import de.sayayi.lib.message.exception.MessageParserException;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import static java.util.Objects.requireNonNull;


/**
 * Message adopter that reads messages and templates from {@link Properties} objects. Property keys are used as
 * message codes or template names and property values are parsed as message format strings.
 * <p>
 * For locale-aware messages, the {@link #adopt(Map)} method accepts a map of {@link Properties} keyed by
 * {@link Locale}. Properties that share the same key across multiple locales are combined into a single localized
 * message.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public class PropertiesAdopter extends AbstractMessageAdopter
{
  /**
   * Creates a properties adopter for the given {@code configurableMessageSupport}. The message
   * factory and message publisher are both obtained from the configurable message support instance.
   *
   * @param configurableMessageSupport  configurable message support, not {@code null}
   */
  public PropertiesAdopter(@NotNull ConfigurableMessageSupport configurableMessageSupport) {
    super(configurableMessageSupport);
  }


  /**
   * Creates a properties adopter for the given {@code messageFactory} and {@code publisher}. This constructor allows
   * the message factory and message publisher to be provided independently.
   *
   * @param messageFactory  message factory used for parsing message format strings, not {@code null}
   * @param publisher       message publisher used for publishing parsed messages and templates, not {@code null}
   */
  public PropertiesAdopter(@NotNull MessageFactory messageFactory, @NotNull MessagePublisher publisher) {
    super(messageFactory, publisher);
  }


  /**
   * Adopts messages from properties. Each property key is used as the message code and its
   * corresponding value is parsed as a message format string. The resulting messages are
   * published to the {@linkplain MessagePublisher message publisher}.
   *
   * @param properties  message properties, not {@code null}
   *
   * @throws MessageParserException  if a property value cannot be parsed
   * @throws DuplicateMessageException  if a message with the same code but different
   *         content has already been published
   *
   * @see #adopt(Map)
   */
  public void adopt(@NotNull Properties properties)
  {
    requireNonNull(properties, "properties must not be null").forEach((code,message) ->
        messagePublisher.addMessage(messageFactory.parseMessage(code.toString(), message.toString())));
  }


  /**
   * Adopts templates from properties. Each property key is used as the template name and its corresponding value is
   * parsed as a template format string. The resulting templates are published to the
   * {@linkplain MessagePublisher message publisher}.
   *
   * @param properties  template properties, not {@code null}
   *
   * @throws MessageParserException      if a property value cannot be parsed
   * @throws DuplicateTemplateException  if a template with the same name but different content has already been
   *                                     published
   * @throws IllegalArgumentException    if a property key is not a valid kebab-case template name
   *
   * @since 0.20.0
   */
  public void adoptTemplates(@NotNull Properties properties)
  {
    requireNonNull(properties, "properties must not be null").forEach((code,message) ->
        messagePublisher.addTemplate(code.toString(), messageFactory.parseTemplate(message.toString())));
  }


  /**
   * Adopts messages from localized properties. Each property key is used as the message code and its corresponding
   * value is parsed as a message format string. If the same message code appears in multiple locale entries, the
   * localized values are combined into a single locale-aware message. The resulting messages are published to the
   * {@linkplain MessagePublisher message publisher}.
   *
   * @param properties  map with property messages keyed by locale, not {@code null}
   *
   * @throws MessageParserException     if a property value cannot be parsed
   * @throws DuplicateMessageException  if a message with the same code but different content has already been published
   *
   * @see #adopt(Properties)
   */
  public void adopt(@NotNull Map<Locale,Properties> properties)
  {
    final var localizedMessagesByCode = new HashMap<String,Map<Locale,String>>();

    for(var entry: requireNonNull(properties, "properties must not be null").entrySet())
    {
      final var locale = entry.getKey();

      for(var localizedProperty: entry.getValue().entrySet())
      {
        localizedMessagesByCode
            .computeIfAbsent(localizedProperty.getKey().toString(), k -> new HashMap<>())
            .put(locale, localizedProperty.getValue().toString());
      }
    }

    localizedMessagesByCode.forEach((code,localizedTexts) ->
        messagePublisher.addMessage(messageFactory.parseMessage(code, localizedTexts)));
  }
}
