package de.sayayi.lib.message;

import lombok.Getter;
import lombok.ToString;


/**
 * @author Jeroen Gremmen
 */
@ToString
public class TextPart implements MessagePart
{
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
  public String getText(MessageContext context) {
    return text;
  }
}
