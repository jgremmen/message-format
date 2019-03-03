package de.sayayi.lib.message;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.sayayi.lib.message.Message.Context;
import lombok.ToString;


/**
 * @author Jeroen Gremmen
 */
@ToString
public final class MessageContext implements Context
{
  private final Locale locale;
  private final Map<String,Object> parameterValues;


  private MessageContext(Locale locale, Map<String,Object> parameterValues)
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


  @Override
  public Set<String> getParameters() {
    return Collections.unmodifiableSet(parameterValues.keySet());
  }


  public static MessageContext.Builder builder() {
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


    public Builder withLocale(Locale locale)
    {
      this.locale = locale;
      return this;
    }


    public Builder withParameter(String parameter, boolean value)
    {
      parameterValues.put(parameter, Boolean.valueOf(value));
      return this;
    }


    public Builder withParameter(String parameter, int value)
    {
      parameterValues.put(parameter, Integer.valueOf(value));
      return this;
    }


    public Builder withParameter(String parameter, long value)
    {
      parameterValues.put(parameter, Long.valueOf(value));
      return this;
    }


    public Builder withParameter(String parameter, double value)
    {
      parameterValues.put(parameter, Double.valueOf(value));
      return this;
    }


    public Builder withParameter(String parameter, Object value)
    {
      parameterValues.put(parameter, value);
      return this;
    }


    public Builder withParameters(Map<String,Object> parameterValues)
    {
      this.parameterValues.putAll(parameterValues);
      return this;
    }


    public Context buildContext() {
      return new MessageContext(locale, parameterValues);
    }
  }
}
