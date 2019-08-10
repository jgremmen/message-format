package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.ParameterFactory;
import de.sayayi.lib.message.data.ParameterData;
import de.sayayi.lib.message.data.ParameterMap;
import de.sayayi.lib.message.data.ParameterString;
import de.sayayi.lib.message.formatter.GenericFormatterRegistry;
import de.sayayi.lib.message.formatter.NamedParameterFormatter;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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
  @SuppressWarnings("serial")
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
          @Override public String format(@NotNull Parameters parameters) { return "YES"; }
          @Override public boolean hasParameters() { return false; }
        });

        put(Boolean.FALSE, new Message() {
          @Override public String format(@NotNull Parameters parameters) { return "NO"; }
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
      public String format(Object value, String format, @NotNull Parameters parameters, ParameterData data) {
        return (value == null) ? null : (Boolean)value ? "1" : "0";
      }

      @NotNull
      @Override
      public String getName() {
        return "bool";
      }

      @NotNull
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
      @NotNull
      @Override
      public String getName() {
        return "hex";
      }

      @SuppressWarnings("RedundantCast")
      @Override
      public String format(Object value, String format, @NotNull Parameters parameters, ParameterData data) {
        return (value == null) ? null : String.format("0x%02x", (Integer)value);
      }

      @NotNull
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

    assertEquals("this, is, a, test", registry.getFormatter(null, int[].class)
        .format(new Object[] { null, "this", null, "is", null, "a", null, "test" }, null , factory, null));
  }
}
