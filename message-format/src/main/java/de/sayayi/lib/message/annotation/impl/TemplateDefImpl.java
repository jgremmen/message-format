/*
 * Copyright 2023 Jeroen Gremmen
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

import de.sayayi.lib.message.annotation.TemplateDef;
import de.sayayi.lib.message.annotation.Text;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.util.Arrays;


/**
 *  {@code TemplateDef} annotation implementation.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public final class TemplateDefImpl implements TemplateDef
{
  private final @NotNull String name;
  private final @NotNull String text;
  private final @NotNull Text[] texts;


  public TemplateDefImpl(@NotNull String name, String text, @NotNull Text[] texts)
  {
    this.name = name == null ? "" : name.trim();
    this.text = text == null ? "" : text.trim();
    this.texts = texts;
  }


  @Override
  public String name() {
    return name;
  }


  @Override
  public String text() {
    return text;
  }


  @Override
  public Text[] texts() {
    return texts;
  }


  @Override
  public Class<? extends Annotation> annotationType() {
    return TemplateDef.class;
  }


  @Override
  public String toString() {
    return "TemplateDef[name=" + name + ",text=" + text + ",texts=" + Arrays.toString(texts) + ']';
  }
}
