package de.sayayi.lib.message.parser;

import lombok.Getter;


/**
 * @author Jeroen Gremmen
 */
@SuppressWarnings("serial")
public class MessageParserException extends RuntimeException
{
  @Getter private final int position;


  MessageParserException(int position, String message)
  {
    super(message);

    this.position = position;
  }
}
