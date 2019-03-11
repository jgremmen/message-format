package de.sayayi.lib.message.parser;

import java.io.Serializable;

import de.sayayi.lib.message.Message;
import lombok.Getter;
import lombok.ToString;


/**
 * @author Jeroen Gremmen
 */
@ToString
public class SimpleMessage implements Message, Serializable
{
  private static final long serialVersionUID = 702546983985164717L;

  @Getter private final MessagePart part;


  public SimpleMessage(MessagePart part) {
    this.part = part;
  }


  @Override
  public String format(Context context)
  {
    final String text = part.getText(context);

    return (text == null) ? "" : text;
  }


  @Override
  public boolean hasParameter() {
    return part.isParameter();
  }
}
