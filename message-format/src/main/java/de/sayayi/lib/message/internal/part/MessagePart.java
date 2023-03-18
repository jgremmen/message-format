/*
 * Copyright 2020 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sayayi.lib.message.internal.part;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.MessageSupport.MessageSupportAccessor;
import de.sayayi.lib.message.SpacesAware;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;


/**
 * This interface represents a part of a compiled message (text, parameter or template).
 *
 * @author Jeroen Gremmen
 */
public interface MessagePart extends SpacesAware, Serializable
{
  /**
   * Message part containing text only.
   */
  interface Text extends MessagePart
  {
    /** Message part representing an empty text value. */
    Text EMPTY = new NoSpaceTextPart("");

    /** Message part representing a {@code null} value. */
    Text NULL = new TextPart(null);


    /**
     * Returns the text for this message part.
     *
     * @return  trimmed text or {@code null}
     */
    @Contract(pure = true)
    String getText();


    /**
     * Returns the text for this message part decorated with spaces, if available.
     *
     * @return  text, never {@code null}
     */
    @Contract(pure = true)
    default @NotNull String getTextWithSpaces()
    {
      String text = getText();
      return text != null ? text : "";
    }


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
    /**
     * Returns the name for this parameter.
     *
     * @return  parameter name, never {@code null}
     */
    @Contract(pure = true)
    @NotNull String getName();


    @Contract(pure = true)
    @NotNull Text getText(@NotNull MessageSupportAccessor messageSupport, @NotNull Parameters parameters);
  }




  /**
   * Message part containing a template reference to be evaluated during formatting.
   *
   * @see MessageSupportAccessor#getTemplateByName(String)
   *
   * @since 0.8.0
   */
  interface Template extends MessagePart
  {
    /**
     * Returns the name of this template.
     *
     * @return  template name, never {@code null}
     */
    @Contract(pure = true)
    @NotNull String getName();


    @Contract(pure = true)
    @NotNull Text getText(@NotNull MessageSupportAccessor messageSupport, @NotNull Parameters parameters);
  }
}
