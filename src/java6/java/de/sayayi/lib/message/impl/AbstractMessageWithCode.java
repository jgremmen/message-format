package de.sayayi.lib.message.impl;

import de.sayayi.lib.message.MessageWithCode;
import lombok.Getter;


/**
 * @author Jeroen Gremmen
 */
public abstract class AbstractMessageWithCode implements MessageWithCode
{
  private static final long serialVersionUID = 1334376878447581605L;

  @Getter protected final String code;


  AbstractMessageWithCode(String code)
  {
    if (code == null || code.isEmpty())
      throw new IllegalArgumentException("message code must not be empty");

    this.code = code;
  }


  @Override
  public int hashCode() {
    return code.hashCode();
  }


  @Override
  public boolean equals(Object o) {
    return this == o || (o instanceof AbstractMessageWithCode && code.equals(((AbstractMessageWithCode)o).code));
  }
}
