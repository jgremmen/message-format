package de.sayayi.lib.message.parser;

import de.sayayi.lib.message.Message.Context;


/**
 * @author Jeroen Gremmen
 */
public interface MessagePart
{
  boolean isSpaceBefore();

  boolean isSpaceAfter();

  String getText(Context context);
}
