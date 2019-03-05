package de.sayayi.lib.message.formatter;


/**
 * @author Jeroen Gremmen
 */
public interface NamedParameterFormatter extends ParameterFormatter
{
  /**
   * Tells the name of this parameter formatter
   *
   * @return  parameter formatter name, never {@code null}
   */
  String getName();
}
