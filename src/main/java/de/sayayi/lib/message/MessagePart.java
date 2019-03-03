package de.sayayi.lib.message;


/**
 * @author Jeroen Gremmen
 */
public interface MessagePart
{
  boolean isSpaceBefore();

  boolean isSpaceAfter();

  String getText(MessageContext context);
}
