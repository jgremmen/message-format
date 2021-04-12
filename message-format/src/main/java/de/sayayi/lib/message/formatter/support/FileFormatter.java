/*
 * Copyright 2019 Jeroen Gremmen
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
import de.sayayi.lib.message.data.Data;
import de.sayayi.lib.message.formatter.ParameterFormatter.SizeQueryable;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Set;

import static de.sayayi.lib.message.internal.part.MessagePartFactory.emptyText;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.noSpaceText;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.nullText;
import static java.util.Collections.singleton;


/**
 * @author Jeroen Gremmen
 */
public final class FileFormatter extends AbstractParameterFormatter implements SizeQueryable
{
  @Override
  public @NotNull Text formatValue(Object value, String format, @NotNull Parameters parameters, Data data)
  {
    if (value == null)
      return nullText();

    final File file = (File)value;
    format = getConfigFormat(format, data, true, null);

    if ("name".equals(format))
      return noSpaceText(file.getName());
    else if ("path".equals(format))
      return noSpaceText(file.getPath());
    else if ("parent".equals(format))
      return noSpaceText(file.getParent());
    else if ("extension".equals(format))
    {
      String name = file.getName();
      int dotidx = name.lastIndexOf('.');

      return dotidx == -1 ? emptyText() : noSpaceText(name.substring(dotidx + 1));
    }

    return noSpaceText(file.getAbsolutePath());
  }


  @Override
  public int size(@NotNull Object value)
  {
    final File file = (File)value;
    return file.isFile() && file.canRead() ? (int)file.length() : -1;
  }


  @Override
  @Contract(value = "-> new", pure = true)
  public @NotNull Set<Class<?>> getFormattableTypes() {
    return singleton(File.class);
  }
}
