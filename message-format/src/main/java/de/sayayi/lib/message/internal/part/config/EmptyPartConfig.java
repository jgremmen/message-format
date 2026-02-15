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
package de.sayayi.lib.message.internal.part.config;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.part.config.ConfigKey.Type;
import de.sayayi.lib.message.part.config.ConfigValue;
import de.sayayi.lib.message.part.config.PartConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Locale;
import java.util.Set;


/**
 * Parameter configuration map implementation that contains no values.
 *
 * @since 0.21.0
 */
public enum EmptyPartConfig implements PartConfig
{
  INSTANCE;


  @Override
  public boolean isEmpty() {
    return false;
  }


  @Override
  public boolean hasMessageWithKeyType(@NotNull Type keyType) {
    return false;
  }


  @Override
  public @NotNull @Unmodifiable Set<String> getConfigNames() {
    return Set.of();
  }


  @Override
  public ConfigValue<?> getConfigValue(@NotNull String name) {
    return null;
  }


  @Override
  public ConfigValue<?> getDefaultValue() {
    return null;
  }


  @Override
  public Message.WithSpaces getMessage(@NotNull MessageAccessor messageAccessor, Object key, @NotNull Locale locale,
                                       @NotNull Set<Type> keyTypes, boolean includeDefault) {
    return null;
  }


  @Override
  public @Unmodifiable @NotNull Set<String> getTemplateNames() {
    return Set.of();
  }


  @Override
  public @NotNull PartConfig excludeConfigByName(@NotNull Set<String> configNames) {
    return this;
  }
}
