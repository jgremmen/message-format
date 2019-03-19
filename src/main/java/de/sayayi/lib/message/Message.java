package de.sayayi.lib.message;

import de.sayayi.lib.message.formatter.ParameterFormatter;

import java.io.Serializable;
import java.util.Locale;
import java.util.Set;


/**
 * Messages are thread safe.
 *
 * @author Jeroen Gremmen
 */
public interface Message extends Serializable
{
  /**
   * Formats the message based on the message context provided.
   *
   * @param context  message context
   *
   * @return  formatted message
   */
  String format(Context context);


  /**
   * Tells whether this message contains one or more parameters.
   *
   * @return  {@code true} if this message contains parameters, {@code false} otherwise
   */
  boolean hasParameter();




  /**
   * <p>
   *   The message context provides information required for formatting messages.
   * </p>
   *
   * #see {@link Message#format(Context)}
   */
  public interface Context
  {
    /**
     * Tells for which locale the message must be formatted. If no locale is provided ({@code null}) or if no message is available for the given locale,
     * the formatter will look for a reasonable default message.
     *
     * @return  locale or {@code null}
     */
    Locale getLocale();


    /**
     * Returns the value for the named {@code parameter}.
     *
     * @param parameter  parameter name
     *
     * @return  parameter value of {@code null} if no value is available for the given parameter name
     */
    Object getParameterValue(String parameter);


    /**
     * Returns a set with names for all parameters available in this context.
     *
     * @return  set with all parameter names
     */
    Set<String> getParameterNames();


    ParameterFormatter getFormatter(String format, Class<?> type);
  }
}
