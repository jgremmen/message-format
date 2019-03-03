package de.sayayi.lib.message.parameter;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageContext;


/**
 * @author Jeroen Gremmen
 */
public class ParameterBoolean implements ParameterData, Message
{
  private final boolean value;


  public ParameterBoolean(boolean value) {
    this.value = value;
  }


  @Override
  public Type getType() {
    return Type.BOOLEAN;
  }


  @Override
  public String format(MessageContext context, Object key) {
    throw new UnsupportedOperationException();
  }


  @Override
  public String format(MessageContext context) {
    return Boolean.toString(value);
  }


  @Override
  public String toString() {
    return Boolean.toString(value);
  }
}
