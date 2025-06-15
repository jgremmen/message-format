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

import de.sayayi.lib.pack.PackInputStream;
import de.sayayi.lib.pack.PackOutputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 * @since 0.20.1
 */
@DisplayName("Pack support")
class PackSupportTest
{
  private static final long[] THRESHOLD_VALUES = new long[] {
      0, 7, -1, -8, 8, 135, -9, -1032, 136, 1159, 1160, 132231, 132232, 9223372036854775807L,
      -1033, -132104, -132105, -9223372036854775808L
  };


  @Test
  @DisplayName("Pack/unpack long values with variable bit width")
  void packLongVar() throws IOException
  {
    final var random = new Random();
    final var numbers = new long[100000];

    System.arraycopy(THRESHOLD_VALUES, 0, numbers, 0, THRESHOLD_VALUES.length);

    for(int n = THRESHOLD_VALUES.length; n < numbers.length; n++)
    {
      final var pct = random.nextInt(100);

      numbers[n] = pct < 30
          ? random.nextInt(100) : pct < 40
          ? -random.nextInt(100) : pct < 50
          ? -random.nextInt(1000) : pct < 90
          ? random.nextInt(1_000_000) : random.nextLong();
    }

    final var byteStream = new ByteArrayOutputStream();

    try(var packStream = new PackOutputStream(PackSupport.PACK_CONFIG, false, byteStream)) {
      for(var number: numbers)
        PackSupport.packLongVar(number, packStream);
    }

    var packed = byteStream.toByteArray();

    try(var packStream = new PackInputStream(PackSupport.PACK_CONFIG, new ByteArrayInputStream(packed))) {
      for(var number: numbers)
        assertEquals(number, PackSupport.unpackLongVar(packStream));
    }
  }
}