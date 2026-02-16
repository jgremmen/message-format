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
 * This class represents the message parameter configuration map.
 *
 * @author Jeroen Gremmen
 * @since 0.4.0
 */
public final class MessagePartConfig implements MessagePart.Config
{
  public static final MessagePartConfig EMPTY_CONFIG = new MessagePartConfig(Map.of());

  private final @NotNull Map<String, TypedValue<?>> config;


  public MessagePartConfig(@NotNull Map<String, TypedValue<?>> config) {
    this.config = config;
  }


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
    var templateNames = new TreeSet<String>();

    for(var configValue: config.values())
      if (configValue instanceof TypedValueMessage)
        templateNames.addAll(((TypedValueMessage)configValue).asObject().getTemplateNames());

    return unmodifiableSet(templateNames);
  }


  @Override
  public boolean equals(Object o) {
    return o instanceof MessagePartConfig that && config.equals(that.config);
  }


  @Override
  public int hashCode() {
    return config.hashCode();
  }


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


  public void pack(@NotNull PackOutputStream packStream) throws IOException
  {
    packStream.writeSmallVar(config.size());

    for(var configEntry: config.entrySet())
    {
      packStream.writeString(configEntry.getKey());
      PackSupport.pack(configEntry.getValue(), packStream);
    }
  }


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
