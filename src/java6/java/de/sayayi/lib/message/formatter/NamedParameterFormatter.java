package de.sayayi.lib.message.formatter;


/**
 * @author Jeroen Gremmen
 */
public interface NamedParameterFormatter extends ParameterFormatter
{
  /**
   * Tells the name of this data formatter
   *
   * @return  data formatter name, never {@code null}
   */
  String getName();
}
