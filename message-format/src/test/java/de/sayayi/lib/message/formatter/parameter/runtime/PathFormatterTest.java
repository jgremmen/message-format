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
package de.sayayi.lib.message.formatter.parameter.runtime;

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueString;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;
import java.nio.file.Paths;
import java.util.Map;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.part.TextPartFactory.nullText;
import static java.util.Locale.ROOT;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
@DisplayName("File/Path formatter")
class PathFormatterTest extends AbstractFormatterTest
{
  @Test
  @EnabledOnOs({ OS.MAC, OS.LINUX })
  @DisplayName("Format File")
  void formatFile()
  {
    val messageAccessor = MessageSupportFactory
        .create(createFormatterService(new PathFormatter(), new IterableFormatter()), NO_CACHE_INSTANCE)
        .setLocale(ROOT)
        .getMessageAccessor();
    val f = new File("/path1/path2/filename.ext");

    assertEquals(nullText(), format(messageAccessor, null));
    assertEquals(noSpaceText("/path1/path2/filename.ext"), format(messageAccessor, f));
    assertEquals(noSpaceText("filename.ext"), format(messageAccessor, f,
        Map.of("path", new TypedValueString("name")), Map.of()));
    assertEquals(noSpaceText("/path1/path2"), format(messageAccessor, f,
        Map.of("path", new TypedValueString("parent")), Map.of()));
    assertEquals(noSpaceText("ext"), format(messageAccessor, f,
        Map.of("path", new TypedValueString("extension")), Map.of()));
  }


  @Test
  @DisplayName("Format path file extension")
  void formatPathFileExtension()
  {
    val context = MessageSupportFactory
        .create(createFormatterService(new IterableFormatter(), new PathFormatter()), NO_CACHE_INSTANCE)
        .setLocale(ROOT);

    assertEquals("file.jpg = image/jpeg", context
        .message("%{f,path:name}  =  %{f,path:extension,'jpg':'image/jpeg'}")
        .with("f", Paths.get("/test/file.jpg"))
        .format());
  }
}
