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
      var header = new byte[magicLength + 1];

      if (packStream.read(header) == magicLength + 1 &&
          (header[magicLength] & 0b0100_0000) != 0 &&
          Arrays.equals(PACK_HEADER, 0, magicLength, header, 0, magicLength))
        return (header[magicLength] & 0b1000_0000) != 0 ? MIME_TYPE + "+gzip" : MIME_TYPE;
    } catch(Exception ignored) {
    }

    return null;
  }
}
