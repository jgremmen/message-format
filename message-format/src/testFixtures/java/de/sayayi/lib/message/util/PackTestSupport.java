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
package de.sayayi.lib.message.util;

import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupport.ConfigurableMessageSupport;
import de.sayayi.lib.pack.PackOutputStream;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.Supplier;

import static de.sayayi.lib.message.pack.PackConstants.PACK_CONFIG;


/**
 * Helpers for tests which write or read message packs.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
public final class PackTestSupport
{
  private PackTestSupport() {
  }


  /**
   * Exports the messages and templates of {@code source} and imports them into a new message support.
   *
   * @param source  message support to export, not {@code null}
   * @param target  supplier for the message support to import into, not {@code null}
   *
   * @return  message support with the imported messages and templates, never {@code null}
   *
   * @throws IOException  if exporting or importing fails
   */
  public static @NotNull ConfigurableMessageSupport packRoundTrip(
      @NotNull MessageSupport source, @NotNull Supplier<ConfigurableMessageSupport> target) throws IOException
  {
    val pack = new ByteArrayOutputStream();

    source.exportMessages(pack);

    return target.get().importMessages(new ByteArrayInputStream(pack.toByteArray()));
  }


  /**
   * Returns the bytes written by {@code writer} to an uncompressed pack stream without a version.
   *
   * @param writer  writer for the pack content, not {@code null}
   *
   * @return  pack bytes, never {@code null}
   *
   * @throws IOException  if writing fails
   */
  public static byte @NotNull [] packBytes(@NotNull PackWriter writer) throws IOException
  {
    val bytes = new ByteArrayOutputStream();

    try(val packStream = new PackOutputStream(PACK_CONFIG, false, bytes)) {
      writer.write(packStream);
    }

    return bytes.toByteArray();
  }


  /**
   * Returns the bytes written by {@code writer} to an uncompressed pack stream with the given version.
   *
   * @param version  pack version
   * @param writer   writer for the pack content, not {@code null}
   *
   * @return  pack bytes, never {@code null}
   *
   * @throws IOException  if writing fails
   */
  public static byte @NotNull [] packBytes(int version, @NotNull PackWriter writer) throws IOException
  {
    val bytes = new ByteArrayOutputStream();

    try(val packStream = new PackOutputStream(PACK_CONFIG, version, false, bytes)) {
      writer.write(packStream);
    }

    return bytes.toByteArray();
  }




  /**
   * Writes content to a pack stream.
   */
  @FunctionalInterface
  public interface PackWriter
  {
    /**
     * Writes content to {@code packStream}.
     *
     * @param packStream  pack stream to write to, not {@code null}
     *
     * @throws IOException  if writing fails
     */
    void write(@NotNull PackOutputStream packStream) throws IOException;
  }
}
