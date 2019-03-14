package de.sayayi.lib.message.parser;

import java.io.Serializable;

import de.sayayi.lib.message.Message.Context;


/**
 * @author Jeroen Gremmen
 */
public final class TextPart extends MessagePart implements Serializable
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


  @Override
  public boolean isParameter() {
    return false;
  }


  @Override
  public String toString()
  {
    final StringBuilder s = new StringBuilder(getClass().getSimpleName()).append("(text=").append(text);

    if (isSpaceBefore() && isSpaceAfter())
      s.append(", space-around");
    else if (isSpaceBefore())
      s.append(", space-before");
    else if (isSpaceAfter())
      s.append(", space-after");

    return s.append(')').toString();
  }
}
