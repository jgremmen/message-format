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
package de.sayayi.lib.message.internal.part.config;

import de.sayayi.lib.message.internal.pack.PackSupport;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueMessage;
import de.sayayi.lib.message.part.MessagePart;
import de.sayayi.lib.message.part.TypedValue;
import de.sayayi.lib.pack.PackInputStream;
import de.sayayi.lib.pack.PackOutputStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.util.*;

import static java.util.Collections.unmodifiableSet;


/**
 * Internal implementation of {@link MessagePart.Config} backed by a name-to-value map. This class holds the
 * configuration entries for message parameters and post-formatters.
 *
 * @author Jeroen Gremmen
 * @since 0.4.0
 */
public final class MessagePartConfig implements MessagePart.Config
{
  /** Shared empty configuration instance. */
  public static final MessagePartConfig EMPTY_CONFIG = new MessagePartConfig(Map.of());

  /** The configuration map, keyed by config name. */
  private final @NotNull Map<String,TypedValue<?>> config;


  /**
   * Creates a new configuration backed by the given map.
   *
   * @param config  the configuration map, not {@code null}
   */
  public MessagePartConfig(@NotNull Map<String,TypedValue<?>> config) {
    this.config = config;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull MessagePart.Config excludeConfigByName(@NotNull Set<String> configNames)
  {
    if (configNames.isEmpty())
      return this;

    final var modifiedConfig = new HashMap<>(config);
    modifiedConfig.keySet().removeAll(configNames);

    return new MessagePartConfig(modifiedConfig);
  }


  /**
   * Tells whether the parameter configuration contains any values.
   *
   * @return  {@code true} if the parameter configuration contains at least 1 value,
   *          {@code false} otherwise
   */
  @Override
  @Contract(pure = true)
  public boolean isEmpty() {
    return config.isEmpty();
  }


  /**
   * Returns a set of config names defined in the parameter configuration map.
   *
   * @return  unmodifiable set of config names, never {@code null}
   *
   * @since 0.20.0
   */
  @Override
  @Contract(pure = true)
  public @NotNull @Unmodifiable Set<String> getConfigNames() {
    return unmodifiableSet(config.keySet());
  }


  /**
   * {@inheritDoc}
   */
  @Override
  @Contract(pure = true)
  public TypedValue<?> getConfigValue(@NotNull String name) {
    return config.get(name);
  }


  /**
   * Returns a set of template names referenced in all message values which are available in
   * the message parameter configuration.
   *
   * @return  unmodifiable set of all referenced template names, never {@code null}
   */
  @Override
  @Contract(pure = true)
  @Unmodifiable
  public @NotNull Set<String> getTemplateNames()
  {
    final var templateNames = new TreeSet<String>();

    for(var configValue: config.values())
      if (configValue instanceof TypedValueMessage)
        templateNames.addAll(((TypedValueMessage)configValue).asObject().getTemplateNames());

    return unmodifiableSet(templateNames);
  }


  /**
   * Compares this configuration with another object for equality based on the underlying configuration map.
   *
   * @param o  the object to compare with
   *
   * @return  {@code true} if {@code o} is a {@code MessagePartConfig} with an equal
   *          configuration map
   */
  @Override
  public boolean equals(Object o) {
    return o instanceof MessagePartConfig that && config.equals(that.config);
  }


  /**
   * Returns the hash code based on the underlying configuration map.
   *
   * @return  hash code
   */
  @Override
  public int hashCode() {
    return config.hashCode();
  }


  /**
   * Returns a string representation of this configuration in the form
   * <code>{key1=value1,key2=value2,...}</code>.
   *
   * @return  string representation, never {@code null}
   */
  @Override
  @Contract(pure = true)
  public String toString()
  {
    final var s = new StringJoiner(",", "{", "}");

    // config
    for(var configEntry: config.entrySet())
      s.add(configEntry.getKey() + '=' + configEntry.getValue());

    return s.toString();
  }


  /**
   * Writes this configuration to the given pack output stream for binary serialization.
   *
   * @param packStream  data output pack target, not {@code null}
   *
   * @throws IOException  if an I/O error occurs
   */
  public void pack(@NotNull PackOutputStream packStream) throws IOException
  {
    packStream.writeSmallVar(config.size());

    for(var configEntry: config.entrySet())
    {
      packStream.writeString(configEntry.getKey());
      PackSupport.pack(configEntry.getValue(), packStream);
    }
  }


  /**
   * Reads a {@code MessagePartConfig} from the given pack input stream.
   *
   * @param unpack      unpacker instance, not {@code null}
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked configuration, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   */
  @SuppressWarnings("unchecked")
  public static @NotNull MessagePartConfig unpack(@NotNull PackSupport unpack, @NotNull PackInputStream packStream)
      throws IOException
  {
    final var size = packStream.readSmallVar();
    final var config = new Map.Entry[size];

    for(var n = 0; n < size; n++)
      config[n] = Map.entry(packStream.readString(), unpack.unpackTypedValue(packStream));

    return new MessagePartConfig(Map.ofEntries(config));
  }
}
