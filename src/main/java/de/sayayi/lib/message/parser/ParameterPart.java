package de.sayayi.lib.message.parser;

import java.io.Serializable;

import de.sayayi.lib.message.Message.Context;
import de.sayayi.lib.message.formatter.ParameterFormatter;
import de.sayayi.lib.message.parameter.ParameterData;
import lombok.Getter;
import lombok.ToString;


/**
 * @author Jeroen Gremmen
 */
@ToString
public class ParameterPart extends MessagePart implements Serializable
{
  private static final long serialVersionUID = 7026268561936531490L;

  @Getter private final String parameter;
  @Getter private final String format;
  @Getter private final ParameterData data;


  public ParameterPart(String parameter, String format, boolean spaceBefore, boolean spaceAfter, ParameterData data)
  {
    super(spaceBefore, spaceAfter);

    this.parameter = parameter;
    this.format = "".equals(format) ? null : format;
    this.data = data;
  }


  @Override
  public String getText(Context context)
  {
    final Object value = context.getParameterValue(parameter);
    final Class<?> type = (value != null) ? value.getClass() : String.class;

    final ParameterFormatter formatter = context.getFormatter(format, type);
    if (formatter == null)
      throw new IllegalStateException("no matching formatter found for parameter " + parameter);

    return formatter.format(parameter, value, format, context, data);
  }
}
