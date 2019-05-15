package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.ParameterFactory;
import de.sayayi.lib.message.data.ParameterData;
import de.sayayi.lib.message.data.ParameterMap;
import de.sayayi.lib.message.data.ParameterString;
import de.sayayi.lib.message.formatter.GenericFormatterRegistry;
import de.sayayi.lib.message.formatter.NamedParameterFormatter;
import org.junit.Test;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static de.sayayi.lib.message.MessageFactory.parse;
import static java.util.Locale.ENGLISH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


/**
 * @author Jeroen Gremmen
 */
public class ArrayFormatterTest extends AbstractFormatterTest
{
  @Test
  public void testFormattableTypes()
  {
    assertFormatterForType(new ArrayFormatter(), boolean[].class);
    assertFormatterForType(new ArrayFormatter(), short[].class);
    assertFormatterForType(new ArrayFormatter(), int[].class);
    assertFormatterForType(new ArrayFormatter(), long[].class);
    assertFormatterForType(new ArrayFormatter(), float[].class);
    assertFormatterForType(new ArrayFormatter(), double[].class);
    assertFormatterForType(new ArrayFormatter(), Object[].class);
  }


  @Test
  public void testBooleanArray()
  {
    GenericFormatterRegistry registry = new GenericFormatterRegistry();
    registry.addFormatter(new ArrayFormatter());
    registry.addFormatter(new BoolFormatter());

    ParameterFactory factory = ParameterFactory.createFor("de-DE", registry);

    assertEquals("wahr, falsch, wahr", registry.getFormatter(null, boolean[].class)
        .format(new boolean[] { true, false, true }, "bool", factory, null));

    ParameterMap booleanMap = new ParameterMap(new HashMap<Serializable,Message>() {
      {
        put(Boolean.TRUE, new Message() {
          @Override public String format(Parameters parameters) { return "YES"; }
          @Override public boolean hasParameters() { return false; }
        });

        put(Boolean.FALSE, new Message() {
          @Override public String format(Parameters parameters) { return "NO"; }
          @Override public boolean hasParameters() { return false; }
        });
      }
    });

    assertEquals("NO, YES", registry.getFormatter(null, boolean[].class)
        .format(new boolean[] { false, true }, null, factory, booleanMap));

    assertNull(registry.getFormatter(null, boolean[].class)
        .format(new boolean[0], null, factory, null));

    registry.addFormatter(new NamedParameterFormatter() {
      @Override
      public String format(Object value, String format, Parameters parameters, ParameterData data) {
        return (value == null) ? null : ((Boolean)value).booleanValue() ? "1" : "0";
      }

      @Override
      public String getName() {
        return "bool";
      }

      @Override
      public Set<Class<?>> getFormattableTypes() {
        return new HashSet<>(Arrays.asList(Boolean.class, boolean.class));
      }
    });

    assertEquals("1, 1, 0, 1, 0, 0, 0", registry.getFormatter(null, boolean[].class)
        .format(new boolean[] { true, true, false, true, false, false, false }, "bool", factory, null));
  }


  @Test
  public void testIntegerArray()
  {
    GenericFormatterRegistry registry = new GenericFormatterRegistry();
    registry.addFormatter(new ArrayFormatter());

    ParameterFactory factory = ParameterFactory.createFor("de-DE", registry);

    assertEquals("12, -7, 99", registry.getFormatter(null, int[].class)
        .format(new int[] { 12, -7, 99 }, null , factory, null));

    assertEquals("1, -7, 248", registry.getFormatter(null, int[].class)
        .format(new int[] { 1, -7, 248 }, null , factory, new ParameterString("##00")));

    registry.addFormatter(new NumberFormatter());

    assertEquals("01, -07, 248", registry.getFormatter(null, int[].class)
        .format(new int[] { 1, -7, 248 }, null , factory, new ParameterString("##00")));

    registry.addFormatter(new NamedParameterFormatter() {
      @Override
      public String getName() {
        return "hex";
      }

      @Override
      public String format(Object value, String format, Parameters parameters, ParameterData data) {
        return (value == null) ? null : String.format("0x%02x", value);
      }

      @Override
      public Set<Class<?>> getFormattableTypes() {
        return Collections.singleton(Integer.class);
      }
    });

    assertEquals("0x40, 0xda, 0x2e", registry.getFormatter(null, int[].class)
        .format(new int[] { 64, 218, 46 }, "hex" , factory, null));
  }


  @Test
  public void testObjectArray()
  {
    GenericFormatterRegistry registry = new GenericFormatterRegistry();
    registry.addFormatter(new ArrayFormatter());
    registry.addFormatter(new BoolFormatter());
    registry.addFormatter(new NumberFormatter());

    ParameterFactory factory = ParameterFactory.createFor("de-DE", registry);

    assertEquals("Test, wahr, -0006", registry.getFormatter(null, int[].class)
        .format(new Object[] { "Test", true, null, -6 }, null , factory, new ParameterString("0000")));
  }


  public void testFormatter()
  {
    final GenericFormatterRegistry formatterRegistry = new GenericFormatterRegistry();
    formatterRegistry.addFormatter(new BoolFormatter());
    ParameterFactory factory = ParameterFactory.createFor(ENGLISH, formatterRegistry);

    final Parameters parameters = factory.parameters()
        .with("a", Boolean.FALSE)
        .with("b", Boolean.TRUE)
        .with("c", Integer.valueOf(1234))
        .with("d", Integer.valueOf(0))
        .with("e", Double.valueOf(3.14d));
    final Message msg = parse("%{a} %{b} %{c} %{c,bool} %{d,bool,{true->'yes',false->'no'}} %{e}");

    assertEquals("false true 1234 true no 3.14", msg.format(parameters));
  }
}
