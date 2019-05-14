package de.sayayi.lib.message.formatter.support;


import de.sayayi.lib.message.formatter.ParameterFormatter;

import static org.junit.Assert.fail;


/**
 * @author Jeroen Gremmen
 */
abstract class AbstractFormatterTest
{
  void assertFormatterForType(ParameterFormatter formatter, Class<?> type)
  {
    for(Class<?> formattableType: formatter.getFormattableTypes())
      if (formattableType.isAssignableFrom(type))
        return;

    fail();
  }
}
