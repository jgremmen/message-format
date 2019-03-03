package de.sayayi.lib.message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.sayayi.lib.message.formatter.BoolParameterFormatter;
import de.sayayi.lib.message.formatter.ChoiceParameterFormatter;
import de.sayayi.lib.message.formatter.IntegerParameterFormatter;
import de.sayayi.lib.message.formatter.StringParameterFormatter;
import de.sayayi.lib.message.parameter.ParameterFormatter;


/**
 * @author Jeroen Gremmen
 */
public class MessageFactory
{
  private static final Map<String,ParameterFormatter> FORMATTERS = new HashMap<String,ParameterFormatter>();


  static {
    registerFormatter(new StringParameterFormatter());
    registerFormatter(new BoolParameterFormatter());
    registerFormatter(new IntegerParameterFormatter());
    registerFormatter(new ChoiceParameterFormatter());
  }


  public static final void registerFormatter(ParameterFormatter formatter)
  {
    final String name = formatter.getName();
    if (name == null || name.length() == 0)
      throw new IllegalArgumentException("formatter name must not be empty");

    FORMATTERS.put(name, formatter);
  }


  public static final ParameterFormatter getFormatter(String name) {
    return FORMATTERS.get(name);
  }


  public static List<MessagePart> parse(String text)
  {
    return null;
  }
}
