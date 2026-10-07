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
package de.sayayi.lib.message.internal.formatter;

import de.sayayi.lib.message.formatter.FormatterService;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter;
import de.sayayi.lib.message.formatter.post.PostFormatter;
import de.sayayi.lib.message.part.MessagePart.Config;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;


/**
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@ApiStatus.Internal
public final class FormatterServiceDelegate implements FormatterService
{
  private final FormatterService delegate;


  public FormatterServiceDelegate(@NotNull FormatterService delegate) {
    this.delegate = requireNonNull(delegate, "delegate must not be null");
  }


  @Override
  public @NotNull ParameterFormatter[] getFormatters(String format, @NotNull Class<?> type, Config config) {
    return delegate.getFormatters(format, type, config);
  }


  @Override
  public @UnmodifiableView @NotNull Map<String,PostFormatter> getPostFormatters() {
    return delegate.getPostFormatters();
  }


  @Override
  public @UnmodifiableView @NotNull Set<String> getParameterConfigNames() {
    return delegate.getParameterConfigNames();
  }


  @Override
  public String toString() {
    return delegate.toString();
  }
}
