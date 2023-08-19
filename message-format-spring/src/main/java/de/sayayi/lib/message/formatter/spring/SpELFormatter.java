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

import de.sayayi.lib.message.formatter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.NamedParameterFormatter;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.ResourceLoader;
import org.springframework.expression.*;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.*;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.springframework.expression.spel.SpelMessage.VARIABLE_ASSIGNMENT_NOT_SUPPORTED;


/**
 * @author Jeroen Gremmen
 * @since 0.8.3
 */
public final class SpELFormatter extends AbstractParameterFormatter<Object>
    implements NamedParameterFormatter
{
  private static final OperatorOverloader OPERATOR_OVERLOADER = new StandardOperatorOverloader();
  private static final TypeComparator TYPE_COMPARATOR = new StandardTypeComparator();
  private static final TypeLocator TYPE_LOCATOR = new StandardTypeLocator();
  private static final List<MethodResolver> METHOD_RESOLVERS =
      singletonList(new ReflectiveMethodResolver());
  private static final List<PropertyAccessor> PROPERTY_ACCESSORS =
      singletonList(DataBindingPropertyAccessor.forReadOnlyAccess());

  private final SpelExpressionParser spelExpressionParser;
  private final TypeConverter typeConverter;


  public SpELFormatter()
  {
    typeConverter = new StandardTypeConverter();
    spelExpressionParser = new SpelExpressionParser();
  }


  public SpELFormatter(@NotNull ConversionService conversionService,
                       @NotNull ResourceLoader resourceLoader) {
    this(conversionService, requireNonNull(resourceLoader.getClassLoader()));
  }


  public SpELFormatter(@NotNull ConversionService conversionService,
                       @NotNull ClassLoader classLoader)
  {
    typeConverter = new StandardTypeConverter(conversionService);
    spelExpressionParser = new SpelExpressionParser(
        new SpelParserConfiguration(null, classLoader));
  }


  @Override
  @Contract(pure = true)
  public @NotNull String getName() {
    return "spel";
  }


  @Override
  protected @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull Object value)
  {
    final Optional<String> spelExpr = context.getConfigValueString("spel-expr");

    if (spelExpr.isPresent())
    {
      value = spelExpressionParser
          .parseExpression(spelExpr.get())
          .getValue(new ParameterEvaluationContext(context, value));
    }

    return context.format(value, null,
        context.getConfigValueString("spel-format").orElse(null));
  }




  private final class ParameterEvaluationContext implements EvaluationContext
  {
    private final FormatterContext context;
    private final TypedValue value;




    private ParameterEvaluationContext(@NotNull FormatterContext context, @NotNull Object value)
    {
      this.context = context;
      this.value = new TypedValue(value);
    }


    @Override
    public @NotNull TypedValue assignVariable(@NotNull String name,
                                              @NotNull Supplier<TypedValue> valueSupplier) {
      throw new SpelEvaluationException(VARIABLE_ASSIGNMENT_NOT_SUPPORTED, "#" + name);
    }


    @Override
    public BeanResolver getBeanResolver() {
      return null;
    }


    @Override
    public @NotNull List<ConstructorResolver> getConstructorResolvers() {
      return emptyList();
    }


    @Override
    public @NotNull List<MethodResolver> getMethodResolvers() {
      return METHOD_RESOLVERS;
    }


    @Override
    public @NotNull OperatorOverloader getOperatorOverloader() {
      return OPERATOR_OVERLOADER;
    }


    @Override
    public @NotNull List<PropertyAccessor> getPropertyAccessors() {
      return PROPERTY_ACCESSORS;
    }


    @Override
    public @NotNull TypedValue getRootObject() {
      return value;
    }


    @Override
    public @NotNull TypeComparator getTypeComparator() {
      return TYPE_COMPARATOR;
    }


    @Override
    public @NotNull TypeConverter getTypeConverter() {
      return typeConverter;
    }


    @Override
    public @NotNull TypeLocator getTypeLocator() {
      return TYPE_LOCATOR;
    }


    @Override
    public Object lookupVariable(@NotNull String name) {
      return context.getParameterValue(name);
    }


    @Override
    public void setVariable(@NotNull String name, Object value) {
    }
  }
}
