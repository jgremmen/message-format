package de.sayayi.lib.message.parser;

import de.sayayi.lib.message.Message.Parameters;
import lombok.Getter;

import java.io.Serializable;


/**
 * @author Jeroen Gremmen
 */
public abstract class MessagePart implements Serializable
{
  private static final long serialVersionUID = 393381341572711007L;

  @Getter protected final boolean spaceBefore;
  @Getter protected final boolean spaceAfter;


  MessagePart(boolean spaceBefore, boolean spaceAfter)
  {
    this.spaceBefore = spaceBefore;
    this.spaceAfter = spaceAfter;
  }


  public abstract String getText(Parameters parameters);


  public abstract boolean isParameter();
}
