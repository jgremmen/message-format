/*
 * Copyright 2026 Jeroen Gremmen
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
package de.sayayi.lib.message.internal.part.map;

import de.sayayi.lib.message.part.MapKey;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

import static de.sayayi.lib.message.part.MapKey.CompareType.EQ;
import static de.sayayi.lib.message.part.MapKey.CompareType.NE;
import static de.sayayi.lib.message.part.MapKey.Type.*;


/**
 * This record represents a configuration key with an order for sorting.
 *
 * @param order      the order of the configuration key, used for sorting
 * @param mapKey  the configuration key, not {@code null}
 *
 * @since 0.21.0
 */
record OrderedConfigKey(int order, @NotNull MapKey mapKey)
{
  static final Comparator<OrderedConfigKey> SORTER = new Comparator<>() {
    @Override
    public int compare(OrderedConfigKey k1, OrderedConfigKey k2)
    {
      var cmp = Integer.compare(configKeyToOrder(k1.mapKey()), configKeyToOrder(k2.mapKey()));
      if (cmp == 0)
        cmp = Integer.compare(k1.order(), k2.order());

      return cmp;
    }


    @Contract(pure = true)
    private static int configKeyToOrder(@NotNull MapKey mapKey)
    {
      final var compareType = mapKey.getCompareType();
      final var keyType = mapKey.getType();

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
  };
}
