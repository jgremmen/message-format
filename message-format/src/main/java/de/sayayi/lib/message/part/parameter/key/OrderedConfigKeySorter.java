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
package de.sayayi.lib.message.part.parameter.key;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Map;

import static de.sayayi.lib.message.part.parameter.key.ConfigKey.CompareType.EQ;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.CompareType.NE;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.Type.*;


/**
 * This sorter sorts parameter configuration keys.
 *
 * @see de.sayayi.lib.message.part.parameter.ParameterConfig#ParameterConfig(Map)
 *
 * @author Jeroen Gremmen
 * @since 0.8.4
 */
public enum OrderedConfigKeySorter implements Comparator<OrderedConfigKeySorter.OrderedConfigKey>
{
  INSTANCE;


  @Override
  public int compare(OrderedConfigKey k1, OrderedConfigKey k2)
  {
    int cmp = Integer.compare(configKeyToOrder(k1.configKey), configKeyToOrder(k2.configKey));
    if (cmp == 0)
      cmp = Integer.compare(k1.order, k2.order);

    return cmp;
  }


  @Contract(pure = true)
  private int configKeyToOrder(@NotNull ConfigKey configKey)
  {
    var compareType = configKey.getCompareType();
    var keyType = configKey.getType();

    if (keyType == NULL && compareType == EQ)
      return 0;  // =null
    if (keyType == EMPTY && compareType == EQ)
      return 1;  // =empty
    if (keyType == BOOL)
      return 2;  // bool
    if (keyType == NUMBER)
      return 3;  // number
    if (keyType == STRING)
      return 4;  // string
    if (keyType == EMPTY && compareType == NE)
      return 5;  // !empty
    if (keyType == NULL && compareType == NE)
      return 6;  // !null

    return 7;  // (default)
  }




  public static final class OrderedConfigKey
  {
    private final int order;
    private final @NotNull ConfigKey configKey;


    public OrderedConfigKey(int order, @NotNull ConfigKey configKey)
    {
      this.order = order;
      this.configKey = configKey;
    }


    @Contract(pure = true)
    public @NotNull ConfigKey getConfigKey() {
      return configKey;
    }
  }
}
