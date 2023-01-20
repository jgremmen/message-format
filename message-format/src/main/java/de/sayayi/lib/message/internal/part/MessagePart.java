/*
 * Copyright 2020 Jeroen Gremmen
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
package de.sayayi.lib.message.internal.part;

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.SpacesAware;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;


/**
 * @author Jeroen Gremmen
 */
public interface MessagePart extends SpacesAware, Serializable
{
  /**
   * Message part containing text only.
   */
  interface Text extends MessagePart
  {
    Text EMPTY = new NoSpaceTextPart("");
    Text NULL = new TextPart(null);


    /**
     * Returns the text for this message part.
     *
     * @return  trimmed text or {@code null}
     */
    @Contract(pure = true)
    String getText();


    /**
     * Tells if this text message part is empty.
     *
     * @return  {@code true} if this message part is empty, {@code false} otherwise
     */
    @Contract(pure = true)
    boolean isEmpty();
  }




  /**
   * Message part containing a parameter to be evaluated during formatting.
   */
  interface Parameter extends MessagePart
  {
    @Contract(pure = true)
    @NotNull Text getText(@NotNull MessageContext messageContext, @NotNull Parameters parameters);
  }
}