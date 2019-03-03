package de.sayayi.lib.message.parameter;

import de.sayayi.lib.message.Message;


/**
 * @author Jeroen Gremmen
 */
public class ParameterInteger implements ParameterData, Message
{
  private final int value;


  public ParameterInteger(int value) {
    this.value = value;
  }


  @Override
  public Type getType() {
    return Type.INTEGER;
  }


  @Override
  public String format(Context context, Object key) {
    throw new UnsupportedOperationException();
  }


  @Override
  public String format(Context context) {
    return Integer.toString(value);
  }


  @Override
  public String toString() {
    return Integer.toString(value);
  }
}
