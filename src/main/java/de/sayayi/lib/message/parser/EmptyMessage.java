package de.sayayi.lib.message.parser;

import lombok.ToString;


/**
 * @author Jeroen Gremmen
 */
@ToString
public final class EmptyMessage extends AbstractMessageWithCode
{
  private static final long serialVersionUID = 1334376878447581605L;


  public EmptyMessage(String code) {
    super(code);
  }


  @Override
  public String format(Context context) {
    return "";
  }


  @Override
  public boolean hasParameter() {
    return false;
  }
}
