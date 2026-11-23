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

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupportFactory;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


/**
 * A template implemented in Java that carries a name, making it eligible for automatic registration via the
 * {@link java.util.ServiceLoader} mechanism.
 * <p>
 * Custom templates implement this interface by providing {@link #getName()}, which returns the kebab-case template
 * name, and {@link #formatAsText(MessageSupport.MessageAccessor, Message.Parameters) formatAsText}, which produces the
 * formatted output. The name is used as the registration key when the template is discovered through
 * {@link MessageSupport.ConfigurableMessageSupport#registerTemplatesFromService(ClassLoader)
 * registerTemplatesFromService}. Service providers are declared for this interface either in {@code module-info.java}
 * ({@code provides de.sayayi.lib.message.template.JavaTemplate with ...}) or in a
 * {@code META-INF/services/de.sayayi.lib.message.template.JavaTemplate} file.
 * <p>
 * Implementations must be thread safe, as a registered template may be used by several threads at the same time.
 * <p>
 * The shared message support ({@link MessageSupportFactory#shared()}) discovers named templates while it is being
 * created. The constructor and static initializers of a named template must therefore not access the shared message
 * support, directly or indirectly. Doing so on the thread creating the shared message support fails with an
 * {@link IllegalStateException}.
 *
 * @since 0.26.0
 */
public non-sealed interface JavaTemplate extends Template
{
  /**
   * Returns the kebab-case name under which this template is registered.
   *
   * @return  template name, never {@code null}
   */
  @Contract(pure = true)
  @NotNull String getName();


  /**
   * {@inheritDoc}
   * <p>
   * This implementation considers {@code template} the same if it is a named template with the same
   * {@linkplain #getName() name}.
   */
  @Contract(pure = true)
  default boolean isSame(@NotNull Template template)
  {
    return
        template instanceof JavaTemplate javaTemplate &&
        getName().equals(javaTemplate.getName());
  }
}
