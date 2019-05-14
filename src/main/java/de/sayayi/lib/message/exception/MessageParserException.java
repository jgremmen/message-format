package de.sayayi.lib.message.exception;

import lombok.Getter;


/**
 * @author Jeroen Gremmen
 */
@SuppressWarnings("serial")
public class MessageParserException extends MessageException
{
  @Getter private final int position;


  public MessageParserException(int position, String message)
  {
    super(message);

    this.position = position;
  }
}
