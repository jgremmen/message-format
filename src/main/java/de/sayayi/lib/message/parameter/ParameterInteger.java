package de.sayayi.lib.message.parameter;

import de.sayayi.lib.message.Message;
import lombok.Getter;


/**
 * @author Jeroen Gremmen
 */
public class ParameterInteger implements ParameterData, Message
{
  private static final long serialVersionUID = 1858469767517999L;

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


  @Override
  public Object asObject() {
    return Integer.valueOf(value);
  }


  @Override
  public boolean hasParameter() {
    return false;
  }
}
