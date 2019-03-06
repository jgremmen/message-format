package de.sayayi.lib.message;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.sayayi.lib.message.Message.Context;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import de.sayayi.lib.message.formatter.FormatterService;
import de.sayayi.lib.message.formatter.ParameterFormatter;
import lombok.Getter;
import lombok.ToString;


/**
 * @author Jeroen Gremmen
 */
@ToString
public final class MessageContext implements Context
{
  @Getter private final Locale locale;
  private final Map<String,Object> parameterValues;
  @Getter private final FormatterService formatters;


  private MessageContext(Locale locale, Map<String,Object> parameterValues, FormatterService formatters)
  {
    this.locale = locale;
    this.parameterValues = parameterValues;
    this.formatters = formatters;
  }


  @Override
  public Object getParameterValue(String parameter) {
    return parameterValues.get(parameter);
  }


  @Override
  public Set<String> getParameters() {
    return Collections.unmodifiableSet(parameterValues.keySet());
  }


  @Override
  public ParameterFormatter getFormatter(String format, Class<?> type) {
    return formatters.getFormatter(format, type);
  }


  public static MessageContext.Builder builder() {
    return new Builder();
  }




  public static final class Builder
  {
    private Locale locale;
    private FormatterService formatters;
    private final Map<String,Object> parameterValues;


    private Builder()
    {
      parameterValues = new HashMap<String,Object>();
      locale = Locale.getDefault();
      formatters = DefaultFormatterService.getSharedInstance();
    }


    public Builder withLocale(Locale locale)
    {
      this.locale = (locale == null) ? Locale.ROOT : locale;
      return this;
    }


    public Builder withFormatterService(FormatterService formatters)
    {
      this.formatters = formatters;
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


    public Builder withParameter(String parameter, float value)
    {
      parameterValues.put(parameter, Float.valueOf(value));
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
      return new MessageContext(locale, parameterValues, formatters);
    }
  }
}
