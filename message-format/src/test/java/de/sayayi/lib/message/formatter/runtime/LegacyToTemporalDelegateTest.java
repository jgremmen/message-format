package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import de.sayayi.lib.message.part.parameter.key.ConfigKeyName;
import de.sayayi.lib.message.part.parameter.value.ConfigValueString;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static java.util.Locale.GERMANY;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 * @since 0.12.0
 */
@DisplayName("Legacy temporal delegate")
class LegacyToTemporalDelegateTest extends AbstractFormatterTest
{
  private MessageSupport.MessageAccessor messageAccessor;


  @BeforeEach
  void init()
  {
    messageAccessor = MessageSupportFactory
        .create(createFormatterService(new TemporalFormatter(), new LegacyToTemporalDelegate()), NO_CACHE_INSTANCE)
        .setLocale(GERMANY)
        .getMessageAccessor();
  }


  @Test
  @DisplayName("Format java.util.Date")
  @SuppressWarnings("deprecation")
  void formatDate()
  {
    val date = new Date(2024 - 1900, Calendar.NOVEMBER, 9);

    assertEquals(noSpaceText("09.11.2024, 00:00:00"),
        format(messageAccessor, date,
            Map.of(new ConfigKeyName("date"), new ConfigValueString("medium"))));
  }


  @Test
  @DisplayName("Format java.sql.Date")
  @SuppressWarnings("deprecation")
  void formatSqlDate()
  {
    val date = new Date(2024 - 1900, Calendar.NOVEMBER, 9);
    val sqlDate = new java.sql.Date(date.getTime());

    assertEquals(noSpaceText("2024-11-09"),
        format(messageAccessor, sqlDate,
            Map.of(new ConfigKeyName("date"), new ConfigValueString("yyyy-MM-dd"))));
  }


  @Test
  @DisplayName("Format java.sql.Time")
  @SuppressWarnings("deprecation")
  void formatSqlTime()
  {
    val sqlTime = new java.sql.Time(23, 36, 4);

    assertEquals(noSpaceText("23:36"),
        format(messageAccessor, sqlTime,
            Map.of(new ConfigKeyName("date"), new ConfigValueString("HH:mm"))));
  }
}