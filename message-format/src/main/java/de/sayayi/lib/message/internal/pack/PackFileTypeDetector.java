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

import de.sayayi.lib.pack.detector.AbstractFileTypeDetector;

import static de.sayayi.lib.message.internal.pack.PackSupport.MIME_TYPE;
import static de.sayayi.lib.message.internal.pack.PackSupport.PACK_CONFIG;


/**
 * File type detector for message format pack files. This detector uses the
 * {@linkplain PackSupport#PACK_CONFIG pack configuration} to identify files by their magic bytes
 * and reports the {@linkplain PackSupport#MIME_TYPE message format pack MIME type}.
 *
 * @author Jeroen Gremmen
 * @since 0.12.1
 */
public final class PackFileTypeDetector extends AbstractFileTypeDetector
{
  /** Creates a new file type detector for message format pack files. */
  public PackFileTypeDetector() {
    super(PACK_CONFIG, MIME_TYPE);
  }
}
