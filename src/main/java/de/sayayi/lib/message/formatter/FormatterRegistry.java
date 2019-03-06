package de.sayayi.lib.message.formatter;


/**
 * @author Jeroen Gremmen
 */
public interface FormatterRegistry extends FormatterService
{
  void addFormatterForType(Class<?> type, ParameterFormatter formatter);

  void addFormatter(ParameterFormatter formatter);
}
