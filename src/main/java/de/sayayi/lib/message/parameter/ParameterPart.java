package de.sayayi.lib.message.parameter;

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.MessagePart;
import lombok.Getter;
import lombok.ToString;


/**
 * @author Jeroen Gremmen
 */
@ToString
public class ParameterPart implements MessagePart
{
  @Getter private final String parameter;
  @Getter private final String format;
  @Getter private final boolean spaceBefore;
  @Getter private final boolean spaceAfter;
  @Getter private final ParameterData data;


  public ParameterPart(String parameter, String format, boolean spaceBefore, boolean spaceAfter, ParameterData data)
  {
    this.parameter = parameter;
    this.format = format;
    this.spaceBefore = spaceBefore;
    this.spaceAfter = spaceAfter;
    this.data = data;
  }


  @Override
  public String getText(MessageContext context)
  {
    final ParameterFormatter formatter = MessageFactory.getFormatter(format);
    if (formatter == null)
      return "??format=" + format + "??";

    return formatter.format(parameter, context, data);
  }
}
