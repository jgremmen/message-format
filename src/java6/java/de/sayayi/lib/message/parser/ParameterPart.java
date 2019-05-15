package de.sayayi.lib.message.parser;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.data.ParameterData;
import de.sayayi.lib.message.exception.MessageException;
import de.sayayi.lib.message.formatter.ParameterFormatter;
import lombok.Getter;


/**
 * @author Jeroen Gremmen
 */
final class ParameterPart extends MessagePart
{
  private static final long serialVersionUID = 7026268561936531490L;

  @Getter private final String parameter;
  @Getter private final String format;
  @Getter private final ParameterData data;


  ParameterPart(String parameter, String format, boolean spaceBefore, boolean spaceAfter, ParameterData data)
  {
    super(spaceBefore, spaceAfter);

    this.parameter = parameter;
    this.format = "".equals(format) ? null : format;
    this.data = data;
  }


  @Override
  public String getText(Parameters parameters)
  {
    final Object value = parameters.getParameterValue(parameter);
    final Class<?> type = (value != null) ? value.getClass() : String.class;

    final ParameterFormatter formatter = parameters.getFormatter(format, type);
    if (formatter == null)
      throw new IllegalStateException("no matching formatter found for data " + parameter);

    try {
      return formatter.format(value, format, parameters, data);
    } catch(Exception ex) {
      throw new MessageException("failed to format parameter " + parameter, ex);
    }
  }


  @Override
  public boolean isParameter() {
    return true;
  }


  @Override
  public String toString()
  {
    final StringBuilder s = new StringBuilder(getClass().getSimpleName()).append("(data=").append(parameter);

    if (format != null)
      s.append(", format=").append(format);
    if (data != null)
      s.append(", data=").append(data);

    if (isSpaceBefore() && isSpaceAfter())
      s.append(", space-around");
    else if (isSpaceBefore())
      s.append(", space-before");
    else if (isSpaceAfter())
      s.append(", space-after");

    return s.append(')').toString();
  }
}
