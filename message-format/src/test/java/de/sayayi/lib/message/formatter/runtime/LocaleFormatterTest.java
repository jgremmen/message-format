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
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import de.sayayi.lib.message.part.parameter.key.ConfigKey;
import de.sayayi.lib.message.part.parameter.key.ConfigKeyName;
import de.sayayi.lib.message.part.parameter.key.ConfigKeyString;
import de.sayayi.lib.message.part.parameter.value.ConfigValue;
import de.sayayi.lib.message.part.parameter.value.ConfigValueString;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Locale;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static java.util.Collections.singletonMap;
import static java.util.Locale.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@DisplayName("Locale formatter")
class LocaleFormatterTest extends AbstractFormatterTest
{
  @Test
  void testFormattableTypes() {
    assertFormatterForType(new LocaleFormatter(), Locale.class);
  }


  @Test
  void testFormatCountry()
  {
    val messageAccessor = MessageSupportFactory
        .create(createFormatterService(new LocaleFormatter()), NO_CACHE_INSTANCE)
        .setLocale(FRANCE)
        .getMessageAccessor();

    assertEquals(noSpaceText("Etats-Unis"), format(messageAccessor, US,
        singletonMap(new ConfigKeyName("locale"), new ConfigValueString("country"))));

    assertEquals(noSpaceText("The Great Kingdom"), format(messageAccessor, UK,
        new HashMap<ConfigKey,ConfigValue>() {{
          put(new ConfigKeyName("locale"), new ConfigValueString("country"));
          put(new ConfigKeyString("GB"), new ConfigValueString("The Great Kingdom"));
        }}));
  }


  @Test
  void testFormatLanguage()
  {
    val messageAccessor = MessageSupportFactory
        .create(createFormatterService(new LocaleFormatter()), NO_CACHE_INSTANCE)
        .setLocale(new Locale("es", "ES"))
        .getMessageAccessor();

    assertEquals(noSpaceText("inglés"), format(messageAccessor, UK,
        singletonMap(new ConfigKeyName("locale"), new ConfigValueString("language"))));

    assertEquals(noSpaceText("francesa"), format(messageAccessor, FRANCE,
        new HashMap<ConfigKey,ConfigValue>() {{
          put(new ConfigKeyName("locale"), new ConfigValueString("language"));
          put(new ConfigKeyString("fr"), new ConfigValueString("francesa"));
        }}));
  }


  @Test
  void testFormatName()
  {
    val messageAccessor = MessageSupportFactory
        .create(createFormatterService(new LocaleFormatter()), NO_CACHE_INSTANCE)
        .setLocale(new Locale("nl", "BE"))
        .getMessageAccessor();

    assertEquals(noSpaceText("Engels (Verenigd Koninkrijk)"), format(messageAccessor, UK));

    assertEquals(noSpaceText("Duits"), format(messageAccessor, GERMAN,
        singletonMap(new ConfigKeyName("locale"), new ConfigValueString("name"))));

    assertEquals(noSpaceText("Koreaans (Zuid-Korea)"), format(messageAccessor, KOREA,
        singletonMap(new ConfigKeyName("locale"), new ConfigValueString("name"))));
  }
}
