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
package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.data.DataMap;
import de.sayayi.lib.message.data.map.MapKeyName;
import de.sayayi.lib.message.data.map.MapValueString;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import de.sayayi.lib.message.formatter.GenericFormatterService;
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
public class FileFormatterTest
{
  @Test
  public void testFormat()
  {
    final FileFormatter formatter = new FileFormatter();
    final MessageContext context =
        new MessageContext(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE, ROOT);
    final Parameters parameters = context.noParameters();

    final File f = new File("/path1/path2/filename.ext");

    assertEquals(nullText(),
        formatter.format(context, null, null, parameters, null));
    assertEquals(noSpaceText("/path1/path2/filename.ext"),
        formatter.format(context, f, null, parameters, null));
    assertEquals(noSpaceText("filename.ext"),
        formatter.format(context, f, null, parameters,
            new DataMap(singletonMap(new MapKeyName("file"), new MapValueString("name")))));
    assertEquals(noSpaceText("/path1/path2"),
        formatter.format(context, f, null, parameters,
            new DataMap(singletonMap(new MapKeyName("file"), new MapValueString("parent")))));
    assertEquals(noSpaceText("ext"),
        formatter.format(context, f, null, parameters,
            new DataMap(singletonMap(new MapKeyName("file"), new MapValueString("extension")))));
  }


  @Test
  public void testFormatter()
  {
    final GenericFormatterService formatterRegistry = new GenericFormatterService();
    formatterRegistry.addFormatter(new FileFormatter());
    final MessageContext context = new MessageContext(formatterRegistry, NO_CACHE_INSTANCE, ROOT);

    assertEquals("file.jpg = image/jpeg", context.getMessageFactory().
        parse("%{f,file:'name'}  =  %{f,file:'extension','jpg':'image/jpeg'}").format(
            context, context.parameters().with("f", new File("/test/file.jpg"))));
  }
}