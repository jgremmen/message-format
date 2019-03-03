package de.sayayi.lib.message;

import java.util.Locale;
import java.util.Map;


/**
 * @author Jeroen Gremmen
 */
public interface Message
{
  String format(Map<String,Object> parameters);

  String format(Locale locale, Map<String,Object> parameters);
}
