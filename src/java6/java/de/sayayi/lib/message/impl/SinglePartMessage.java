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
package de.sayayi.lib.message.impl;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.parser.MessagePart;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


/**
 * @author Jeroen Gremmen
 */
@ToString
public class SinglePartMessage implements Message
{
  private static final long serialVersionUID = 201L;

  @Getter private final MessagePart part;


  public SinglePartMessage(@NotNull MessagePart part) {
    this.part = part;
  }


  @Override
  @Contract(pure = true)
  public String format(@NotNull Parameters parameters) {
    return part.getText(parameters);
  }


  @Override
  @Contract(pure = true)
  public boolean hasParameters() {
    return part.isParameter();
  }
}
