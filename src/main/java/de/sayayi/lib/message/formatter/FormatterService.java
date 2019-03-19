package de.sayayi.lib.message.formatter;


/**
 * @author Jeroen Gremmen
 */
public interface FormatterService
{
  /**
   * <p>
   *   Returns a parameter formatter for the given {@code format} and {@code type}.
   * </p>
   * <p>
   *   Implementing classes must make sure that for any combination of parameters {@code format} and
   *   {@code type} this function always returns a formatter. A good choice for a default formatter
   *   would be {@link de.sayayi.lib.message.formatter.support.StringFormatter}.
   * </p>
   *
   * @param format  name of the formatter or {@code null}
   * @param type    type of the value to format
   *
   * @return  parameter formatter, never {@code null}
   */
  ParameterFormatter getFormatter(String format, Class<?> type);
}
