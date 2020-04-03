/*
 * Copyright 2019 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sayayi.lib.message.formatter;

import de.sayayi.lib.message.formatter.support.*;


/**
 * Formatter service providing formatters for the most common java types.
 *
 * @author Jeroen Gremmen
 */
public class DefaultFormatterService extends GenericFormatterService
{
  private static final FormatterService INSTANCE = new DefaultFormatterService();


  public static FormatterService getSharedInstance() {
    return INSTANCE;
  }


  @SuppressWarnings("WeakerAccess")
  public DefaultFormatterService() {
    addDefaultFormatters();
  }


  @SuppressWarnings("WeakerAccess")
  protected void addDefaultFormatters()
  {
    // named formatters
    addFormatter(new ChoiceFormatter());
    addFormatter(new BoolFormatter());
    addFormatter(new BitsFormatter());
    addFormatter(new GeoFormatter());

    // typed formatters
    addFormatter(new StringFormatter());
    addFormatter(new NumberFormatter());
    addFormatter(new DateFormatter());
    addFormatter(new ArrayFormatter());
    addFormatter(new CollectionFormatter());
    addFormatter(new MapFormatter());
    addFormatter(new FileFormatter());
    addFormatter(new PackageFormatter());
    addFormatter(new ClassFormatter());
    addFormatter(new URIFormatter());
    addFormatter(new URLFormatter());
    addFormatter(new ReferenceFormatter());
    addFormatter(new ThreadLocalFormatter());
    addFormatter(new InetAddressFormatter());
    addFormatter(new AtomicBooleanFormatter());

    if (hasClass("org.joda.time.DateTime"))
      addFormatter(new de.sayayi.lib.message.formatter.support.JodaDateTimeFormatter());

    if (isJava8())
    {
      addFormatter(new de.sayayi.lib.message.formatter.support.Java8DateTimeFormatter());
      addFormatter(new de.sayayi.lib.message.formatter.support.OptionalFormatter());
      addFormatter(new de.sayayi.lib.message.formatter.support.SupplierFormatter());
      addFormatter(new de.sayayi.lib.message.formatter.support.DoubleSupplierFormatter());
      addFormatter(new de.sayayi.lib.message.formatter.support.LongSupplierFormatter());
      addFormatter(new de.sayayi.lib.message.formatter.support.IntSupplierFormatter());
      addFormatter(new de.sayayi.lib.message.formatter.support.BooleanSupplierFormatter());
      addFormatter(new de.sayayi.lib.message.formatter.support.PathFormatter());
    }
  }


  @SuppressWarnings("WeakerAccess")
  protected boolean isJava8() {
    return hasClass("java.time.LocalDate") && hasClass("java.util.function.DoubleSupplier");
  }


  @SuppressWarnings("WeakerAccess")
  protected boolean hasClass(String className)
  {
    try {
      Class.forName(className, false, DefaultFormatterService.class.getClassLoader());
      return true;
    } catch(final Exception ex) {
      return false;
    }
  }
}
