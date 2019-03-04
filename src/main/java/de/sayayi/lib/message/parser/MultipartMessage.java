package de.sayayi.lib.message.parser;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import lombok.ToString;


/**
 * @author Jeroen Gremmen
 */
@ToString
public class MultipartMessage extends AbstractMessage
{
  private static final long serialVersionUID = 3562616383044215265L;

  private final Map<Locale,List<MessagePart>> localizedParts;


  public MultipartMessage(String code, List<MessagePart> parts) {
    this(code, Collections.<Locale,List<MessagePart>>singletonMap(null, parts));
  }


  public MultipartMessage(String code, Map<Locale,List<MessagePart>> localizedParts)
  {
    super(code);

    this.localizedParts = localizedParts;
  }


  @Override
  public String format(Context context)
  {
    final StringBuilder message = new StringBuilder();
    boolean spaceBefore = false;

    for(final MessagePart part: findPartsByLocale(context.getLocale()))
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


  protected List<MessagePart> findPartsByLocale(Locale locale)
  {
    final String searchLanguage = locale.getLanguage();
    final String searchCountry = locale.getCountry();

    int match = -1;
    List<MessagePart> parts = null;

    for(final Entry<Locale,List<MessagePart>> entry: localizedParts.entrySet())
    {
      final Locale keyLocale = entry.getKey();

      if (parts == null)
        parts = entry.getValue();

      if (match == -1 && (keyLocale == null || Locale.ROOT.equals(keyLocale)))
      {
        parts = entry.getValue();
        match = 0;
      }
      else if (keyLocale.getLanguage().equals(searchLanguage))
      {
        if (keyLocale.getCountry().equals(searchCountry))
          return entry.getValue();
        else if (match < 1)
        {
          parts = entry.getValue();
          match = 1;
        }
      }
    }

    return parts;
  }
}
