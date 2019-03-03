package de.sayayi.lib.message;

import java.util.Locale;


/**
 * @author Jeroen Gremmen
 */
public interface MessageContext
{
  Locale getLocale();

  Object getParameterValue(String parameter);
}
