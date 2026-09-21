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
package de.sayayi.lib.message.pack;

import de.sayayi.lib.pack.PackConfig;

import java.io.InputStream;
import java.io.OutputStream;


/**
 * Shared constants used for reading and writing the packed (binary) representation of messages and templates.
 * <p>
 * A pack is a compact, serialized form of message data that can be written to and read from a stream, optionally
 * compressed. The constants defined here describe how such packed data is identified and configured.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 * 
 * @see de.sayayi.lib.message.MessageSupport#exportMessages(OutputStream)
 * @see de.sayayi.lib.message.MessageSupport.ConfigurableMessageSupport#importMessages(InputStream) 
 */
public interface PackConstants
{
  /** MIME type used to identify message format pack data. */
  String MIME_TYPE = "application/x-message-format-pack";

  /** Default pack configuration with magic bytes, version range and compression support. */
  PackConfig PACK_CONFIG = new PackConfig
      .Builder()
      .withMagic("%{msg}")
      .withVersionRange(1, 100)
      .withCompressionSupport(true)
      .build();
}
