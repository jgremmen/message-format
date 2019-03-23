package de.sayayi.lib.message.data;

import de.sayayi.lib.message.Message;
import lombok.Getter;

import java.io.Serializable;


/**
 * @author Jeroen Gremmen
 */
public class ParameterBoolean implements ParameterData, Message
{
  private static final long serialVersionUID = 4014416484591250617L;

  @Getter private final boolean value;


  public ParameterBoolean(boolean value) {
    this.value = value;
  }


  @Override
  public String format(Context context, Serializable key) {
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


  @Override
  public Serializable asObject() {
    return value;
  }


  @Override
  public boolean hasParameter() {
    return false;
  }
}
