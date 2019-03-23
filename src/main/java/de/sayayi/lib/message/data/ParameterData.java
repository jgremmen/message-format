package de.sayayi.lib.message.data;

import de.sayayi.lib.message.Message.Context;

import java.io.Serializable;


/**
 * @author Jeroen Gremmen
 */
public interface ParameterData extends Serializable
{
  String format(Context context, Serializable key);

  String format(Context context);

  Serializable asObject();
}
