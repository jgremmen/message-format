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
package de.sayayi.lib.message.internal;

import de.sayayi.lib.message.Message;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


/**
 * @author Jeroen Gremmen
 */
@SuppressWarnings("squid:S2160")
@ToString
public class MessageDelegateWithCode extends AbstractMessageWithCode
{
  private static final long serialVersionUID = 400L;

  @Getter private final Message message;


  public MessageDelegateWithCode(@NotNull String code, @NotNull Message message)
  {
    super(code);

    this.message = message;
  }


  @Override
  @Contract(pure = true)
  public String format(@NotNull Parameters parameters) {
    return message.format(parameters);
  }


  @Override
  @Contract(pure = true)
  public boolean hasParameters() {
    return message.hasParameters();
  }
}
