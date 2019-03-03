package de.sayayi.lib.message;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import lombok.ToString;


/**
 * @author Jeroen Gremmen
 */
@ToString
public class SimpleMessageContext implements MessageContext
{
  private final Locale locale;
  private final Map<String,Object> parameterValues;


  private SimpleMessageContext(Locale locale, Map<String,Object> parameterValues)
  {
    this.locale = locale;
    this.parameterValues = parameterValues;
  }


  @Override
  public Locale getLocale() {
    return locale;
  }


  @Override
  public Object getParameterValue(String parameter) {
    return parameterValues.get(parameter);
  }


  public static final SimpleMessageContext.Builder builder() {
    return new Builder();
  }


  public static final class Builder
  {
    private Locale locale;
    private final Map<String,Object> parameterValues;


    private Builder()
    {
      parameterValues = new HashMap<String,Object>();
      locale = Locale.getDefault();
    }


    public Builder with(Locale locale)
    {
      this.locale = locale;
      return this;
    }


    public Builder with(String parameter, boolean value)
    {
      parameterValues.put(parameter, Boolean.valueOf(value));
      return this;
    }


    public Builder with(String parameter, int value)
    {
      parameterValues.put(parameter, Integer.valueOf(value));
      return this;
    }


    public Builder with(String parameter, long value)
    {
      parameterValues.put(parameter, Long.valueOf(value));
      return this;
    }


    public Builder with(String parameter, double value)
    {
      parameterValues.put(parameter, Double.valueOf(value));
      return this;
    }


    public Builder with(String parameter, Object value)
    {
      parameterValues.put(parameter, value);
      return this;
    }


    public MessageContext buildContext() {
      return new SimpleMessageContext(locale, parameterValues);
    }
  }
}
