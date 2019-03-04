package de.sayayi.lib.message.parser;

import java.io.Serializable;

import de.sayayi.lib.message.MessageWithCode;
import lombok.ToString;


/**
 * @author Jeroen Gremmen
 */
@ToString
public final class EmptyMessage implements MessageWithCode, Serializable
{
  private static final long serialVersionUID = 1334376878447581605L;

  private final String code;


  public EmptyMessage(String code) {
    this.code = "".equals(code) ? null : code;
  }


  @Override
  public String format(Context context) {
    return "";
  }


  @Override
  public String getCode() {
    return code;
  }
}
