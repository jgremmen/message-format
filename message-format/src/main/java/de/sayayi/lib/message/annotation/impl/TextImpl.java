/*
 * Copyright 2021 Jeroen Gremmen
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
package de.sayayi.lib.message.annotation.impl;

import de.sayayi.lib.message.annotation.Text;

import java.lang.annotation.Annotation;


/**
 * {@code Text} annotation implementation.
 *
 * @author Jeroen Gremmen
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public final class TextImpl implements Text
{
  private final String locale;
  private final String text;
  private final String value;


  public TextImpl(String locale, String text, String value)
  {
    this.locale = locale;
    this.text = text;
    this.value = value;
  }


  @Override
  public String locale() {
    return locale == null ? "" : locale;
  }


  @Override
  public String text() {
    return text == null ? "" : text;
  }


  @Override
  public String value() {
    return value == null ? "" : value;
  }


  @Override
  public Class<? extends Annotation> annotationType() {
    return Text.class;
  }


  @Override
  public String toString() {
    return "Text[locale=" + locale() + ",text=" + text() + ",value=" + value() + ']';
  }
}
