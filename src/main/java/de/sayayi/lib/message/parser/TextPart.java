package de.sayayi.lib.message.parser;

import java.io.Serializable;

import de.sayayi.lib.message.Message.Context;
import lombok.Getter;
import lombok.ToString;


/**
 * @author Jeroen Gremmen
 */
@ToString
public class TextPart implements MessagePart, Serializable
{
  private static final long serialVersionUID = 5325056895074186084L;

  private final String text;
  @Getter private final boolean spaceBefore;
  @Getter private final boolean spaceAfter;


  public TextPart(String text, boolean spaceBefore, boolean spaceAfter)
  {
    this.text = text;
    this.spaceBefore = spaceBefore;
    this.spaceAfter = spaceAfter;
  }


  @Override
  public String getText(Context context) {
    return text;
  }
}
