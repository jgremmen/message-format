package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.Context;
import de.sayayi.lib.message.formatter.NamedParameterFormatter;
import de.sayayi.lib.message.parameter.ParameterData;
import de.sayayi.lib.message.parameter.ParameterMap;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.util.ResourceBundle.getBundle;


/**
 * @author Jeroen Gremmen
 */
public class BoolFormatter implements NamedParameterFormatter
{
  @Override
  public String getName() {
    return "bool";
  }


  @Override
  public String format(String parameter, Object value, String format, Context context, ParameterData data)
  {
    Boolean bool;

    if (value instanceof Boolean)
      bool = (Boolean)value;
    else if (value instanceof BigInteger)
      bool = Boolean.valueOf(((BigInteger)value).signum() != 0);
    else if (value instanceof BigDecimal)
      bool = Boolean.valueOf(((BigDecimal)value).signum() != 0);
    else if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long)
      bool = Boolean.valueOf(((Number)value).longValue() != 0);
    else if (value instanceof Number)
      bool = Boolean.valueOf(Math.signum(((Number)value).doubleValue()) != 0);
    else
      bool = Boolean.valueOf(String.valueOf(value));

    // allow custom messages for true/false value?
    if (data instanceof ParameterMap)
    {
      final Message message = ((ParameterMap)data).getMessageForKey(bool);
      if (message != null)
        return message.format(context);
    }

    return getBundle(getClass().getPackage().getName() + ".Formatter",
        context.getLocale()).getString(bool.toString());
  }


  @Override
  public Set<Class<?>> getFormattableTypes() {
    return new HashSet<Class<?>>(Arrays.<Class<?>>asList(Boolean.class, boolean.class));
  }
}
