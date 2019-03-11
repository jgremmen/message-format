package de.sayayi.lib.message;

import java.lang.reflect.AnnotatedElement;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import de.sayayi.lib.message.annotation.Text;
import de.sayayi.lib.message.parser.MessageParser;
import de.sayayi.lib.message.spi.EmptyMessageWithCode;
import de.sayayi.lib.message.spi.MessageDelegateWithCode;
import de.sayayi.lib.message.spi.MultipartLocalizedMessageBundleWithCode;


/**
 * @author Jeroen Gremmen
 */
public final class MessageFactory
{
  public static Message parse(String text) {
    return new MessageParser(text).parseMessage();
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
    return new MessageDelegateWithCode(code, new MessageParser(text).parseMessage());
  }


  public static MessageWithCode parse(String code, Map<Locale,String> localizedTexts) throws ParseException
  {
    if (localizedTexts.isEmpty())
      return new EmptyMessageWithCode(code);

    final Map<Locale,Message> localizedParts = new LinkedHashMap<Locale,Message>();

    for(final Entry<Locale,String> localizedText: localizedTexts.entrySet())
      localizedParts.put(localizedText.getKey(), new MessageParser(localizedText.getValue()).parseMessage());

    return new MultipartLocalizedMessageBundleWithCode(code, localizedParts);
  }


  public static MessageWithCode parseAnnotation(AnnotatedElement element) throws ParseException
  {
    final de.sayayi.lib.message.annotation.Message annotation =
        element.getAnnotation(de.sayayi.lib.message.annotation.Message.class);

    if (annotation == null)
      throw new IllegalArgumentException(element.toString() + " has no @Message annotation");

    final Text[] texts = annotation.texts();
    if (texts == null || texts.length == 0)
      return new EmptyMessageWithCode(annotation.code());

    final Map<Locale,String> localizedTexts = new LinkedHashMap<Locale,String>();

    for(final Text text: texts)
    {
      final Locale locale = forLanguageTag(text.locale());
      if (!localizedTexts.containsKey(locale))
        localizedTexts.put(locale, text.text());
    }

    return parse(annotation.code(), localizedTexts);
  }


  private static Locale forLanguageTag(String locale) throws ParseException
  {
    if (locale.isEmpty())
      return Locale.ROOT;

    final int length = locale.length();
    if (length < 2)
      throw new ParseException("missing language code for locale " + locale, 0);

    if (!Character.isLowerCase(locale.charAt(0)) || !Character.isLowerCase(locale.charAt(1)))
        throw new ParseException("invalid language code for locale " + locale, 0);

    if (length == 2)
      return new Locale(locale);

    if (length != 5)
      throw new ParseException("unexpected length " + length + " for locale " + locale, 2);

    if (locale.charAt(2) != '-' && locale.charAt(2) != '_')
      throw new ParseException("missing separator '-' between language and country code for locale " + locale, 2);

    if (!Character.isUpperCase(locale.charAt(3)) || !Character.isUpperCase(locale.charAt(4)))
      throw new ParseException("invalid country code for locale " + locale, 3);

    return new Locale(locale.substring(0,  2), locale.substring(3, 5));
  }
}
