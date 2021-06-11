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
package de.sayayi.lib.message;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static java.util.Objects.requireNonNull;


/**
 * @author Jeroen Gremmen
 */
public abstract class AbstractScannedMessageBundle extends MessageBundle
{
  protected AbstractScannedMessageBundle(@NotNull MessageFactory messageFactory, @NotNull Set<String> packageNames) {
    this(messageFactory, packageNames, requireNonNull(ClassLoader.getSystemClassLoader()));
  }


  protected AbstractScannedMessageBundle(@NotNull MessageFactory messageFactory, @NotNull Set<String> packageNames,
                                         @NotNull ClassLoader classLoader)
  {
    super(messageFactory);

    scan(packageNames, classLoader);
  }


  @Contract(mutates = "this")
  protected abstract void scan(@NotNull Set<String> packageNames, @NotNull ClassLoader classLoader);


  @Override
  public final void add(@NotNull Class<?> classWithMessages) {
    throw new UnsupportedOperationException();
  }
}
