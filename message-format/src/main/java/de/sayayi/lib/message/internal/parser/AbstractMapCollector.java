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
package de.sayayi.lib.message.internal.parser;

import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static java.util.function.Function.identity;
import static java.util.stream.Collector.Characteristics.IDENTITY_FINISH;
import static java.util.stream.Collector.Characteristics.UNORDERED;


/**
 * Abstract {@link Collector} implementation that collects ANTLR {@link ParserRuleContext} elements into a {@link Map}.
 * <p>
 * Subclasses must implement the {@link #accumulator(Map, ParserRuleContext)} method to define how each parser rule
 * context is converted into a key-value pair and added to the map.
 * <p>
 * The collector characteristics are determined by the type of map provided by the supplier:
 * <ul>
 *   <li>
 *     If the supplier produces a {@link LinkedHashMap}, only {@link Characteristics#IDENTITY_FINISH IDENTITY_FINISH}
 *     is reported (insertion order is preserved).
 *   </li>
 *   <li>
 *     Otherwise, both {@link Characteristics#IDENTITY_FINISH IDENTITY_FINISH} and
 *     {@link Characteristics#UNORDERED UNORDERED} are reported.
 *   </li>
 * </ul>
 *
 * @param <C>  the type of ANTLR parser rule context to collect
 * @param <K>  the type of map keys
 * @param <V>  the type of map values
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
abstract class AbstractMapCollector<C extends ParserRuleContext,K,V> implements Collector<C,Map<K,V>,Map<K,V>>
{
  private final Supplier<Map<K,V>> supplier;
  private final Set<Characteristics> characteristics;


  /**
   * Creates a new map collector with the given map supplier.
   *
   * @param supplier  supplier providing new mutable map instances, not {@code null}
   */
  protected AbstractMapCollector(@NotNull Supplier<Map<K,V>> supplier)
  {
    this.supplier = supplier;
    this.characteristics = supplier.get() instanceof LinkedHashMap
        ? Set.of(IDENTITY_FINISH)
        : Set.of(IDENTITY_FINISH, UNORDERED);
  }


  /**
   * {@inheritDoc}
   *
   * @return  the map supplier provided at construction time
   */
  @Override
  public Supplier<Map<K,V>> supplier() {
    return supplier;
  }


  /**
   * {@inheritDoc}
   * <p>
   * The returned bi-consumer delegates to {@link #accumulator(Map, ParserRuleContext)}.
   *
   * @return  a bi-consumer that adds a parsed context entry to the accumulating map
   */
  @Override
  public BiConsumer<Map<K,V>,C> accumulator() {
    return this::accumulator;
  }


  /**
   * Processes a single parser rule context and adds the resulting key-value pair to the map.
   *
   * @param map      the mutable map being accumulated into, not {@code null}
   * @param context  the parser rule context to process, not {@code null}
   */
  protected abstract void accumulator(@NotNull Map<K,V> map, @NotNull C context);


  /**
   * {@inheritDoc}
   * <p>
   * Returns a combiner that merges two maps by putting all entries from the second map into the
   * first.
   *
   * @return  a binary operator that merges two maps
   */
  @Override
  public BinaryOperator<Map<K,V>> combiner() {
    return (map1,map2) -> { map1.putAll(map2); return map1; };
  }


  /**
   * {@inheritDoc}
   * <p>
   * Returns the {@linkplain Function#identity() identity function}, as no transformation of the accumulated map
   * is needed.
   *
   * @return  the identity function
   */
  @Override
  public Function<Map<K,V>,Map<K,V>> finisher() {
    return identity();
  }


  /**
   * {@inheritDoc}
   *
   * @return an unmodifiable set of collector characteristics
   *
   * @see #AbstractMapCollector(Supplier)
   */
  @Override
  public Set<Characteristics> characteristics() {
    return characteristics;
  }
}
