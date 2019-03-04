package de.sayayi.lib.message.parameter;

import de.sayayi.lib.message.Message;
import lombok.Getter;


/**
 * @author Jeroen Gremmen
 */
public class ParameterInteger implements ParameterData, Message
{
  @Getter private final int value;


  public ParameterInteger(int value) {
    this.value = value;
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
