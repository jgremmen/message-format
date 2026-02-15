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
package de.sayayi.lib.message.internal.part.post;

import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.formatter.post.PostFormatterContext;
import de.sayayi.lib.message.internal.part.config.BasePartConfigAccessor;
import de.sayayi.lib.message.part.config.PartConfig;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;


/**
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
final class PostFormatterContextImpl extends BasePartConfigAccessor implements PostFormatterContext
{
  PostFormatterContextImpl(@NotNull MessageAccessor messageAccessor, @NotNull PartConfig partConfig) {
    super(messageAccessor, partConfig);
  }


  @Override
  public @NotNull Locale getLocale() {
    return messageAccessor.getLocale();
  }
}
