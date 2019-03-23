package de.sayayi.lib.message.impl;

import de.sayayi.lib.message.Message;
import lombok.ToString;


/**
 * @author Jeroen Gremmen
 */
@ToString
public class MessageDelegateWithCode extends AbstractMessageWithCode
{
  private static final long serialVersionUID = 4222709361838277916L;

  private final Message message;


  public MessageDelegateWithCode(String code, Message message)
  {
    super(code);

    this.message = message;
  }


  @Override
  public String format(Context context) {
    return message.format(context);
  }


  @Override
  public boolean hasParameter() {
    return message.hasParameter();
  }
}
