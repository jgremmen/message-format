/*
 * Copyright 2025 Jeroen Gremmen
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
package de.sayayi.lib.message.internal.pack;

import de.sayayi.lib.pack.detector.AbstractTikaDetector;

import static de.sayayi.lib.message.internal.pack.PackSupport.MIME_TYPE;
import static de.sayayi.lib.message.internal.pack.PackSupport.PACK_CONFIG;


/**
 * Tika detector for message format packs.
 *
 * @author Jeroen Gremmen
 * @since 0.20.0
 */
public final class PackTikaDetector extends AbstractTikaDetector
{
  public PackTikaDetector() {
    super(PACK_CONFIG, MIME_TYPE);
  }
}
