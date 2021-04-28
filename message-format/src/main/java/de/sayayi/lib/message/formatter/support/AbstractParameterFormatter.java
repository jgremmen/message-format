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
import de.sayayi.lib.message.data.DataMap;
import de.sayayi.lib.message.data.DataNumber;
import de.sayayi.lib.message.data.DataString;
import de.sayayi.lib.message.data.map.MapKey.Type;
import de.sayayi.lib.message.data.map.MapValue;
import de.sayayi.lib.message.data.map.MapValueBool;
import de.sayayi.lib.message.data.map.MapValueNumber;
import de.sayayi.lib.message.data.map.MapValueString;
import de.sayayi.lib.message.formatter.ParameterFormatter;
import de.sayayi.lib.message.internal.EmptyMessage;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;

import static de.sayayi.lib.message.data.map.MapKey.EMPTY_NULL_TYPE;
import static de.sayayi.lib.message.data.map.MapKey.NAME_TYPE;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.messageToText;


/**
 * @author Jeroen Gremmen
 */
@SuppressWarnings("WeakerAccess")
public abstract class AbstractParameterFormatter implements ParameterFormatter
{
  /**
   * Name of the formatter resource bundle.
   */
  public static final String FORMATTER_BUNDLE_NAME =
      AbstractParameterFormatter.class.getPackage().getName() + ".Formatter";


  /**
   * A set containing all data map key types, except for {@link Type#NAME}.
   */
  protected static final Set<Type> NO_NAME_KEY_TYPES =
      EnumSet.of(Type.NULL, Type.EMPTY, Type.BOOL, Type.NUMBER, Type.STRING);


  @Override
  public @NotNull Text format(Object value, String format, @NotNull Parameters parameters, Data data)
  {
    // handle empty, !empty, null and !null first
    Message.WithSpaces msg = getMessage(value, EMPTY_NULL_TYPE, parameters, data, false);
    if (msg != null)
      return messageToText(msg, parameters);

    Text text = formatValue(value, format, parameters, data);

    // map result against map keys...
    msg = getMessage(text.getText(), NO_NAME_KEY_TYPES, parameters, data, false);

    return msg == null ? text : messageToText(msg, parameters);
  }


  protected abstract @NotNull Text formatValue(Object value, String format, @NotNull Parameters parameters, Data data);


  public int getPriority() {
    return 0;
  }


  @Contract(pure = true)
  protected Data getConfigValue(@NotNull String name, @NotNull Parameters parameters, Data data, boolean checkDataValue)
  {
    if (data instanceof DataMap)
    {
      MapValue mapValue = ((DataMap)data).find(name,parameters, NAME_TYPE, null);
      if (mapValue != null)
        return mapValue;
    }

    if (checkDataValue && (data instanceof DataString || data instanceof DataNumber))
      return data;

    return null;
  }


  @Contract(pure = true)
  protected String getConfigValueString(@NotNull String name, @NotNull Parameters parameters, Data data,
                                        boolean checkDataString, String defaultValue)
  {
    if (data instanceof DataMap)
    {
      MapValueString string = (MapValueString)((DataMap)data).find(name, parameters, NAME_TYPE, MapValue.STRING_TYPE);
      if (string != null)
        return string.asObject();
    }

    if (checkDataString && data instanceof DataString)
      return ((DataString)data).asObject();

    return defaultValue;
  }


  @Contract(pure = true)
  protected long getConfigValueNumber(@NotNull String name, @NotNull Parameters parameters, Data data,
                                      boolean checkDataNumber, long defaultValue)
  {
    if (data instanceof DataMap)
    {
      MapValueNumber number = (MapValueNumber)((DataMap)data).find(name, parameters, NAME_TYPE, MapValue.NUMBER_TYPE);
      if (number != null)
        return number.asObject();
    }

    if (checkDataNumber && data instanceof DataNumber)
      return ((DataNumber)data).asObject();

    return defaultValue;
  }


  @Contract(pure = true)
  protected boolean getConfigValueBool(@NotNull String name, @NotNull Parameters parameters, Data data,
                                       boolean defaultValue)
  {
    if (!(data instanceof DataMap))
      return defaultValue;

    MapValueBool bool = (MapValueBool)((DataMap)data).find(name, parameters, NAME_TYPE, MapValue.BOOL_TYPE);
    return bool == null ? defaultValue :bool.asObject();
  }


  @Contract(pure = true)
  protected String getConfigFormat(String format, Data data, boolean checkDataString, String defaultFormat)
  {
    String configFormat =
        getConfigValueString("format", Parameters.EMPTY, data, checkDataString, null);

    if (configFormat == null && format != null)
      configFormat = format;

    return configFormat == null ? defaultFormat : configFormat;
  }


  @Contract(pure = true)
  protected Message.WithSpaces getMessage(Object value, Set<Type> keyTypes, Parameters parameters, Data data,
                                          boolean notNull)
  {
    Message.WithSpaces message = null;

    if (data instanceof DataMap)
      message = ((DataMap)data).getMessage(value, parameters, keyTypes, false);

    return message == null && notNull ? EmptyMessage.INSTANCE : message;
  }


  @Contract(pure = true)
  protected String getDataString(Data data) {
    return getDataString(data, null);
  }


  @Contract(pure = true)
  protected String getDataString(Data data, String defaultValue) {
    return (data instanceof DataString) ? ((DataString)data).asObject() : defaultValue;
  }


  @Contract(pure = true)
  protected String formatNull(@NotNull Parameters parameters, Data data) {
    return getMessage(null, EMPTY_NULL_TYPE, parameters, data, true).format(parameters);
  }


  @Contract(pure = true)
  protected String formatEmpty(@NotNull Parameters parameters, Data data) {
    return getMessage("", EMPTY_NULL_TYPE, parameters, data, true).format(parameters);
  }


  @Contract(pure = true)
  protected String formatString(String value, @NotNull Parameters parameters, Data data)
  {
    Message msg = getMessage(value, NO_NAME_KEY_TYPES, parameters, data, false);
    return msg == null ? value : msg.format(parameters);
  }


  @Contract(pure = true)
  protected @NotNull String trimNotNull(String s) {
    return s == null ? "" : s.trim();
  }


  @Contract(pure = true)
  protected @NotNull String trimNotNull(Text text)
  {
    if (text == null)
      return "";

    String s = text.getText();

    return s == null ? "" : s;
  }
}
