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

import java.util.*;

import static java.util.Collections.singletonMap;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public class ResourceBundleAdopter extends AbstractMessageAdopter
{
  public ResourceBundleAdopter(@NotNull ConfigurableMessageSupport configurableMessageSupport) {
    super(configurableMessageSupport);
  }


  public ResourceBundleAdopter(@NotNull MessageFactory messageFactory,
                               @NotNull MessagePublisher publisher) {
    super(messageFactory, publisher);
  }


  @Contract(pure = true)
  public void adopt(@NotNull ResourceBundle resourceBundle)
  {
    final Locale locale = resourceBundle.getLocale();

    resourceBundle.keySet().forEach(
        code -> messagePublisher.addMessage(
            messageFactory.parseMessage(code, singletonMap(locale, resourceBundle.getString(code)))));
  }


  @Contract(pure = true)
  public void adopt(@NotNull Collection<ResourceBundle> resourceBundles)
  {
    final Map<String,Map<Locale,String>> localizedMessagesByCode = new HashMap<>();

    for(ResourceBundle resourceBundle: resourceBundles)
    {
      final Locale locale = resourceBundle.getLocale();

      for(String code: resourceBundle.keySet())
      {
        localizedMessagesByCode.computeIfAbsent(code, k -> new HashMap<>())
            .put(locale, resourceBundle.getString(code));
      }
    }

    localizedMessagesByCode.forEach((code,localizedTexts) ->
        messagePublisher.addMessage(messageFactory.parseMessage(code, localizedTexts)));
  }
}
