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
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import lombok.val;
import org.junit.jupiter.api.*;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;


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
  private MessageSupport messageSupport;


  @BeforeEach
  void init()
  {
    val formatterService = new DefaultFormatterService();
    formatterService.addFormatter(new NodeFormatter());

    messageSupport = MessageSupportFactory.create(formatterService);
  }


  @Test
  void testFormattableTypes()
  {
    assertFormatterForType(new NodeFormatter(), Element.class);
    assertFormatterForType(new NodeFormatter(), Attr.class);
  }


  @Test
  @DisplayName("Format XPath of elements and attributes")
  void testFormat() throws Exception
  {
    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(
        "<catalog><book id='1'/><book id='2'><title/></book></catalog>")));
    val book = (Element)document.getElementsByTagName("book").item(1);

    assertEquals("//catalog", messageSupport.message("%{node}").with("node", document.getDocumentElement()).format());
    assertEquals("Invalid element: //catalog/book[2]",
        messageSupport.message("Invalid element: %{node}").with("node", book).format());
    assertEquals("Invalid attribute: //catalog/book[2]/@id",
        messageSupport.message("Invalid attribute: %{node}").with("node", book.getAttributeNode("id")).format());
    assertEquals("//catalog/book[2]/title",
        messageSupport.message("%{node}").with("node", book.getFirstChild()).format());
    assertEquals("//catalog/book[1]",
        messageSupport.message("%{node}").with("node", document.getElementsByTagName("book").item(0)).format());
    assertEquals("document root",
        messageSupport.message("%{node,null:'document root'}").with("node", (Element)null).format());
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

    assertEquals(path.toString(), messageSupport.message("%{node}").with("node", node).format());
  }
}
