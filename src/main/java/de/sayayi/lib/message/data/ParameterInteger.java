package de.sayayi.lib.message.data;

import de.sayayi.lib.message.Message;
import lombok.Getter;

import java.io.Serializable;


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
  public String format(Context context, Serializable key) {
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
  public Serializable asObject() {
    return value;
  }


  @Override
  public boolean hasParameter() {
    return false;
  }
}
