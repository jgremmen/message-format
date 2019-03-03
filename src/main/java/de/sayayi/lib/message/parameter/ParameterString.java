package de.sayayi.lib.message.parameter;

import de.sayayi.lib.message.Message;


/**
 * @author Jeroen Gremmen
 */
public class ParameterString implements ParameterData, Message
{
  private final String string;


  public ParameterString(String string) {
    this.string = string;
  }


  @Override
  public Type getType() {
    return Type.STRING;
  }


  @Override
  public String format(Context context, Object key) {
    throw new UnsupportedOperationException();
  }


  @Override
  public String format(Context context) {
    return string;
  }


  @Override
  public String toString() {
    return string;
  }
}
