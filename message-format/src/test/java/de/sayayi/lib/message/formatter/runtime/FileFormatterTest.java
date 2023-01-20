/*
 * Copyright 2020 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.data.map.MapKeyName;
import de.sayayi.lib.message.data.map.MapValueString;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.io.File;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.noSpaceText;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.nullText;
import static java.util.Collections.singletonMap;
import static java.util.Locale.ROOT;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
public class FileFormatterTest extends AbstractFormatterTest
{
  @Test
  public void testFormat()
  {
    val context = new MessageContext(createFormatterService(new FileFormatter()), NO_CACHE_INSTANCE, ROOT);
    val f = new File("/path1/path2/filename.ext");

    assertEquals(nullText(), format(context, null));
    assertEquals(noSpaceText("/path1/path2/filename.ext"), format(context, f));
    assertEquals(noSpaceText("filename.ext"), format(context, f,
        singletonMap(new MapKeyName("file"), new MapValueString("name"))));
    assertEquals(noSpaceText("/path1/path2"), format(context, f,
        singletonMap(new MapKeyName("file"), new MapValueString("parent"))));
    assertEquals(noSpaceText("ext"), format(context, f,
        singletonMap(new MapKeyName("file"), new MapValueString("extension"))));
  }


  @Test
  public void testFormatter()
  {
    val context = new MessageContext(createFormatterService(new FileFormatter()), NO_CACHE_INSTANCE, ROOT);

    assertEquals("file.jpg = image/jpeg", context.getMessageFactory().
        parse("%{f,file:name}  =  %{f,file:extension,'jpg':'image/jpeg'}").format(
            context, context.parameters().with("f", new File("/test/file.jpg"))));
  }
}