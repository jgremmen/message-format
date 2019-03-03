package de.sayayi.lib.message.parser;

import de.sayayi.lib.message.MessageContext;

/**
 * @author Jeroen Gremmen
 */
public interface MessagePart
{
  boolean isSpaceBefore();

  boolean isSpaceAfter();

  String getText(MessageContext context);
}
