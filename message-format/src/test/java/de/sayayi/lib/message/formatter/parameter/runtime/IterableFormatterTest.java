/*
 * Copyright 2021 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter.parameter.runtime;

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.parameter.named.SizeFormatter;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * Tests {@link IterableFormatter}: formattable types, escaped list values and non-collection iterables. The list
 * configuration shared with {@link ArrayFormatter} is tested by {@link ListFormatterConfigTest}.
 *
 * @author Jeroen Gremmen
 * @since 0.6.0
 */
@DisplayName("Iterable formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class IterableFormatterTest extends AbstractFormatterTest
{
  @Test
  void testFormattableTypes() {
    assertFormatterForType(new IterableFormatter(), Iterable.class);
  }


  @Test
  @DisplayName("Escaped parameter in string list value stays text")
  void testListValueEscapedParameter() throws IOException
  {
    val formatterService = createFormatterService(new IterableFormatter());
    val messageSupport = MessageSupportFactory.create(formatterService);

    assertEquals("<1>, <2>", messageSupport
        .message("%{c,list-value:'<%{value}>'}").with("c", List.of(1, 2)).format());
    assertEquals("<%{value}>, <%{value}>", messageSupport
        .message("%{c,list-value:'<\\%{value}>'}").with("c", List.of(1, 2)).format());

    // the string value survives a pack round trip
    messageSupport.addMessage("MSG", "%{c,list-value:'<\\%{value}>'}");

    val pack = new ByteArrayOutputStream();
    messageSupport.exportMessages(pack);

    val importedMessageSupport = MessageSupportFactory.create(formatterService);
    importedMessageSupport.importMessages(new ByteArrayInputStream(pack.toByteArray()));

    assertEquals("<%{value}>, <%{value}>", importedMessageSupport.code("MSG").with("c", List.of(1, 2)).format());
  }


  @Test
  @DisplayName("Non-collection iterables are iterated for formatting only")
  void testOneShotIterable()
  {
    val messageSupport = MessageSupportFactory
        .create(createFormatterService(new IterableFormatter(), new SizeFormatter()));

    // the empty keys are matched against the formatted text
    assertEquals("none", messageSupport
        .message("%{c,empty:'none',!empty:'some'}").with("c", new OneShotIterable()).format());
    assertEquals("some", messageSupport
        .message("%{c,empty:'none',!empty:'some'}").with("c", new OneShotIterable("a", "b")).format());
    assertEquals("a, b", messageSupport
        .message("%{c}").with("c", new OneShotIterable("a", "b")).format());
    assertEquals("unknown", messageSupport
        .message("%{c,format:size,empty:'unknown'}").with("c", new OneShotIterable("a", "b")).format());

    // collections still report their size and emptiness
    assertEquals("none", messageSupport
        .message("%{c,empty:'none',!empty:'some'}").with("c", List.of()).format());
    assertEquals("some", messageSupport
        .message("%{c,empty:'none',!empty:'some'}").with("c", List.of("a")).format());
    assertEquals("2", messageSupport
        .message("%{c,format:size}").with("c", List.of("a", "b")).format());

    val iterable = new OneShotIterable("a");
    iterable.iterator();
    assertThrows(IllegalStateException.class, iterable::iterator);
  }




  private static final class OneShotIterable implements Iterable<String>
  {
    private final List<String> elements;
    private boolean iterated;


    private OneShotIterable(String... elements) {
      this.elements = List.of(elements);
    }


    @Override
    public Iterator<String> iterator()
    {
      if (iterated)
        throw new IllegalStateException("already iterated");

      iterated = true;

      return elements.iterator();
    }
  }
}
