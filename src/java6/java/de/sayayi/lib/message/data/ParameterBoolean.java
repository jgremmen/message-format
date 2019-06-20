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
package de.sayayi.lib.message.data;

import de.sayayi.lib.message.Message;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;


/**
 * @author Jeroen Gremmen
 */
public class ParameterBoolean implements ParameterData, Message
{
  private static final long serialVersionUID = 4014416484591250617L;

  @Getter private final boolean value;


  public ParameterBoolean(boolean value) {
    this.value = value;
  }


  @Override
  public String format(@NotNull Parameters parameters, Serializable key) {
    throw new UnsupportedOperationException();
  }


  @Override
  public String format(@NotNull Parameters parameters) {
    return Boolean.toString(value);
  }


  @Override
  public String toString() {
    return Boolean.toString(value);
  }


  @Override
  public Serializable asObject() {
    return value;
  }


  @Override
  public boolean hasParameters() {
    return false;
  }
}
