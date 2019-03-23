package de.sayayi.lib.message.data;

import de.sayayi.lib.message.Message;
import lombok.Getter;

import java.io.Serializable;


/**
 * @author Jeroen Gremmen
 */
public class ParameterString implements ParameterData, Message
{
  private static final long serialVersionUID = 6702969353416992174L;

  @Getter private final String value;


  public ParameterString(String value) {
    this.value = value;
  }


  @Override
  public String format(Context context, Serializable key) {
    throw new UnsupportedOperationException();
  }


  @Override
  public String format(Context context) {
    return value;
  }


  @Override
  public String toString() {
    return value;
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
