/*
 * Copyright 2023 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter.named;

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import de.sayayi.lib.message.formatter.runtime.DoubleSupplierFormatter;
import de.sayayi.lib.message.parameter.key.ConfigKeyName;
import de.sayayi.lib.message.parameter.value.ConfigValueNumber;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.function.DoubleSupplier;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.noSpaceText;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.nullText;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Jeroen Gremmen
 */
public class ClipFormatterTest extends AbstractFormatterTest
{
  @Test
  void testFormatterConfig()
  {
    val formatter = new ClipFormatter();

    assertTrue(formatter.getFormattableTypes().isEmpty());
    assertEquals("clip", formatter.getName());
  }


  @Test
  void testDefault()
  {
    val accessor = MessageSupportFactory
        .create(createFormatterService(new ClipFormatter()), NO_CACHE_INSTANCE)
        .getAccessor();

    assertEquals(noSpaceText("This is a very long text which is clipped at a length of 64 c..."),
        format(accessor, "This is a very long text which is clipped at a length of 64 characters",
            "clip"));
  }


  @Test
  void testSize()
  {
    val accessor = MessageSupportFactory
        .create(createFormatterService(new ClipFormatter()), NO_CACHE_INSTANCE)
        .getAccessor();

    assertEquals(noSpaceText("This is a very..."),
        format(accessor, "This is a very long text which is clipped at a length of 64 characters",
            singletonMap(new ConfigKeyName("clip-size"), new ConfigValueNumber(18)), "clip"));
    assertEquals(noSpaceText("This..."),
        format(accessor, "This is a very long text",
            singletonMap(new ConfigKeyName("clip-size"), new ConfigValueNumber(2)), "clip"));
  }


  @Test
  void testWrapper()
  {
    val accessor = MessageSupportFactory
        .create(createFormatterService(new ClipFormatter(), new DoubleSupplierFormatter()), NO_CACHE_INSTANCE)
        .getAccessor();

    assertEquals(noSpaceText("3.1415926..."),
        format(accessor, (DoubleSupplier)() -> Math.PI,
            singletonMap(new ConfigKeyName("clip-size"), new ConfigValueNumber(12)), "clip"));
  }


  @Test
  void testNull()
  {
    val accessor = MessageSupportFactory
        .create(createFormatterService(new ClipFormatter(), new DoubleSupplierFormatter()), NO_CACHE_INSTANCE)
        .getAccessor();

    assertEquals(nullText(), format(accessor, (Object)null, "clip"));
  }
}
