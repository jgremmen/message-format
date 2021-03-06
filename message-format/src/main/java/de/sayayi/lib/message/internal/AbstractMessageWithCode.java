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
import org.jetbrains.annotations.NotNull;


/**
 * @author Jeroen Gremmen
 */
abstract class AbstractMessageWithCode implements Message.WithCode
{
  private static final long serialVersionUID = 500L;

  @Getter protected final String code;


  @SuppressWarnings("squid:S2589")
  AbstractMessageWithCode(@NotNull String code)
  {
    //noinspection ConstantConditions
    if (code == null || code.isEmpty())
      throw new IllegalArgumentException("message code must not be empty");

    this.code = code;
  }


  @Override
  public int hashCode() {
    return code.hashCode();
  }


  @Override
  public boolean equals(Object o) {
    return this == o || (o instanceof AbstractMessageWithCode && code.equals(((AbstractMessageWithCode)o).code));
  }
}
