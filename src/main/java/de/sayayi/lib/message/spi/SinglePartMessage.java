package de.sayayi.lib.message.spi;

import java.io.Serializable;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.parser.MessagePart;
import lombok.Getter;
import lombok.ToString;


/**
 * @author Jeroen Gremmen
 */
@ToString
public class SinglePartMessage implements Message, Serializable
{
  private static final long serialVersionUID = 702546983985164717L;

  @Getter private final MessagePart part;


  public SinglePartMessage(MessagePart part) {
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
