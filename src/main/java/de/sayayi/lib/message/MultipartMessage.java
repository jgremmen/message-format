package de.sayayi.lib.message;

import java.util.List;


/**
 * @author Jeroen Gremmen
 */
public class MultipartMessage implements Message
{
  private final List<MessagePart> parts;


  public MultipartMessage(List<MessagePart> parts) {
    this.parts = parts;
  }


  @Override
  public String format(MessageContext context)
  {
    final StringBuilder message = new StringBuilder();
    boolean spaceBefore = false;

    for(final MessagePart part: parts)
    {
      final String text = part.getText(context);

      if (text != null && !text.isEmpty())
      {
        if (spaceBefore || part.isSpaceBefore())
          message.append(' ');

        message.append(text);
        spaceBefore = part.isSpaceAfter();
      }
    }

    return message.toString();
  }
}
