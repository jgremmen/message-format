package de.sayayi.lib.message.parameter;

import de.sayayi.lib.message.MessageContext;

/**
 * @author Jeroen Gremmen
 */
public interface ParameterData
{
  Type getType();


  String format(MessageContext context, Object key);


  String format(MessageContext context);


  public enum Type {
    INTEGER, BOOLEAN, STRING, MAP;
  }
}
