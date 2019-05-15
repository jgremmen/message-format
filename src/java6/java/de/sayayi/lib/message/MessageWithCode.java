package de.sayayi.lib.message;


/**
 * @author Jeroen Gremmen
 */
public interface MessageWithCode extends Message
{
  /**
   * Returns a unique message code.
   *
   * @return  message code, never {@code null}
   */
  String getCode();
}
