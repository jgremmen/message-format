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
package de.sayayi.lib.message.part.template;

import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static java.util.Collections.singletonList;
import static java.util.Locale.GERMANY;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 * @since 0.8.3
 */
public class TemplateTest
{
  private MessageSupport messageSupport;


  @BeforeEach
  void init()
  {
    val messageFactory = NO_CACHE_INSTANCE;
    val configurableMessageSupport = MessageSupportFactory
        .create(DefaultFormatterService.getSharedInstance(), messageFactory)
        .setLocale(GERMANY);

    // Template 'buch', requiring parameter 'collection' and 'fall'
    configurableMessageSupport.addTemplate("buch",
        messageFactory.parseTemplate("%{collection,size," +
            "<>1:'%{fall,choice,'nom':'die Bücher','gen':'der Bücher','dat':'den Büchern','akk':'die Bücher'}'," +
            "1:'%{fall,choice,'nom':'das Buch','gen':'des Buches','dat':'dem Buch','akk':'das Buch'}'}"));

    messageSupport = configurableMessageSupport;
  }


  @Test
  void testSingularPlural()
  {
    // akkusativ singular
    assertEquals("Ich gebe dem Mann das Buch", messageSupport
        .message("Ich gebe dem Mann %[buch,collection=col,fall:akk]")
        .with("col", singletonList("Buch 1"))
        .format());

    // akkusativ plural
    assertEquals("Ich gebe dem Mann die Bücher", messageSupport
        .message("Ich gebe dem Mann %['buch',fall:akk]")
        .with("collection", Arrays.asList("Buch 1", "Buch 2"))
        .format());
  }
}
