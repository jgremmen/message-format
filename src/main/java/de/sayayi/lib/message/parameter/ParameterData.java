package de.sayayi.lib.message.parameter;

import de.sayayi.lib.message.Message.Context;


/**
 * @author Jeroen Gremmen
 */
public interface ParameterData
{
  Type getType();


  String format(Context context, Object key);


  String format(Context context);


  public enum Type {
    INTEGER, BOOLEAN, STRING, MAP;
  }
}
