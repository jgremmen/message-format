package de.sayayi.lib.message.parser;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.sayayi.lib.message.MessageWithCode;
import lombok.Getter;


/**
 * @author Jeroen Gremmen
 */
public class MultipartMessage implements MessageWithCode
{
  @Getter private final String code;
  private final Map<Locale,List<MessagePart>> localizedParts;


  public MultipartMessage(String code, List<MessagePart> parts) {
    this(code, Collections.<Locale,List<MessagePart>>singletonMap(null, parts));
  }


  public MultipartMessage(String code, Map<Locale,List<MessagePart>> localizedParts)
  {
    this.code = code;
    this.localizedParts = localizedParts;
  }


  @Override
  public String format(Context context)
  {
    final StringBuilder message = new StringBuilder();
    boolean spaceBefore = false;

    for(final MessagePart part: findMessagePartsByLocale(context.getLocale()))
    {
      final String text = part.getText(context);

      if (text != null && !text.isEmpty())
      {
        if (spaceBefore || part.isSpaceBefore())
          message.append(' ');

        message.append(text);
        spaceBefore = part.isSpaceAfter();
      }
    }

    return message.toString();
  }


  protected List<MessagePart> findMessagePartsByLocale(Locale locale)
  {
    List<MessagePart> parts = localizedParts.get(locale);
    if (parts == null)
      parts = localizedParts.get(Locale.ROOT);

    return parts;
  }
}
