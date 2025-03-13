package de.sayayi.lib.message.internal.pack;

import java.nio.file.Path;
import java.nio.file.spi.FileTypeDetector;
import java.util.Arrays;

import static de.sayayi.lib.message.internal.pack.PackOutputStream.PACK_HEADER;
import static java.nio.file.Files.newInputStream;


/**
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
