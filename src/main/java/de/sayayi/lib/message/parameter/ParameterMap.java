package de.sayayi.lib.message.parameter;

import java.math.BigInteger;
import java.util.Map;
import java.util.Map.Entry;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageContext;
import lombok.ToString;


/**
 * @author Jeroen Gremmen
 *
 */
@ToString
public class ParameterMap implements ParameterData
{
  private final Map<Object,Message> map;


  public ParameterMap(Map<Object,Message> map) {
    this.map = map;
  }


  @Override
  public Type getType() {
    return Type.MAP;
  }


  @Override
  public String format(MessageContext context, Object key)
  {
    Message message = map.get(key);

    if (message == null && key != null)
      for(final Entry<Object,Message> entry: map.entrySet())
        if (compareKey(key, entry.getKey()))
        {
          message = entry.getValue();
          break;
        }

    if (message == null)
      message = map.get(null);

    return (message == null) ? null : message.format(context);
  }


  private boolean compareKey(Object key, Object mapKey)
  {
    try {
      if (key instanceof Boolean || mapKey instanceof Boolean)
        return toBoolean(key) == toBoolean(mapKey);

      if (key instanceof Number || mapKey instanceof Number)
        return toInteger(key) == toInteger(mapKey);

      return String.valueOf(key).equals(String.valueOf(mapKey));
    } catch(final Exception ex) {
      return false;
    }
  }


  private boolean toBoolean(Object value)
  {
    if (value instanceof Boolean)
      return ((Boolean)value).booleanValue();

    if (value instanceof BigInteger)
      return ((BigInteger)value).signum() != 0;

    if (value instanceof Number)
      return ((Number)value).longValue() != 0;

    return Boolean.parseBoolean(String.valueOf(value));
  }


  private int toInteger(Object value)
  {
    if (value instanceof Number)
      return ((Number)value).intValue();

    return Integer.parseInt(String.valueOf(value));
  }


  @Override
  public String format(MessageContext context) {
    return format(context, null);
  }
}
