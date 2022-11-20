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
import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.data.Data;
import de.sayayi.lib.message.data.DataMap;
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
  public @NotNull Text format(@NotNull MessageContext messageContext, Object value, String format,
                              @NotNull Parameters parameters, DataMap data)
  {
    // handle empty, !empty, null and !null first
    Message.WithSpaces msg = getMessage(messageContext, value, EMPTY_NULL_TYPE, parameters, data, false);
    if (msg != null)
      return messageToText(messageContext, msg, parameters);

    final Text text = formatValue(messageContext, value, format, parameters, data);

    // map result against map keys...
    msg = getMessage(messageContext, text.getText(), NO_NAME_KEY_TYPES, parameters, data, false);

    return msg == null ? text : messageToText(messageContext, msg, parameters);
  }


  protected abstract @NotNull Text formatValue(@NotNull MessageContext messageContext, Object value, String format,
                                               @NotNull Parameters parameters, DataMap data);


  public int getPriority() {
    return 0;
  }


  @Contract(pure = true)
  protected Data getConfigValue(@NotNull MessageContext messageContext, @NotNull String name,
                                @NotNull Parameters parameters, DataMap map)
  {
    if (map != null)
    {
      MapValue mapValue = map.find(messageContext, name, parameters, NAME_TYPE, null);
      if (mapValue != null)
        return mapValue;
    }

    return messageContext.getDefaultData(name);
  }


  @Contract(pure = true)
  protected String getConfigValueString(@NotNull MessageContext messageContext, @NotNull String name,
                                        @NotNull Parameters parameters, DataMap map) {
    return getConfigValueString(messageContext, name, parameters, map, null);
  }


  @Contract(pure = true)
  protected String getConfigValueString(@NotNull MessageContext messageContext, @NotNull String name,
                                        @NotNull Parameters parameters, DataMap map, String defaultValue)
  {
    if (map != null)
    {
      final MapValueString string =
          (MapValueString)map.find(messageContext, name, parameters, NAME_TYPE, MapValue.STRING_TYPE);

      if (string != null)
        return string.asObject();
    }

    final MapValue mapValue = messageContext.getDefaultData(name);

    return mapValue instanceof MapValueString ? ((MapValueString)mapValue).asObject() : defaultValue;
  }


  @Contract(pure = true)
  protected long getConfigValueNumber(@NotNull MessageContext messageContext, @NotNull String name,
                                      @NotNull Parameters parameters, DataMap map, long defaultValue)
  {
    if (map != null)
    {
      final MapValueNumber number =
          (MapValueNumber)map.find(messageContext, name, parameters, NAME_TYPE, MapValue.NUMBER_TYPE);

      if (number != null)
        return number.asObject();
    }

    final MapValue mapValue = messageContext.getDefaultData(name);

    return mapValue instanceof MapValueNumber ? ((MapValueNumber)mapValue).asObject() : defaultValue;
  }


  @Contract(pure = true)
  protected boolean getConfigValueBool(@NotNull MessageContext messageContext, @NotNull String name,
                                       @NotNull Parameters parameters, DataMap map) {
    return getConfigValueBool(messageContext, name, parameters, map, false);
  }


  @Contract(pure = true)
  protected boolean getConfigValueBool(@NotNull MessageContext messageContext, @NotNull String name,
                                       @NotNull Parameters parameters, DataMap map, boolean defaultValue)
  {
    if (map == null)
      return defaultValue;

    final MapValueBool bool =
        (MapValueBool)map.find(messageContext, name, parameters, NAME_TYPE, MapValue.BOOL_TYPE);

    if (bool != null)
      return bool.asObject();

    final MapValue mapValue = messageContext.getDefaultData(name);

    return mapValue instanceof MapValueBool ? ((MapValueBool)mapValue).asObject() : defaultValue;
  }


  @Contract(pure = true)
  protected Message.WithSpaces getMessage(@NotNull MessageContext messageContext, Object value, Set<Type> keyTypes,
                                          Parameters parameters, DataMap data, boolean notNull)
  {
    Message.WithSpaces message = null;

    if (data != null)
      message = data.getMessage(messageContext, value, parameters, keyTypes, false);

    return message == null && notNull ? EmptyMessage.INSTANCE : message;
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