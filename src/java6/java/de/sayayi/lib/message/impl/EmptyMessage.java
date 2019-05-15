package de.sayayi.lib.message.impl;

import de.sayayi.lib.message.Message;
import lombok.ToString;


/**
 * @author Jeroen Gremmen
 */
@ToString
public final class EmptyMessage implements Message
{
  private static final long serialVersionUID = 1334376878447581605L;


  @Override
  public String format(Parameters parameters) {
    return null;
  }


  @Override
  public boolean hasParameters() {
    return false;
  }
}
