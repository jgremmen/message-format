package de.sayayi.lib.message.spi;

import java.io.Serializable;

import de.sayayi.lib.message.Message;
import lombok.ToString;


/**
 * @author Jeroen Gremmen
 */
@ToString
public class EmptyMessage implements Message, Serializable
{
  private static final long serialVersionUID = 593052619339233869L;


  @Override
  public String format(Context context) {
    return "";
  }


  @Override
  public boolean hasParameter() {
    return false;
  }
}
