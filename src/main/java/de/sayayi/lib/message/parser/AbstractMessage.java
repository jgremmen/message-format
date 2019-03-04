package de.sayayi.lib.message.parser;

import java.io.Serializable;

import de.sayayi.lib.message.MessageWithCode;
import lombok.Getter;


/**
 * @author Jeroen Gremmen
 */
public abstract class AbstractMessage implements MessageWithCode, Serializable
{
  private static final long serialVersionUID = 1334376878447581605L;

  @Getter protected final String code;


  public AbstractMessage(String code) {
    this.code = "".equals(code) ? null : code;
  }
}
