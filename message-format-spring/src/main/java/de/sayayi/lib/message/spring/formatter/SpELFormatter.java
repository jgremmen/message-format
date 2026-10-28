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
package de.sayayi.lib.message.spring.formatter;

import de.sayayi.lib.message.formatter.parameter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.NamedParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.ResourceLoader;
import org.springframework.expression.*;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardOperatorOverloader;
import org.springframework.expression.spel.support.StandardTypeComparator;
import org.springframework.expression.spel.support.StandardTypeConverter;

import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static org.springframework.expression.spel.SpelMessage.TYPE_NOT_FOUND;
import static org.springframework.expression.spel.SpelMessage.VARIABLE_ASSIGNMENT_NOT_SUPPORTED;
import static org.springframework.expression.spel.support.DataBindingMethodResolver.forInstanceMethodInvocation;
import static org.springframework.expression.spel.support.DataBindingPropertyAccessor.forReadOnlyAccess;


/**
 * A named parameter formatter that evaluates Spring Expression Language (SpEL) expressions on parameter values during
 * message formatting.
 * <p>
 * This formatter is registered under the name {@code spel} and supports the following parameter configuration options:
 * <ul>
 *   <li>
 *     {@code spel-expr}: a SpEL expression to evaluate against the parameter value. The parameter value serves as the
 *     root object of the expression.
 *   </li>
 *   <li>
 *     {@code spel-format}: an optional format name used to format the result of the SpEL expression.
 *   </li>
 * </ul>
 * <p>
 * Message parameters referencing other parameter values are made available as SpEL variables
 * (e.g. {@code #otherParam}). Variable assignment is not supported and will result in a
 * {@link org.springframework.expression.spel.SpelEvaluationException SpelEvaluationException}.
 * <p>
 * The evaluation context is restricted to reading data from the root object, the variables and the objects reachable
 * from them:
 * <ul>
 *   <li>properties can be read, but not written</li>
 *   <li>instance methods can be invoked, but static methods and methods on {@link Class} objects cannot</li>
 *   <li>
 *     type references ({@code T(...)}) are rejected, so static methods, static fields and classes cannot be accessed by
 *     name
 *   </li>
 *   <li>constructors ({@code new ...}) and bean references ({@code @bean}) are not available</li>
 * </ul>
 * Expressions that violate these restrictions fail with a
 * {@link org.springframework.expression.spel.SpelEvaluationException SpelEvaluationException}, which is reported as a
 * {@link de.sayayi.lib.message.exception.MessageFormatException MessageFormatException}.
 * <p>
 * Instance methods of the reachable objects are still invoked, including methods with side effects. SpEL expressions
 * must therefore never be taken from untrusted input; message format strings that contain a {@code spel-expr}
 * configuration must be constant or come from a trusted source.
 *
 * @author Jeroen Gremmen
 * @since 0.8.3  (refactored in 0.12.0)
 */
@SuppressWarnings("SpellCheckingInspection")
public final class SpELFormatter extends AbstractParameterFormatter<Object> implements NamedParameterFormatter
{
  /** Configuration keys supported by this formatter. */
  private static final Set<String> PARAMETER_CONFIG_NAMES = Set.of("spel-expr", "spel-format");

  /** Operator overloader supporting the standard operators only. */
  private static final OperatorOverloader OPERATOR_OVERLOADER = new StandardOperatorOverloader();

  /** Standard type comparator. */
  private static final TypeComparator TYPE_COMPARATOR = new StandardTypeComparator();

  /**
   * Method resolvers allowing instance methods to be invoked, but no static methods or methods on {@link Class}
   * objects.
   */
  private static final List<MethodResolver> METHOD_RESOLVERS = List.of(forInstanceMethodInvocation());

  /** Property accessors allowing properties to be read, but not written. */
  private static final List<PropertyAccessor> PROPERTY_ACCESSORS = List.of(forReadOnlyAccess());

  /** Type locator rejecting all type references, so {@code T(...)} cannot be used to access arbitrary classes. */
  private static final TypeLocator TYPE_LOCATOR = typeName -> {
    throw new SpelEvaluationException(TYPE_NOT_FOUND, typeName);
  };

  /** Parser for SpEL expressions. */
  private final SpelExpressionParser spelExpressionParser;

  /** Type converter used while evaluating expressions. */
  private final TypeConverter typeConverter;


  /**
   * Creates a new SpEL formatter with the default configuration.
   */
  public SpELFormatter()
  {
    spelExpressionParser = new SpelExpressionParser();
    typeConverter = new StandardTypeConverter();
  }


  /**
   * Creates a new SpEL formatter with the given conversion service and resource loader.
   *
   * @param conversionService  type conversion service, not {@code null}
   * @param resourceLoader     resource loader (usually the application context), not {@code null}
   */
  public SpELFormatter(@NotNull ConversionService conversionService, @NotNull ResourceLoader resourceLoader) {
    this(conversionService, requireNonNull(resourceLoader.getClassLoader()));
  }


