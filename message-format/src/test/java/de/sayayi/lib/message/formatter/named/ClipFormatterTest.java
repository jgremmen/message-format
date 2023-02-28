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

import de.sayayi.lib.message.MessageContext;
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
  public void testFormatterConfig()
  {
    val formatter = new ClipFormatter();

    assertTrue(formatter.getFormattableTypes().isEmpty());
    assertEquals("clip", formatter.getName());
  }


  @Test
  public void testDefault()
  {
    val context = new MessageContext(createFormatterService(new ClipFormatter()), NO_CACHE_INSTANCE);

    assertEquals(noSpaceText("This is a very long text which is clipped at a length of 64 c..."),
        format(context, "This is a very long text which is clipped at a length of 64 characters",
            "clip"));
  }


  @Test
  public void testSize()
  {
    val context = new MessageContext(createFormatterService(new ClipFormatter()), NO_CACHE_INSTANCE);

    assertEquals(noSpaceText("This is a very..."),
        format(context, "This is a very long text which is clipped at a length of 64 characters",
            singletonMap(new ConfigKeyName("clip-size"), new ConfigValueNumber(18)), "clip"));
    assertEquals(noSpaceText("This..."),
        format(context, "This is a very long text",
            singletonMap(new ConfigKeyName("clip-size"), new ConfigValueNumber(2)), "clip"));
  }


  @Test
  public void testWrapper()
  {
    val context = new MessageContext(createFormatterService(
        new ClipFormatter(), new DoubleSupplierFormatter()), NO_CACHE_INSTANCE);

    assertEquals(noSpaceText("3.1415926..."),
        format(context, (DoubleSupplier)() -> Math.PI,
            singletonMap(new ConfigKeyName("clip-size"), new ConfigValueNumber(12)), "clip"));
  }


  @Test
  public void testNull()
  {
    val context = new MessageContext(createFormatterService(new ClipFormatter()), NO_CACHE_INSTANCE);

    assertEquals(nullText(), format(context, (Object)null, "clip"));
  }
}
