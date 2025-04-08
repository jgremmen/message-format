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

import java.nio.file.Path;
import java.nio.file.spi.FileTypeDetector;
import java.util.Arrays;

import static de.sayayi.lib.message.internal.pack.PackOutputStream.PACK_HEADER;
import static java.nio.file.Files.newInputStream;


/**
 * File type detector for message format packs.
 *
 * @author Jeroen Gremmen
 * @since 0.12.1
 */
public final class PackFileTypeDetector extends FileTypeDetector
{
  private static final String MIME_TYPE = "application/x-message-format-pack";


  @Override
  public String probeContentType(Path path)
  {
    try(var packStream = newInputStream(path)) {
      var magicLength = PACK_HEADER.length;
      var header = new byte[magicLength + 2];

      if (packStream.read(header) == magicLength + 2 &&
          (header[magicLength] & 0b0101_1011) == 0b0101_1011 &&
          Arrays.equals(PACK_HEADER, 0, magicLength, header, 0, magicLength))
      {
        return MIME_TYPE +
            ";version=" + (header[magicLength + 1] & 0xff) +
            ";compress=" + ((header[magicLength] & 0b1000_0000) != 0);
      }
    } catch(Exception ignored) {
    }

    return null;
  }
}