  /**
   * Creates a new SpEL formatter with the given conversion service and class loader.
   *
   * @param conversionService  type conversion service, not {@code null}
   * @param classLoader        class loader used by the SpEL parser configuration, not {@code null}
   */
  public SpELFormatter(@NotNull ConversionService conversionService, @NotNull ClassLoader classLoader)
  {
    spelExpressionParser = new SpelExpressionParser(new SpelParserConfiguration(null, classLoader));
    typeConverter = new StandardTypeConverter(conversionService);
  }


  /** {@inheritDoc} */
  @Override
  @Contract(pure = true)
  public @NotNull String getName() {
    return "spel";
  }


  /**
   * {@inheritDoc}
   * <p>
   * If a SpEL expression ({@code spel-expr}) is configured, it is evaluated with {@code value} as its root object.
   * The result (or the original value if no expression is configured) is then formatted using the formatter named by
   * {@code spel-format}, or the formatters for its type if no format is configured. The {@code spel-expr} and
   * {@code spel-format} configuration entries are not passed on to that formatter.
   */
  @Override
  protected @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull Object value)
  {
    var spelExpr = context.getConfigValueString("spel-expr");
    if (spelExpr.isPresent())
    {
      value = spelExpressionParser
          .parseExpression(spelExpr.get())
          .getValue(new ParameterEvaluationContext(context, value));
    }

    return context.format(value, null, context.getConfigValueString("spel-format").orElse(null),
        context.getConfig().excludeConfigByName(PARAMETER_CONFIG_NAMES));
  }


  /** {@inheritDoc} */
  @Override
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames() {
    return PARAMETER_CONFIG_NAMES;
  }


  /** {@inheritDoc} */
  @Override
  public boolean autoApplyOnNamedConfigParameter() {
    return true;
  }




  /**
   * A read-only SpEL {@link EvaluationContext} that uses the parameter value as its root object and exposes other
   * message parameters as SpEL variables. Type references, static method invocations, constructors, bean references,
   * property writes and variable assignments are not supported.
   *
   * @since 0.8.3
   */
  private final class ParameterEvaluationContext implements EvaluationContext
  {
    /** Formatter context providing the message parameters as variables. */
    private final ParameterFormatterContext context;

    /** Parameter value, used as root object. */
    private final TypedValue value;


    /**
     * Creates a new evaluation context for the given parameter value.
     *
     * @param context  formatter context providing the message parameters as variables, not {@code null}
     * @param value    parameter value, used as root object, not {@code null}
     */
    private ParameterEvaluationContext(@NotNull ParameterFormatterContext context, @NotNull Object value)
    {
      this.context = context;
      this.value = new TypedValue(value);
    }


    /**
     * {@inheritDoc}
     * <p>
     * Bean references are not supported, so this method always returns {@code null}.
     */
    @Override
    public BeanResolver getBeanResolver() {
      return null;
    }


    /**
     * {@inheritDoc}
     * <p>
     * Constructors are not supported, so this method always returns an empty list.
     */
    @Override
    public @NotNull List<ConstructorResolver> getConstructorResolvers() {
      return List.of();
    }


    /**
     * {@inheritDoc}
     * <p>
     * Only instance methods can be invoked.
     */
    @Override
    public @NotNull List<MethodResolver> getMethodResolvers() {
      return METHOD_RESOLVERS;
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull OperatorOverloader getOperatorOverloader() {
      return OPERATOR_OVERLOADER;
    }


    /**
     * {@inheritDoc}
     * <p>
     * Properties can be read, but not written.
     */
    @Override
    public @NotNull List<PropertyAccessor> getPropertyAccessors() {
      return PROPERTY_ACCESSORS;
    }


    /**
     * {@inheritDoc}
     * <p>
     * The root object is the parameter value.
     */
    @Override
    public @NotNull TypedValue getRootObject() {
      return value;
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull TypeComparator getTypeComparator() {
      return TYPE_COMPARATOR;
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull TypeConverter getTypeConverter() {
      return typeConverter;
    }


    /**
     * {@inheritDoc}
     * <p>
     * The returned type locator rejects all type references.
     */
    @Override
    public @NotNull TypeLocator getTypeLocator() {
      return TYPE_LOCATOR;
    }


    /**
     * {@inheritDoc}
     * <p>
     * Variables resolve to the message parameters with the same name.
     */
    @Override
    public Object lookupVariable(@NotNull String name) {
      return context.getParameterValue(name);
    }


    /**
     * {@inheritDoc}
     * <p>
     * Variable assignment is not supported.
     *
     * @throws SpelEvaluationException  always
     */
    @Override
    public void setVariable(@NotNull String name, Object value) {
      throw new SpelEvaluationException(VARIABLE_ASSIGNMENT_NOT_SUPPORTED, "#" + name);
    }
  }
}
