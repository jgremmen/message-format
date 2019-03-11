package de.sayayi.lib.message.parser;

import de.sayayi.lib.message.Message.Context;
import lombok.Getter;


/**
 * @author Jeroen Gremmen
 */
public abstract class MessagePart
{
  @Getter protected final boolean spaceBefore;
  @Getter protected final boolean spaceAfter;


  protected MessagePart(boolean spaceBefore, boolean spaceAfter)
  {
    this.spaceBefore = spaceBefore;
    this.spaceAfter = spaceAfter;
  }


  public abstract String getText(Context context);


  public abstract boolean isParameter();
}
