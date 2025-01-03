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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public class PropertiesAdopter extends AbstractMessageAdopter
{
  /**
   * Create a properties adopter for the given {@code configurableMessageSupport}.
   *
   * @param configurableMessageSupport  configurable message support, not {@code null}
   */
  public PropertiesAdopter(@NotNull ConfigurableMessageSupport configurableMessageSupport) {
    super(configurableMessageSupport);
  }


  /**
   * Create a properties adopter for the given {@code messageFactory} and {@code publisher}.
   *
   * @param messageFactory  message factory, not {@code null}
   * @param publisher       message publisher, not {@code null}
   */
  public PropertiesAdopter(@NotNull MessageFactory messageFactory, @NotNull MessagePublisher publisher) {
    super(messageFactory, publisher);
  }


  /**
   * Adopt messages from properties.
   * <p>
   * Each key from the properties object is used as the message code,
   * the value is parsed as a message.
   *
   * @param properties  message properties, not {@code null}
   */
  @Contract(pure = true)
  public void adopt(@NotNull Properties properties)
  {
    properties.forEach((code,message) ->
        messagePublisher.addMessage(messageFactory.parseMessage(code.toString(), message.toString())));
  }


  /**
   * Adopt messages from localized properties.
   * <p>
   * Each key from the properties object is used as the message code,
   * the value is parsed as a message. If the same message code is available in multiple
   * property objects, a single message containing multiple localized messages is created for
   * each message code.
   *
   * @param properties  map with property messages keyed by locale, not {@code null}
   */
  @Contract(pure = true)
  public void adopt(@NotNull Map<Locale,Properties> properties)
  {
    var localizedMessagesByCode = new HashMap<String,Map<Locale,String>>();

    for(var entry: properties.entrySet())
    {
      var locale = entry.getKey();

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
