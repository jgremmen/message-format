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
package de.sayayi.lib.message.formatter.runtime.extra;

import de.sayayi.lib.message.formatter.AbstractSingleTypeParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.TextPartFactory;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import static java.lang.Math.max;
import static java.lang.System.arraycopy;
import static org.w3c.dom.Node.ATTRIBUTE_NODE;
import static org.w3c.dom.Node.ELEMENT_NODE;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class NodeFormatter extends AbstractSingleTypeParameterFormatter<Node>
{
  @Override
  protected @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull Node value)
  {
    return "xpath".equals(context.getConfigValueString("node").orElse("xpath"))
        ? TextPartFactory.noSpaceText(formatValue_xpath(value))
        : context.delegateToNextFormatter();
  }


  private @NotNull String formatValue_xpath(@NotNull Node node)
  {
    final XPathBuilder path = new XPathBuilder();
    boolean firstStep = true;

    while(node != null)
    {
      if (firstStep)
        firstStep = false;
      else
        path.add('/');

      final short type = node.getNodeType();
      final String name = node.getNodeName();

      if (type == ATTRIBUTE_NODE)
      {
        path.add(name);
        path.add('@');

        node = ((Attr)node).getOwnerElement();
      }
      else if (type == ELEMENT_NODE)
      {
        int index = 1;

        for(Node e = node; (e = e.getPreviousSibling()) != null;)
          if (e instanceof Element && name.equals(e.getNodeName()))
            index++;

        boolean indexed = index > 1;

        if (!indexed)
          for(Node e = node; (e = e.getNextSibling()) != null;)
            if (e instanceof Element && (indexed = name.equals(e.getNodeName())))
              break;

        if (indexed)
        {
          path.add(']');
          path.add(Integer.toString(index));
          path.add('[');
        }

        path.add(name);
        node = node.getParentNode();
      }
      else
        break;
    }

    return path.toString();
  }


  @Override
  public @NotNull FormattableType getFormattableType() {
    return new FormattableType(Node.class);
  }


  @SuppressWarnings({"UnusedReturnValue", "UnstableApiUsage"})
  private static final class XPathBuilder
  {
    private char[] xpath = new char[64];
    private int idx = xpath.length;


    @Contract(value = "_ -> this", mutates = "this")
    public @NotNull XPathBuilder add(char c)
    {
      assureCapacity(1);
      xpath[--idx] = c;

      return this;
    }


    @Contract(value = "_ -> this", mutates = "this")
    public @NotNull XPathBuilder add(@NotNull String s)
    {
      final int n = s.length();
      if (n > 0)
      {
        assureCapacity(n);
        arraycopy(s.toCharArray(), 0, xpath, idx -= n, n);
      }

      return this;
    }


    private void assureCapacity(int n)
    {
      if (idx < n)
      {
        final int oldLength = xpath.length;
        final int increment = max(16, n - idx + 8);
        final char[] newXPath = new char[oldLength + increment];

        arraycopy(xpath, 0, newXPath, increment, oldLength);

        xpath = newXPath;
        idx += increment;
      }
    }


    @Override
    public String toString() {
      return new String(xpath, idx, xpath.length - idx);
    }
  }
}
