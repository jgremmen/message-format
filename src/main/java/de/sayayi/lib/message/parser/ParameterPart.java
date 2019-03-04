package de.sayayi.lib.message.parser;

import java.io.Serializable;

import de.sayayi.lib.message.Message.Context;
import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.parameter.ParameterData;
import de.sayayi.lib.message.parameter.ParameterFormatter;
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
    this.format = format;
    this.data = data;
  }


  @Override
  public String getText(Context context)
  {
    final ParameterFormatter formatter = MessageFactory.getFormatter(format);
    if (formatter == null)
      return "??format=" + format + "??";

    return formatter.format(parameter, context, data);
  }
}
