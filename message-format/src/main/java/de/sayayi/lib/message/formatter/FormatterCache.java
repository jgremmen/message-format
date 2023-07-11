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
package de.sayayi.lib.message.formatter;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

import static java.lang.System.arraycopy;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
final class FormatterCache
{
  private final int capacity;
  private final Object[] typeFormatters;  // 2 * n = type, 2 * n + 1 = node

  private int typeCount;
  private Node head;
  private Node tail;


  public FormatterCache(int n)
  {
    capacity = Math.max(n, 8);
    typeFormatters = new Object[n * 2];

    clear();
  }


  public synchronized void clear()
  {
    head = null;
    tail = null;
    typeCount = 0;
  }


  public synchronized @NotNull ParameterFormatter[] lookup(
      @NotNull Class<?> type, @NotNull Function<Class<?>,ParameterFormatter[]> buildFormatters)
  {
    final int idx = findTypeIndex(type);

    if (idx >= 0)
    {
      final Node node = (Node)typeFormatters[idx * 2 + 1];

      // move to head?
      if (node != head && typeCount >= capacity * 3 / 4 && node.countNext < typeCount / 4)
        lookup_moveNodeToHead(node);

      return node.formatters;
    }

    final ParameterFormatter[] formatters = buildFormatters.apply(type);

    lookup_addNew(type, formatters);

    return formatters;
  }


  private void lookup_moveNodeToHead(@NotNull Node node)
  {
    final Node prevNode = node.prev;
    assert prevNode != null;

    for(Node n = prevNode; n != null; n = n.prev)
      n.countNext--;

    prevNode.next = node.next;

    if (node == tail)
      tail = prevNode;
    else
      node.next.prev = prevNode;

    node.prev = null;
    node.next = head;
    node.countNext = head.countNext + 1;

    head.prev = node;
    head = node;
  }


  private void lookup_addNew(@NotNull Class<?> type, @NotNull ParameterFormatter[] formatters)
  {
    // if capacity has been reached -> remove tail
    if (typeCount == capacity)
    {
      int typeOffset = findTypeIndex(tail.type) * 2;

      arraycopy(typeFormatters, typeOffset + 2, typeFormatters, typeOffset,
          (capacity - 1) * 2 - typeOffset);
      typeCount--;

      final Node prevNode = tail.prev;
      assert prevNode != null;

      for(Node n = prevNode; n != null; n = n.prev)
        n.countNext--;

      prevNode.next = null;
      tail = prevNode;
    }

    final Node node = new Node(type, formatters);
    final int insertOffset;

    if (head != null)
    {
      insertOffset = (-findTypeIndex(type) - 1) * 2;

      arraycopy(typeFormatters, insertOffset, typeFormatters, insertOffset + 2,
          typeCount * 2 - insertOffset);

      node.next = head;
      node.countNext = head.countNext + 1;
      head.prev = node;
    }
    else
    {
      insertOffset = 0;
      tail = node;
    }

    typeFormatters[insertOffset] = type;
    typeFormatters[insertOffset + 1] = node;
    typeCount++;

    head = node;
  }


  private int findTypeIndex(@NotNull Class<?> type)
  {
    final String typeName = type.getName();
    int low = 0;
    int high = typeCount - 1;

    while(low <= high)
    {
      final int mid = (low + high) >>> 1;
      final Class<?> midClass = (Class<?>)typeFormatters[mid * 2];

      if (midClass == type)
        return mid;

      final int midValCmp = midClass.getName().compareTo(typeName);

      if (midValCmp < 0)
        low = mid + 1;
      else if (midValCmp > 0)
        high = mid - 1;
    }

    return -(low + 1);  // type not found
  }




  private static final class Node
  {
    private final Class<?> type;
    private final ParameterFormatter[] formatters;

    private Node prev;
    private Node next;
    private int countNext;


    public Node(@NotNull Class<?> type, @NotNull ParameterFormatter[] formatters)
    {
      this.type = type;
      this.formatters = formatters;
    }

    @Override
    public String toString()
    {
      return "Node(countNext=" + countNext + ",type=" + type + ",head=" + (prev == null) +
          ",tail=" + (next == null) + ')';
    }
  }
}
