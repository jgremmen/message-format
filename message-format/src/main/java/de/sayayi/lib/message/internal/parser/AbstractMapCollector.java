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
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
abstract class AbstractMapCollector<C extends ParserRuleContext,K,V> implements Collector<C,Map<K,V>,Map<K,V>>
{
  private final Supplier<Map<K,V>> supplier;
  private final Set<Characteristics> characteristics;


  protected AbstractMapCollector(@NotNull Supplier<Map<K,V>> supplier)
  {
    this.supplier = supplier;
    this.characteristics = supplier.get() instanceof LinkedHashMap
        ? Set.of(IDENTITY_FINISH)
        : Set.of(IDENTITY_FINISH, UNORDERED);
  }


  @Override
  public Supplier<Map<K,V>> supplier() {
    return supplier;
  }


  @Override
  public BiConsumer<Map<K,V>,C> accumulator() {
    return this::accumulator;
  }


  protected abstract void accumulator(@NotNull Map<K,V> map, @NotNull C context);


  @Override
  public BinaryOperator<Map<K,V>> combiner()
  {
    return (map1,map2) -> {
      map1.putAll(map2);
      return map1;
    };
  }


  @Override
  public Function<Map<K,V>,Map<K,V>> finisher() {
    return identity();
  }


  @Override
  public Set<Characteristics> characteristics() {
    return characteristics;
  }
}
