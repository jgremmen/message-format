package de.sayayi.lib.message.impl;

import de.sayayi.lib.message.Message;
import lombok.Synchronized;
import lombok.ToString;

import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;


/**
 * @author Jeroen Gremmen
 */
@ToString
public class MultipartLocalizedMessageBundleWithCode extends AbstractMessageWithCode
{
  private static final long serialVersionUID = -8638540396975308919L;

  private final Map<Locale,Message> localizedMessages;

  private Boolean _hasParameter;


  public MultipartLocalizedMessageBundleWithCode(String code, Map<Locale,Message> localizedMessages)
  {
    super(code);

    this.localizedMessages = localizedMessages;
  }


  @Override
  public String format(Context context) {
    return findMessageByLocale(context.getLocale()).format(context);
  }


  private Message findMessageByLocale(Locale locale)
  {
    final String searchLanguage = locale.getLanguage();
    final String searchCountry = locale.getCountry();

    int match = -1;
    Message message = null;

    for(final Entry<Locale,Message> entry: localizedMessages.entrySet())
    {
      final Locale keyLocale = entry.getKey();

      if (message == null)
        message = entry.getValue();

      if (match == -1 && (keyLocale == null || Locale.ROOT.equals(keyLocale)))
      {
        message = entry.getValue();
        match = 0;
      }
      else if (keyLocale.getLanguage().equals(searchLanguage))
      {
        if (keyLocale.getCountry().equals(searchCountry))
          return entry.getValue();
        else if (match < 1)
        {
          message = entry.getValue();
          match = 1;
        }
      }
    }

    return message;
  }


  @Synchronized
  @Override
  public boolean hasParameter()
  {
    if (_hasParameter == null)
    {
      _hasParameter = Boolean.FALSE;

      for(final Message message: localizedMessages.values())
        if (message.hasParameter())
        {
          _hasParameter = Boolean.TRUE;
          break;
        }
    }

    return _hasParameter;
  }
}
