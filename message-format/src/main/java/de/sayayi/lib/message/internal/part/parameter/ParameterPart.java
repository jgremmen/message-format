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
package de.sayayi.lib.message.internal.part.parameter;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.internal.pack.PackSupport;
import de.sayayi.lib.message.internal.part.config.PartConfigImpl;
import de.sayayi.lib.message.part.MessagePart.Parameter;
import de.sayayi.lib.message.part.config.PartConfig;
import de.sayayi.lib.pack.PackInputStream;
import de.sayayi.lib.pack.PackOutputStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import static de.sayayi.lib.message.part.TextPartFactory.addSpaces;
import static java.util.Objects.requireNonNull;


/**
 * Parameter message part with optional leading and/or trailing spaces.
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
public final class ParameterPart implements Parameter
{
  /** parameter name. */
  private final @NotNull String name;

  /** optional parameter formatter name. */
  private final String format;

  /** parameter configuration. */
  private final @NotNull PartConfigImpl config;

  /** tells whether the parameter has a leading space. */
  private final boolean spaceBefore;

  /** tells whether the parameter has a trailing space. */
  private final boolean spaceAfter;


  /**
   * Construct a parameter part with the given parameter {@code name}.
   *
   * @param name  parameter name, not {@code null} or empty
   */
  public ParameterPart(@NotNull String name) {
    this(name, false, false);
  }


  /**
   * Construct a parameter part with the given parameter {@code name} and optional leading and
   * trailing spaces.
   *
   * @param name  parameter name, not {@code null} or empty
   * @param spaceBefore  adds a leading space to this parameter
   * @param spaceAfter   adds a trailing space to this parameter
   */
  public ParameterPart(@NotNull String name, boolean spaceBefore, boolean spaceAfter) {
    this(name, null, spaceBefore, spaceAfter, new PartConfigImpl(Map.of()));
  }


  /**
   * Construct a parameter part with the given parameter {@code name} and parameter configuration.
   *
   * @param name         parameter name, not {@code null} or empty
   * @param config  parameter configuration, not {@code null}
   */
  public ParameterPart(@NotNull String name, @NotNull PartConfigImpl config) {
    this(name, null, false, false, config);
  }


  public ParameterPart(@NotNull String name, String format, boolean spaceBefore, boolean spaceAfter,
                       @NotNull PartConfigImpl config)
  {
    if ((this.name = requireNonNull(name, "name must not be null")).isEmpty())
      throw new IllegalArgumentException("name must not be empty");

    this.format = "".equals(format) ? null : format;
    this.config = requireNonNull(config, "paramConfig must not be null");
    this.spaceBefore = spaceBefore;
    this.spaceAfter = spaceAfter;
  }


  @Override
  @Contract(pure = true)
  public @NotNull String getName() {
    return name;
  }


  @Override
  @Contract(pure = true)
  public String getFormat() {
    return format;
  }


  @Override
  @Contract(pure = true)
  public @NotNull PartConfig getConfig() {
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


  @Override
  @Contract(pure = true)
  public @NotNull Text getText(@NotNull MessageAccessor messageAccessor, @NotNull Parameters parameters)
  {
    final var context = new ParameterFormatterContextImpl(messageAccessor, parameters,
        parameters.getParameterValue(name), null, format, config);

    return addSpaces(context.delegateToNextFormatter(), spaceBefore, spaceAfter);
  }


  @Override
  public boolean equals(Object o)
  {
    return o instanceof Parameter that &&
        name.equals(that.getName()) &&
        Objects.equals(format, that.getFormat()) &&
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
    final var s = new StringBuilder("Parameter(name=").append(name);

    if (format != null)
      s.append(",format=").append(format);
    if (!config.isEmpty())
      s.append(",map=").append(config);

    if (spaceBefore && spaceAfter)
      s.append(",space-around");
    else if (spaceBefore)
      s.append(",space-before");
    else if (spaceAfter)
      s.append(",space-after");

    return s.append(')').toString();
  }


  /**
   * @param packStream  data output pack target
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   *
   * @hidden
   */
  public void pack(@NotNull PackOutputStream packStream) throws IOException
  {
    packStream.writeBoolean(spaceBefore);
    packStream.writeBoolean(spaceAfter);
    packStream.writeString(format);
    packStream.writeString(name);

    config.pack(packStream);
  }


  /**
   * @param unpack      unpacker instance, not {@code null}
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked parameter part, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   *
   * @hidden
   */
  public static @NotNull ParameterPart unpack(@NotNull PackSupport unpack, @NotNull PackInputStream packStream)
      throws IOException
  {
    final var spaceBefore = packStream.readBoolean();
    final var spaceAfter = packStream.readBoolean();
    final var format = packStream.readString();
    final var name = requireNonNull(packStream.readString());

    return new ParameterPart(name, format, spaceBefore, spaceAfter, PartConfigImpl.unpack(unpack, packStream));
  }
}
