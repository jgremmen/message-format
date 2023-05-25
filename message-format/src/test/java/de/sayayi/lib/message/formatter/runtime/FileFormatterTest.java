/*
 * Copyright 2020 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import de.sayayi.lib.message.parameter.key.ConfigKeyName;
import de.sayayi.lib.message.parameter.value.ConfigValueString;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.io.File;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.internal.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.internal.part.TextPartFactory.nullText;
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
    val messageAccessor = MessageSupportFactory
        .create(createFormatterService(new FileFormatter()), NO_CACHE_INSTANCE)
        .setLocale(ROOT)
        .getMessageAccessor();
    val f = new File("/path1/path2/filename.ext");

    assertEquals(nullText(), format(messageAccessor, null));
    assertEquals(noSpaceText("/path1/path2/filename.ext"), format(messageAccessor, f));
    assertEquals(noSpaceText("filename.ext"), format(messageAccessor, f,
        singletonMap(new ConfigKeyName("file"), new ConfigValueString("name"))));
    assertEquals(noSpaceText("/path1/path2"), format(messageAccessor, f,
        singletonMap(new ConfigKeyName("file"), new ConfigValueString("parent"))));
    assertEquals(noSpaceText("ext"), format(messageAccessor, f,
        singletonMap(new ConfigKeyName("file"), new ConfigValueString("extension"))));
  }


  @Test
  public void testFormatter()
  {
    val context = MessageSupportFactory
        .create(createFormatterService(new FileFormatter()), NO_CACHE_INSTANCE)
        .setLocale(ROOT);

    assertEquals("file.jpg = image/jpeg", context
        .message("%{f,file:name}  =  %{f,file:extension,'jpg':'image/jpeg'}")
        .with("f", new File("/test/file.jpg"))
        .format());
  }
}
