package de.sayayi.lib.message.formatter;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.data.ParameterData;

import java.util.Set;


/**
 * @author Jeroen Gremmen
 */
public interface ParameterFormatter
{
  /**
   * Formats the parameter value to a string representation.
   *
   * @param value       parameter value (can be {@code null})
   * @param format      formatter name used by the parameter or {@code null}. Eg.: {@code %{val,myformat}}
   * @param parameters  parameter values available for formatting the current message. Additionally, this instance
   *                    provides access to the formatting registry as well as to the locale. This parameter is never
   *                    {@code null}
   * @param data        parameter data provided by the parameter definition or {@code null}
   *
   * @return  formatted parameter value or {@code null} if this formatter does not produce any output
   */
  String format(Object value, String format, Parameters parameters, ParameterData data);


  /**
   * <p>
   *   Returns a set of java types which are supported by this formatter.
   * </p>
   * On registration {@link FormatterRegistry#addFormatter(ParameterFormatter)} existing types which are also supported
   * by this formatter will be overridden.
   *
   * @return  a set with supported java types for this formatter, not {@code null}
   */
  Set<Class<?>> getFormattableTypes();
}
