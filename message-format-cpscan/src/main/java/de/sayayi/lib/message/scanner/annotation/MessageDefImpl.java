/*
 * Copyright 2021 Jeroen Gremmen
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
package de.sayayi.lib.message.scanner.annotation;

import de.sayayi.lib.message.annotation.MessageDef;
import de.sayayi.lib.message.annotation.Text;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.util.Arrays;


/**
 * @author Jeroen Gremmen
 */
@AllArgsConstructor
@SuppressWarnings("ClassExplicitlyAnnotation")
public class MessageDefImpl implements MessageDef
{
  private final String code;
  private final String text;
  private final @NotNull Text[] texts;


  @Override
  public String code() {
    return code == null ? "" : code;
  }


  @Override
  public Text[] texts() {
    return texts;
  }


  @Override
  public String text() {
    return text == null ? "" : text;
  }


  @Override
  public Class<? extends Annotation> annotationType() {
    return MessageDef.class;
  }


  @Override
  public String toString() {
    return "MessageDef[code=" + code() + ",text=" + text() + ",texts=" + Arrays.toString(texts) + ']';
  }
}
