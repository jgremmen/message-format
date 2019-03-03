package de.sayayi.lib.message;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import de.sayayi.lib.message.formatter.BoolFormatter;
import de.sayayi.lib.message.formatter.ChoiceFormatter;
import de.sayayi.lib.message.formatter.IntegerFormatter;
import de.sayayi.lib.message.formatter.StringFormatter;
import de.sayayi.lib.message.parameter.ParameterFormatter;
import de.sayayi.lib.message.parser.MessageParser;


/**
 * @author Jeroen Gremmen
 */
public final class MessageFactory
{
  private static final Map<String,ParameterFormatter> FORMATTERS = new HashMap<String,ParameterFormatter>();


  static {
    registerFormatter(new StringFormatter());
    registerFormatter(new BoolFormatter());
    registerFormatter(new IntegerFormatter());
    registerFormatter(new ChoiceFormatter());
  }


  public static void registerFormatter(ParameterFormatter formatter)
  {
    final String name = formatter.getName();
    if (name == null || name.length() == 0)
      throw new IllegalArgumentException("formatter name must not be empty");

    FORMATTERS.put(name, formatter);
  }


  public static ParameterFormatter getFormatter(String name) {
    return FORMATTERS.get(name);
  }


  public static Message parse(String text) throws ParseException {
    return new MessageParser(text).parse();
  }
}
