package de.sayayi.lib.message;

import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import de.sayayi.lib.message.formatter.BoolFormatter;
import de.sayayi.lib.message.formatter.ChoiceFormatter;
import de.sayayi.lib.message.formatter.IntegerFormatter;
import de.sayayi.lib.message.formatter.StringFormatter;
import de.sayayi.lib.message.parameter.ParameterFormatter;
import de.sayayi.lib.message.parser.MessageParser;
import de.sayayi.lib.message.parser.MessagePart;
import de.sayayi.lib.message.parser.MultipartMessage;


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


  /**
   * Parse the message {@code text} into a {@link Message} instance which can be used to
   *
   * @param text
   * @return
   * @throws ParseException
   */
  public static Message parse(String text) throws ParseException {
    return new MultipartMessage(new MessageParser(text).parse());
  }


  public static Message parse(Map<Locale,String> localizedTexts) throws ParseException
  {
    final Map<Locale,List<MessagePart>> localizedParts = new LinkedHashMap<Locale,List<MessagePart>>();

    for(final Entry<Locale,String> localizedText: localizedTexts.entrySet())
      localizedParts.put(localizedText.getKey(), new MessageParser(localizedText.getValue()).parse());

    if (!localizedTexts.containsKey(null))
    {
      List<MessagePart> parts = localizedParts.get(Locale.getDefault());
      if (parts == null)
        parts = localizedParts.values().iterator().next();

      localizedParts.put(null, parts);
    }

    return new MultipartMessage(localizedParts);
  }
}
