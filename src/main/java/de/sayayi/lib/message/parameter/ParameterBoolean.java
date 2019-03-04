package de.sayayi.lib.message.parameter;

import de.sayayi.lib.message.Message;
import lombok.Getter;


/**
 * @author Jeroen Gremmen
 */
public class ParameterBoolean implements ParameterData, Message
{
  @Getter private final boolean value;


  public ParameterBoolean(boolean value) {
    this.value = value;
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
