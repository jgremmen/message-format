package de.sayayi.lib.message;

import de.sayayi.lib.message.formatter.ParameterFormatter;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * Messages are thread safe.
 *
 * @author Jeroen Gremmen
 */
public interface Message extends Serializable
{
  /**
   * Formats the message based on the message parameters provided.
   *
   * @param parameters  message parameters
   *
   * @return  formatted message
   */
  String format(Parameters parameters);


  /**
   * Tells whether this message contains one or more parameters.
   *
   * @return  {@code true} if this message contains parameters, {@code false} otherwise
   */
  boolean hasParameters();




  interface Parameters
  {
    /**
     * Tells for which locale the message must be formatted. If no locale is provided ({@code null}) or if no message is available for the given locale,
     * the formatter will look for a reasonable default message.
     *
     * @return  locale, never {@code null}
     */
    Locale getLocale();


    ParameterFormatter getFormatter(String format, Class<?> type);


    /**
     * Returns the value for the named {@code data}.
     *
     * @param parameter  data name
     *
     * @return  data value of {@code null} if no value is available for the given data name
     */
    Object getParameterValue(String parameter);


    /**
     * Returns a set with names for all parameters available in this context.
     *
     * @return  set with all data names
     */
    Set<String> getParameterNames();
  }


  interface ParameterBuilder extends Parameters
  {
    ParameterBuilder clear();


    ParameterBuilder with(String parameter, boolean value);


    ParameterBuilder with(String parameter, int value);


    ParameterBuilder with(String parameter, long value);


    ParameterBuilder with(String parameter, float value);


    ParameterBuilder with(String parameter, double value);


    ParameterBuilder with(String parameter, Object value);


    ParameterBuilder with(Map<String,Object> parameterValues);


    ParameterBuilder withLocale(Locale locale);


    ParameterBuilder withLocale(String locale);
  }
}
