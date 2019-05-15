package de.sayayi.lib.message.exception;


/**
 * @author Jeroen Gremmen
 */
@SuppressWarnings("serial")
public class MessageException extends RuntimeException
{
  public MessageException(String message) {
    super(message);
  }


  public MessageException(String message, Throwable cause) {
    super(message, cause);
  }


  public MessageException(Throwable cause) {
    super(cause);
  }
}
