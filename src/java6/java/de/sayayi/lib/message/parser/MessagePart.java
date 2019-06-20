/*
 * Copyright 2019 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sayayi.lib.message.parser;

import de.sayayi.lib.message.Message.Parameters;
import lombok.Getter;

import java.io.Serializable;


/**
 * @author Jeroen Gremmen
 */
public abstract class MessagePart implements Serializable
{
  private static final long serialVersionUID = 393381341572711007L;

  @Getter protected final boolean spaceBefore;
  @Getter protected final boolean spaceAfter;


  MessagePart(boolean spaceBefore, boolean spaceAfter)
  {
    this.spaceBefore = spaceBefore;
    this.spaceAfter = spaceAfter;
  }


  public abstract String getText(Parameters parameters);


  public abstract boolean isParameter();
}
