package de.sayayi.lib.message.impl;

import lombok.ToString;


/**
 * @author Jeroen Gremmen
 */
@ToString
public final class EmptyMessageWithCode extends AbstractMessageWithCode
{
  private static final long serialVersionUID = -293629874443616813L;


  public EmptyMessageWithCode(String code) {
    super(code);
  }


  @Override
  public String format(Parameters parameters) {
    return null;
  }


  @Override
  public boolean hasParameters() {
    return false;
  }
}
