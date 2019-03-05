package de.sayayi.lib.message.formatter;


/**
 * @author Jeroen Gremmen
 */
public interface FormatterService
{
  ParameterFormatter getFormatter(String format, Class<?> type);
}
