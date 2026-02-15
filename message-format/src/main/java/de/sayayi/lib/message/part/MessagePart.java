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
package de.sayayi.lib.message.part;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.SpacesAware;
import de.sayayi.lib.message.internal.part.text.NoSpaceTextPart;
import de.sayayi.lib.message.internal.part.text.TextPart;
import de.sayayi.lib.message.part.config.PartConfig;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


/**
 * This interface represents an immutable part of a compiled message (text, parameter or template).
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
public interface MessagePart extends SpacesAware
{
  /**
   * Returns the post formatted text with optional leading/trailing spaces.
   *
   * @param messageAccessor  message support instance, not {@code null}
   * @param parameters       formatting parameters, not {@code null}
   *
   * @return  formatted text, never {@code null}
   */
  @Contract(pure = true)
  @NotNull Text getText(@NotNull MessageAccessor messageAccessor, @NotNull Parameters parameters);




  /**
   * Message part with a name, like parameter or template.
   *
   * @since 0.21.0
   */
  interface NamedMessagePart extends MessagePart
  {
    /**
     * Returns the name for this message part.
     *
     * @return  name, never empty or {@code null}
     */
    @Contract(pure = true)
    @NotNull String getName();
  }




  /**
   * Message part representing text only, optionally decorated with leading/trailing space.
   */
  interface Text extends MessagePart
  {
    /** Message part representing an empty text value. */
    Text EMPTY = new NoSpaceTextPart("");

    /** Message part representing a {@code null} value. */
    Text NULL = new TextPart(null);


    /**
     * Returns the trimmed text for this message part.
     *
     * @return  trimmed text or {@code null}
     *
     * @see #getTextNotNull()
     */
    @Contract(pure = true)
    String getText();


    /**
     * Returns the trimmed text for this message part.
     *
     * @return  trimmed text, never {@code null}
     *
     * @see #getText()
     *
     * @since 0.21.0
     */
    @Contract(pure = true)
    @NotNull String getTextNotNull();


    /**
     * Returns the text for this message part decorated with spaces, if available. If the message
     * text is {@code null} an empty string is returned.
     *
     * @return  text, never {@code null}
     */
    @Contract(pure = true)
    @NotNull String getTextWithSpaces();


    /**
     * Tells if this text message part is empty.
     *
     * @return  {@code true} if this message part is empty, {@code false} otherwise
     */
    @Contract(pure = true)
    boolean isEmpty();


    @Override
    default @NotNull Text getText(@NotNull MessageAccessor messageAccessor, @NotNull Parameters parameters) {
      return this;
    }
  }




  /**
   * Message part representing a parameter to be evaluated during formatting.
   */
  interface Parameter extends NamedMessagePart
  {
    /**
     * Returns the name of the preferred formatter for this parameter.
     *
     * @return  formatter name or {@code null} if the formatter should be determined by the parameter value type
     *          or the default formatter
     */
    @Contract(pure = true)
    String getFormat();


    /**
     * Returns the configuration settings for this parameter.
     *
     * @return  parameter configuration, never {@code null}
     */
    @Contract(pure = true)
    @NotNull PartConfig getConfig();
  }




  /**
   * Message part representing a template reference to be evaluated during formatting.
   *
   * @see MessageAccessor#getTemplateByName(String)
   *
   * @since 0.8.0
   */
  interface Template extends NamedMessagePart {
  }




  /**
   * @since 0.21.0
   */
  interface PostFormat extends NamedMessagePart
  {
    /**
     * Returns the message to be post formatted.
     *
     * @return  message, never {@code null}
     */
    @NotNull Message.WithSpaces getMessage();


    /**
     * Returns the configuration settings for this post format.
     *
     * @return  post format configuration, never {@code null}
     */
    @Contract(pure = true)
    @NotNull PartConfig getConfig();
  }
}
