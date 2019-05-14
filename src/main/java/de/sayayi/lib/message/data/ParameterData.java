package de.sayayi.lib.message.data;

import de.sayayi.lib.message.Message.Parameters;

import java.io.Serializable;


/**
 * @author Jeroen Gremmen
 */
public interface ParameterData extends Serializable
{
  String format(Parameters parameters, Serializable key);

  String format(Parameters parameters);

  Serializable asObject();
}
