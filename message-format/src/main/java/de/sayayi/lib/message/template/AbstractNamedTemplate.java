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

import org.jetbrains.annotations.NotNull;


/**
 * Base class for custom {@link Template} implementations that can be discovered and registered automatically via the
 * {@link java.util.ServiceLoader} mechanism.
 * <p>
 * Subclasses must implement {@link #getName()} to return the kebab-case template name and
 * {@link #formatAsText(de.sayayi.lib.message.MessageSupport.MessageAccessor,
 * de.sayayi.lib.message.Message.Parameters) formatAsText} to produce the formatted output.
 * <p>
 * This class provides a default {@link #isSame(Template)} implementation that considers two
 * templates the same if the other template is a {@link NamedTemplate} with the same
 * {@linkplain #getName() name}.
 * <p>
 * To make a custom template discoverable by
 * {@link de.sayayi.lib.message.MessageSupport.ConfigurableMessageSupport#registerTemplatesFromService(ClassLoader)
 * registerTemplatesFromService}, declare it as a service provider for
 * {@link NamedTemplate} in your {@code module-info.java}:
 * <pre>
 * provides de.sayayi.lib.message.template.NamedTemplate with com.example.MyCustomTemplate;
 * </pre>
 * or in a {@code META-INF/services/de.sayayi.lib.message.template.NamedTemplate} file.
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 *
 * @see NamedTemplate
 */
public abstract non-sealed class AbstractNamedTemplate implements NamedTemplate
{
  /** {@inheritDoc} */
  @Override
  public boolean isSame(@NotNull Template template)
  {
    return
        template instanceof NamedTemplate namedTemplate &&
        getName().equals(namedTemplate.getName());
  }
}
