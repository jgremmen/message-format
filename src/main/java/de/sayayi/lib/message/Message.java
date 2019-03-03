package de.sayayi.lib.message;


/**
 * Messages are thread safe.
 *
 * @author Jeroen Gremmen
 */
public interface Message
{
  /**
   * Formats the message based on the message context provided.
   *
   * @param context  message context
   *
   * @return  formatted message
   */
  String format(MessageContext context);
}
