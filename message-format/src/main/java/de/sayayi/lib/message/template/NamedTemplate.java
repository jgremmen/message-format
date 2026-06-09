/*
 * Copyright 2026 Jeroen Gremmen
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
package de.sayayi.lib.message.template;

import de.sayayi.lib.message.MessageSupport;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


/**
 * A {@link Template} that carries a name, making it eligible for automatic registration via the
 * {@link java.util.ServiceLoader} mechanism.
 * <p>
 * Implementations should extend {@link AbstractNamedTemplate} rather than implementing this interface directly. The name
 * returned by {@link #getName()} is used as the registration key when the template is discovered through
 * {@link MessageSupport.ConfigurableMessageSupport#registerTemplatesFromService(ClassLoader)
 * registerTemplatesFromService}.
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 *
 * @see AbstractNamedTemplate
 */
public sealed interface NamedTemplate extends Template permits AbstractNamedTemplate
{
  /**
   * Returns the kebab-case name under which this template is registered.
   *
   * @return  template name, never {@code null}
   */
  @Contract(pure = true)
  @NotNull String getName();
}
