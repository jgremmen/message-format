package de.sayayi.lib.message.parameter;

import de.sayayi.lib.message.Message;


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
  public String format(Context context, Object key) {
    throw new UnsupportedOperationException();
  }


  @Override
  public String format(Context context) {
    return Boolean.toString(value);
  }


  @Override
  public String toString() {
    return Boolean.toString(value);
  }
}
