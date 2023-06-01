package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import de.sayayi.lib.message.part.parameter.key.ConfigKey;
import de.sayayi.lib.message.part.parameter.key.ConfigKeyName;
import de.sayayi.lib.message.part.parameter.key.ConfigKeyString;
import de.sayayi.lib.message.part.parameter.value.ConfigValue;
import de.sayayi.lib.message.part.parameter.value.ConfigValueString;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Locale;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.CompareType.EQ;
import static java.util.Collections.singletonMap;
import static java.util.Locale.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


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
          put(new ConfigKeyString(EQ, "GB"), new ConfigValueString("The Great Kingdom"));
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
          put(new ConfigKeyString(EQ, "fr"), new ConfigValueString("francesa"));
        }}));
  }
}
