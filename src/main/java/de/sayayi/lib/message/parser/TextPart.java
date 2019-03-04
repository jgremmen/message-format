package de.sayayi.lib.message.parser;

import java.io.Serializable;

import de.sayayi.lib.message.Message.Context;
import lombok.ToString;


/**
 * @author Jeroen Gremmen
 */
@ToString
public class TextPart extends MessagePart implements Serializable
{
  private static final long serialVersionUID = 5325056895074186084L;

  private final String text;


  public TextPart(String text, boolean spaceBefore, boolean spaceAfter)
  {
    super(spaceBefore, spaceAfter);

    this.text = text;
  }


  @Override
  public String getText(Context context) {
    return text;
  }
}
