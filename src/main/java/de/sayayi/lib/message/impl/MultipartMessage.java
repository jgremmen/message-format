package de.sayayi.lib.message.impl;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.parser.MessagePart;
import lombok.ToString;

import java.util.List;


/**
 * @author Jeroen Gremmen
 */
@ToString
public class MultipartMessage implements Message
{
  private static final long serialVersionUID = 3562616383044215265L;

  private final MessagePart[] parts;


  public MultipartMessage(List<MessagePart> parts) {
    this.parts = parts.toArray(new MessagePart[0]);
  }


  @Override
  public String format(Parameters parameters)
  {
    final StringBuilder message = new StringBuilder();
    boolean spaceBefore = false;

    for(final MessagePart part: parts)
    {
      final String text = part.getText(parameters);

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


  @Override
  public boolean hasParameters() {
    return parts.length > 0 && (parts.length > 1 || parts[0].isParameter());
  }
}
