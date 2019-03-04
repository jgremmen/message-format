package de.sayayi.lib.message;

import java.lang.reflect.AnnotatedElement;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.sayayi.lib.message.annotation.Text;
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


  static
  {
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
    return parse(null, text);
  }


  public static Message parse(Map<Locale,String> localizedTexts) throws ParseException {
    return parse(null, localizedTexts);
  }


  /**
   * Parse the message {@code text} into a {@link Message} instance which can be used to
   *
   * @param text
   * @return
   * @throws ParseException
   */
  public static MessageWithCode parse(String code, String text) throws ParseException {
    return new MultipartMessage(code, new MessageParser(text).parse());
  }


  public static MessageWithCode parse(String code, Map<Locale,String> localizedTexts) throws ParseException
  {
    final Map<Locale,List<MessagePart>> localizedParts = new LinkedHashMap<Locale,List<MessagePart>>();

    for(final Entry<Locale,String> localizedText: localizedTexts.entrySet())
      localizedParts.put(localizedText.getKey(), new MessageParser(localizedText.getValue()).parse());

    if (!localizedTexts.containsKey(Locale.ROOT))
    {
      List<MessagePart> parts = localizedParts.get(Locale.getDefault());
      if (parts == null)
        parts = localizedParts.values().iterator().next();

      localizedParts.put(Locale.ROOT, parts);
    }

    return new MultipartMessage(code, localizedParts);
  }


  public static MessageWithCode parseAnnotation(AnnotatedElement element) throws ParseException
  {
    final de.sayayi.lib.message.annotation.Message annotation =
        element.getAnnotation(de.sayayi.lib.message.annotation.Message.class);

    if (annotation == null)
      throw new IllegalArgumentException(element.toString() + " has no @Message annotation");

    final Text[] texts = annotation.texts();
    if (texts == null || texts.length == 0)
      throw new IllegalArgumentException("@Message annotation on  " + element + " contains no texts");

    final Set<Locale> locales = new HashSet<Locale>();
    final Map<Locale,String> localizedTexts = new LinkedHashMap<Locale,String>();

    for(final Text text: texts)
    {
      final Locale locale = text.locale().isEmpty() ? Locale.ROOT : new Locale(text.locale());
      if (locales.add(locale))
        localizedTexts.put(locale, text.text());
    }

    return parse(annotation.code(), localizedTexts);
  }
}
