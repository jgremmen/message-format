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

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.ParameterFactory;
import de.sayayi.lib.message.formatter.GenericFormatterRegistry;
import org.junit.Test;

import java.io.File;

import static de.sayayi.lib.message.MessageFactory.parse;
import static java.util.Locale.ROOT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


/**
 * @author Jeroen Gremmen
 */
public class FileFormatterTest
{
  @Test
  public void testFormat()
  {
    final FileFormatter formatter = new FileFormatter();
    final Parameters parameters = ParameterFactory.createFor(ROOT).noParameters();

    File f = new File("/path1/path2/filename.ext");

    assertNull(formatter.format(null, null, parameters, null));
    assertEquals("/path1/path2/filename.ext", formatter.format(f, null, parameters, null));
    assertEquals("filename.ext", formatter.format(f, "name", parameters, null));
    assertEquals("/path1/path2", formatter.format(f, "parent", parameters, null));
    assertEquals("ext", formatter.format(f, "extension", parameters, null));
  }


  @Test
  public void testFormatter()
  {
    final GenericFormatterRegistry formatterRegistry = new GenericFormatterRegistry();
    formatterRegistry.addFormatter(new FileFormatter());
    ParameterFactory factory = ParameterFactory.createFor(ROOT, formatterRegistry);

    assertEquals("file.jpg = image/jpeg", parse("%{f,name}  =  %{f,extension,{'jpg':'image/jpeg'}}")
        .format(factory.with("f", new File("/test/file.jpg"))));

  }
}
