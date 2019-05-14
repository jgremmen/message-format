package de.sayayi.lib.message.impl;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.parser.MessagePart;
import lombok.Getter;
import lombok.ToString;


/**
 * @author Jeroen Gremmen
 */
@ToString
public class SinglePartMessage implements Message
{
  private static final long serialVersionUID = 702546983985164717L;

  @Getter private final MessagePart part;


  public SinglePartMessage(MessagePart part) {
    this.part = part;
  }


  @Override
  public String format(Parameters parameters)
  {
    final String text = part.getText(parameters);

    return (text == null) ? "" : text;
  }


  @Override
  public boolean hasParameters() {
    return part.isParameter();
  }
}
