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

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.data.Data;
import de.sayayi.lib.message.data.map.MapKey;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collections;
import java.util.Set;


/**
 * @author Jeroen Gremmen
 */
public final class FileFormatter extends AbstractParameterFormatter
{
  @Override
  public String formatValue(Object value, String format, @NotNull Parameters parameters, Data data)
  {
    String s = value == null ? null : format0((File)value, format, parameters, data);

    if (s == null)
      return formatNull(parameters, data);
    if ((s = s.trim()).isEmpty())
      return formatEmpty(parameters, data);

    return formatString(s, parameters, data);
  }


  private String format0(File file, String format, @NotNull Parameters parameters, Data data)
  {
    if ("name".equals(format))
      return file.getName();
    else if ("path".equals(format))
      return file.getPath();
    else if ("parent".equals(format))
      return file.getParent();
    else if ("extension".equals(format))
    {
      String name = file.getName();
      int dotidx = name.lastIndexOf('.');
      if (dotidx == -1)
        return null;

      String extension = name.substring(dotidx + 1);
      Message msg = getMessage(extension, MapKey.STRING_TYPE, data, false);

      return msg == null ? extension : msg.format(parameters);
    }

    return file.getAbsolutePath();
  }


  @NotNull
  @Override
  @Contract(value = "-> new", pure = true)
  public Set<Class<?>> getFormattableTypes() {
    return Collections.<Class<?>>singleton(File.class);
  }
}
