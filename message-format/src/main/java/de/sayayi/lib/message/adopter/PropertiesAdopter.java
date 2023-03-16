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
import de.sayayi.lib.message.MessageSupport.MessagePublisher;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public class PropertiesAdopter extends AbstractMessageAdopter
{
  public PropertiesAdopter(@NotNull MessageFactory messageFactory,
                           @NotNull MessagePublisher publisher) {
    super(messageFactory, publisher);
  }


  @Contract(pure = true)
  public void adopt(@NotNull Properties properties)
  {
    properties.forEach((k,v) -> messagePublisher.addMessage(
        messageFactory.parseMessage(k.toString(), v.toString())));
  }


  @Contract(pure = true)
  public void adopt(@NotNull Map<Locale,Properties> properties)
  {
    final Map<String,Map<Locale,String>> localizedMessagesByCode = new HashMap<>();

    for(Entry<Locale,Properties> entry: properties.entrySet())
      for(Entry<Object,Object> localizedProperty: entry.getValue().entrySet())
      {
        localizedMessagesByCode
            .computeIfAbsent(localizedProperty.getKey().toString(), k -> new HashMap<>())
            .put(entry.getKey(), localizedProperty.getValue().toString());
      }

    localizedMessagesByCode.forEach((code,localizedTexts) ->
        messagePublisher.addMessage(messageFactory.parseMessage(code, localizedTexts)));
  }
}
