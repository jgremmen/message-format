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
 * This record represents a map key with an insertion order for stable sorting. Map keys are sorted first by their
 * logical sort order and then by insertion order to preserve definition sequence within the same sort order.
 * <p>
 * The logical sort order is determined by both the key type and the compare type:
 * <table>
 *   <caption>Map key sort order</caption>
 *   <tr><th>Order</th><th>Type</th><th>CompareType</th><th>Description</th></tr>
 *   <tr><td>0</td><td>NULL</td><td>EQ</td><td>=null</td></tr>
 *   <tr><td>1</td><td>EMPTY</td><td>EQ</td><td>=empty</td></tr>
 *   <tr><td>2</td><td>BOOL</td><td>(any)</td><td>bool</td></tr>
 *   <tr><td>3</td><td>NUMBER</td><td>(any)</td><td>number</td></tr>
 *   <tr><td>4</td><td>STRING</td><td>(any)</td><td>string</td></tr>
 *   <tr><td>5</td><td>EMPTY</td><td>NE</td><td>!empty</td></tr>
 *   <tr><td>6</td><td>NULL</td><td>NE</td><td>!null</td></tr>
 *   <tr><td>7</td><td>(other)</td><td>(other)</td><td>default</td></tr>
 * </table>
 *
 * @param order   the insertion order of the map key, used as tie-breaker during sorting
 * @param mapKey  the map key, not {@code null}
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
record OrderedMapKey(int order, @NotNull MapKey mapKey)
{
  /**
   * Comparator that sorts map keys by their logical type priority first, and by insertion order second.
   */
  static final Comparator<OrderedMapKey> SORTER = new Comparator<>() {
    @Override
    public int compare(OrderedMapKey k1, OrderedMapKey k2)
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
