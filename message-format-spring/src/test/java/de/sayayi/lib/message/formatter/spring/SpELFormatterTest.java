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
package de.sayayi.lib.message.formatter.spring;

import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import lombok.val;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.support.DefaultConversionService;

import java.util.TreeMap;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 * @since 0.8.3
 */
class SpELFormatterTest extends AbstractFormatterTest
{
  private static MessageSupport messageSupport;


  @BeforeAll
  static void init()
  {
    final DefaultFormatterService formatterService = new DefaultFormatterService();

    formatterService.addFormatter(new SpELFormatter(
        new DefaultConversionService(), SpELFormatterTest.class.getClassLoader()));

    messageSupport = MessageSupportFactory.create(formatterService, NO_CACHE_INSTANCE);
  }


  @Test
  void testFormat()
  {
    val map = new TreeMap<String,Integer>();
    map.put("A", 0);
    map.put("C", 34);
    map.put("D", -8);

    assertEquals("34", messageSupport
        .message("%{map,spel,spel-expr:'entrySet().toArray()[1].value'}")
        .with("map", map)
        .format());

    assertEquals("no", messageSupport
        .message("%{map,spel,spel-expr:'entrySet().toArray()[0].value',spel-format:bool,false:no}")
        .with("map", map)
        .format());

    assertEquals("negative", messageSupport
        .message("%{map,spel,spel-expr:'entrySet().toArray()[2].value',spel-format:choice,<0:negative}")
        .with("map", map)
        .format());
  }
}
