package de.sayayi.lib.message.formatter;

import de.sayayi.lib.message.formatter.support.ArrayFormatter;
import de.sayayi.lib.message.formatter.support.BoolFormatter;
import de.sayayi.lib.message.formatter.support.ChoiceFormatter;
import de.sayayi.lib.message.formatter.support.CollectionFormatter;
import de.sayayi.lib.message.formatter.support.DateFormatter;
import de.sayayi.lib.message.formatter.support.JodaDateTimeFormatter;
import de.sayayi.lib.message.formatter.support.MapFormatter;
import de.sayayi.lib.message.formatter.support.NumberFormatter;
import de.sayayi.lib.message.formatter.support.StringFormatter;
import lombok.Synchronized;


/**
 * Formatter service providing formatters for the most common java types.
 *
 * @author Jeroen Gremmen
 */
public class DefaultFormatterService extends GenericFormatterRegistry
{
  private static FormatterService INSTANCE = null;


  @Synchronized
  public static FormatterService getSharedInstance()
  {
    if (INSTANCE == null)
      INSTANCE = new DefaultFormatterService();

    return INSTANCE;
  }


  public DefaultFormatterService() {
    addDefaultFormatters();
  }


  protected void addDefaultFormatters()
  {
    addNamedFormatters();
    addTypedFormatters();

    if (hasClass("org.joda.time.DateTime"))
      addJodaTimeFormatters();
  }


  protected void addNamedFormatters()
  {
    addFormatter(new ChoiceFormatter());
    addFormatter(new BoolFormatter());
  }


  protected void addTypedFormatters()
  {
    addFormatter(new StringFormatter());
    addFormatter(new NumberFormatter());
    addFormatter(new DateFormatter());
    addFormatter(new ArrayFormatter());
    addFormatter(new CollectionFormatter());
    addFormatter(new MapFormatter());
  }


  protected void addJodaTimeFormatters() {
    addFormatter(new JodaDateTimeFormatter());
  }


  protected boolean hasClass(String className)
  {
    try {
      Class.forName(className, false, DefaultFormatterService.class.getClassLoader());
      return true;
    } catch(final Throwable ex) {
      return false;
    }
  }
}
