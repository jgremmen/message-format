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
package de.sayayi.lib.message.scanner.asm;

import de.sayayi.lib.message.AbstractScannedMessageBundle;
import de.sayayi.lib.message.MessageFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Set;


/**
 * @author Jeroen Gremmen
 */
public class AsmClassPathMessageBundle extends AbstractScannedMessageBundle
{
  public AsmClassPathMessageBundle(@NotNull MessageFactory messageFactory, @NotNull Set<String> packageNames) {
    super(messageFactory, packageNames);
  }


  public AsmClassPathMessageBundle(@NotNull MessageFactory messageFactory, @NotNull Set<String> packageNames,
                                   ClassLoader classLoader) {
    super(messageFactory, packageNames, classLoader);
  }


  @Override
  protected void scan(@NotNull Set<String> packageNames, @NotNull ClassLoader classLoader) {
    new AsmClassPathScanner(this, packageNames, classLoader).run();
  }
}
