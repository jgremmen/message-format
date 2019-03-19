package de.sayayi.lib.message.spi;

import lombok.ToString;


/**
 * @author Jeroen Gremmen
 */
@ToString
public final class EmptyMessageWithCode extends AbstractMessageWithCode
{
  private static final long serialVersionUID = 1334376878447581605L;


  public EmptyMessageWithCode() {
    this(null);
  }


  public EmptyMessageWithCode(String code) {
    super(code);
  }


  @Override
  public String format(Context context) {
    return null;
  }


  @Override
  public boolean hasParameter() {
    return false;
  }
}
