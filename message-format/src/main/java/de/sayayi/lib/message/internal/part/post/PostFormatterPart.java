/*
 * Copyright 2026 Jeroen Gremmen
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
package de.sayayi.lib.message.internal.part.post;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.internal.pack.PackSupport;
import de.sayayi.lib.message.internal.part.config.MessagePartConfig;
import de.sayayi.lib.message.part.MessagePart;
import de.sayayi.lib.pack.PackInputStream;
import de.sayayi.lib.pack.PackOutputStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static de.sayayi.lib.message.part.MessagePart.Text.SPACE;
import static de.sayayi.lib.message.part.TextPartFactory.addSpaces;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.util.MessageUtil.serializeMessage;
import static de.sayayi.lib.message.util.MessageUtil.validateName;
import static java.util.Objects.requireNonNull;


/**
 * Internal implementation of {@link MessagePart.PostFormat} that applies a named
 * {@link de.sayayi.lib.message.formatter.post.PostFormatter PostFormatter} to the formatted text of a contained
 * message.
 * <p>
 * This part first formats the embedded message, then passes the resulting text through the registered post formatter
 * identified by {@linkplain #getName() name}. Configuration values and space handling are preserved throughout this
 * process.
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 *
 * @see MessagePart.PostFormat
 * @see de.sayayi.lib.message.formatter.post.PostFormatter PostFormatter
 */
public final class PostFormatterPart implements MessagePart.PostFormat
{
  /** post formatter name. */
  private final @NotNull String name;

  /** message */
  private final @NotNull Message.WithSpaces message;

  /** configuration. */
  private final @NotNull MessagePartConfig config;

  /** tells whether the parameter has a leading space. */
  private final boolean spaceBefore;

  /** tells whether the parameter has a trailing space. */
  private final boolean spaceAfter;


  /**
   * Creates a new post formatter part.
   *
   * @param name         the post formatter name, not empty or {@code null}
   * @param message      the message whose formatted text will be post-processed, not {@code null}
   * @param spaceBefore  whether this part is preceded by a space
   * @param spaceAfter   whether this part is followed by a space
   * @param config       the configuration for the post formatter, not {@code null}
   */
  public PostFormatterPart(@NotNull String name, @NotNull Message.WithSpaces message,
                           boolean spaceBefore, boolean spaceAfter, @NotNull MessagePartConfig config)
  {
    this.name = validateName(name, "post formatter name");
    this.message = requireNonNull(message, "message must not be null");
    this.config = requireNonNull(config, "config must not be null");
    this.spaceBefore = spaceBefore;
    this.spaceAfter = spaceAfter;
  }


  @Override
  public @NotNull String getName() {
    return name;
  }


  @Override
  public @NotNull Message.WithSpaces getMessage() {
    return message;
  }


  @Override
  @Contract(pure = true)
  public @NotNull MessagePart.Config getConfig() {
    return config;
  }


  @Override
  public boolean isSpaceBefore() {
    return spaceBefore;
  }


  @Override
  public boolean isSpaceAfter() {
    return spaceAfter;
  }


  /**
   * Formats the embedded message and applies the named post formatter to the result.
   * <p>
   * If the embedded message formats to empty text, the empty text is returned as-is without invoking the post
   * formatter. Leading and trailing spaces from both this part and the formatted text are merged.
   *
   * @param messageAccessor  accessor for retrieving formatters and locale, not {@code null}
   * @param parameters       formatting parameters, not {@code null}
   *
   * @return  the post-formatted text, never {@code null}
   */
  @Override
  @Contract(pure = true)
  public @NotNull Text getText(@NotNull MessageAccessor messageAccessor, @NotNull Parameters parameters)
  {
    final var text = message.formatAsText(messageAccessor, parameters);
    if (text.isEmpty())
      return text;

    final var postFormatter = requireNonNull(
        messageAccessor.getPostFormatter(name),
        "post formatter with name '" + name + "' has not been registered");

    return addSpaces(
        noSpaceText(postFormatter.format(text.getTextNotNull(), new PostFormatterContextImpl(messageAccessor, config))),
        spaceBefore || text.isSpaceBefore(),
        spaceAfter || text.isSpaceAfter());
  }


  @Override
  public void serialize(@NotNull Context context)
  {
    final var textJoiner = context.textJoiner();

    if (spaceBefore)
      textJoiner.add(SPACE);

    textJoiner.addNoSpace("%(").addNoSpace(name).add(',');
    serializeMessage(context, message, true);

    final var contextWithoutQuotes = context.withoutStringQuote();

    for(var configName: config.getConfigNames())
    {
      textJoiner.add(',').addNoSpace(configName).add(':');
      config.getConfigValue(configName).serialize(contextWithoutQuotes);
    }

    textJoiner.add(')');

    if (spaceAfter)
      textJoiner.add(SPACE);
  }


  @Override
  public boolean equals(Object o)
  {
    return o instanceof PostFormat that &&
        name.equals(that.getName()) &&
        message.equals(that.getMessage()) &&
        spaceBefore == that.isSpaceBefore() &&
        spaceAfter == that.isSpaceAfter() &&
        config.equals(that.getConfig());
  }


  @Override
  public int hashCode() {
    return name.hashCode() * 11 + (spaceBefore ? 8 : 0) + (spaceAfter ? 2 : 0);
  }


  @Override
  @Contract(pure = true)
  public String toString()
  {
    final var s = new StringBuilder("PostFormat(message=").append(message);

    if (!config.isEmpty())
      s.append(",config=").append(config);

    if (spaceBefore && spaceAfter)
      s.append(",space-around");
    else if (spaceBefore)
      s.append(",space-before");
    else if (spaceAfter)
      s.append(",space-after");

    return s.append(')').toString();
  }


  /**
   * Writes this post formatter part to the given pack output stream.
   *
   * @param packStream  data output pack target, not {@code null}
   *
   * @throws IOException  if an I/O error occurs
   */
  public void pack(@NotNull PackOutputStream packStream) throws IOException
  {
    packStream.writeBoolean(spaceBefore);
    packStream.writeBoolean(spaceAfter);
    packStream.writeString(name);
    PackSupport.pack(message, packStream);
    config.pack(packStream);
  }


  /**
   * Reads a post formatter part from the given pack input stream.
   *
   * @param unpack      unpacker instance providing deserialization support, not {@code null}
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked post formatter part, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   */
  public static @NotNull PostFormatterPart unpack(@NotNull PackSupport unpack, @NotNull PackInputStream packStream)
      throws IOException
  {
    final var spaceBefore = packStream.readBoolean();
    final var spaceAfter = packStream.readBoolean();
    final var name = requireNonNull(packStream.readString());
    final var message = unpack.unpackMessageWithSpaces(packStream);

    return new PostFormatterPart(name, message, spaceBefore, spaceAfter, MessagePartConfig.unpack(unpack, packStream));
  }
}
