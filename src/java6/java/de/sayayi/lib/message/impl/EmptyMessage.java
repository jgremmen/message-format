/**
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
import lombok.ToString;


/**
 * @author Jeroen Gremmen
 */
@ToString
public final class EmptyMessage implements Message
{
  private static final long serialVersionUID = 1334376878447581605L;


  @Override
  public String format(Parameters parameters) {
    return null;
  }


  @Override
  public boolean hasParameters() {
    return false;
  }
}
