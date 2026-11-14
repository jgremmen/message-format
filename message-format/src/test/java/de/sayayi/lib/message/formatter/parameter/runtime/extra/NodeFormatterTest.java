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
package de.sayayi.lib.message.formatter.parameter.runtime.extra;

import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import lombok.val;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


/**
 * Tests {@link NodeFormatter}: formattable types and the XPath of elements and attributes.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@DisplayName("Node formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class NodeFormatterTest extends AbstractFormatterTest
{
  private final MessageSupport messageSupport = createDefaultMessageSupport(new NodeFormatter());


  @Test
  @DisplayName("Formattable types")
  void testFormattableTypes()
  {
    assertFormatterForType(new NodeFormatter(), Element.class);
    assertFormatterForType(new NodeFormatter(), Attr.class);
  }


  static Stream<Arguments> formattedNodes() throws Exception
  {
    val document = DocumentBuilderFactory
        .newInstance()
        .newDocumentBuilder()
        .parse(new InputSource(new StringReader(
            "<catalog><book id='1'/><book id='2'><title/></book></catalog>")));
    val book = (Element)document
        .getElementsByTagName("book")
        .item(1);

    return Stream.of(
        arguments("%{node}", document.getDocumentElement(), "//catalog"),
        arguments("Invalid element: %{node}", book, "Invalid element: //catalog/book[2]"),
        arguments("Invalid attribute: %{node}", book.getAttributeNode("id"),
            "Invalid attribute: //catalog/book[2]/@id"),
        arguments("%{node}", book.getFirstChild(), "//catalog/book[2]/title"),
        arguments("%{node}", document.getElementsByTagName("book").item(0), "//catalog/book[1]"),
        arguments("%{node,null:'document root'}", null, "document root")
    );
  }


  @ParameterizedTest(name = "{0} -> {2}")
  @DisplayName("Format XPath of elements and attributes")
  @MethodSource("formattedNodes")
  void testFormat(@Language("MessageFormat") String formatString, org.w3c.dom.Node node, String expected)
  {
    assertEquals(
        expected,
        messageSupport
            .message(formatString)
            .with("node", node)
            .format());
  }


  @Test
  @DisplayName("XPath of a deeply nested element")
  void testDeepPath() throws Exception
  {
    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    val path = new StringBuilder("/");
    org.w3c.dom.Node node = document;

    // the XPath exceeds the initial capacity of the XPath builder
    for(var n = 0; n < 40; n++)
    {
      node = node.appendChild(document.createElement("element" + n));
      path.append("/element").append(n);
    }

    assertEquals(
        path.toString(),
        messageSupport
            .message("%{node}")
            .with("node", node)
            .format());
  }
}
