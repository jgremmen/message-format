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
package de.sayayi.lib.message.exception;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

import static de.sayayi.lib.message.MessageFactory.isGeneratedCode;
import static java.util.Comparator.comparingInt;
import static java.util.Comparator.naturalOrder;
import static java.util.Locale.ROOT;
import static java.util.Locale.UK;
import static java.util.stream.Collectors.toUnmodifiableSet;


/**
 * Exception providing detailed information about what went wrong during message or template formatting.
 * <p>
 * Besides the cause, the exception records the context in which the error occurred (message code, template name,
 * locale, parameter name and post formatter name), as far as it is known. The context is typically added while the
 * exception propagates through nested messages and templates, using the {@code with...} methods. The order in which
 * the context is added is retained, so the detail message is able to describe the actual nesting.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public class MessageFormatException extends MessageException
{
  /**
   * Detail message texts, keyed by context key sequence and sorted by key length, longest first.
   * <p>
   * A context key consists of the kind prefix ({@code p} = parameter, {@code pf} = post formatter, {@code t} =
   * template) and a number which is incremented per kind, in the order the context was recorded. The locale has key
   * {@code l} and the code has key {@code c}; if present, they are always the first keys in a sequence, in that order.
   * Placeholders {@code <key>} in a text are replaced with the associated context value.
   * <p>
   * The entry with key {@link #UNRECOGNIZED_KEY} is used if no key sequence matches. Its placeholder {@code <key>} is
   * replaced with the unmatched sequence.
   */
  private static final Map<String,String> MESSAGES =
      new TreeMap<>(comparingInt(String::length).reversed().thenComparing(naturalOrder()));

  /** Message key used if the context key sequence is not recognized. It never matches a key sequence. */
  private static final String UNRECOGNIZED_KEY = "?";


  static
  {
    // failed to format ...
    MESSAGES.put(UNRECOGNIZED_KEY, "message (unknown key '<key>')");
    MESSAGES.put("", "message");  // default
    MESSAGES.put("c", "message with code '<c>'");  // fallback
    MESSAGES.put("l", "message");  // fallback
    MESSAGES.put("p1", "parameter '<p1>'");
    MESSAGES.put("p1-p2", "parameter '<p1>' in mapped message of parameter '<p2>'");
    MESSAGES.put("p1-p2-pf1", "parameter '<p1>' in mapped message of sub-message parameter '<p2>' of post formatter '<pf1>'");
    MESSAGES.put("p1-p2-t1", "parameter '<p1>' in mapped message of parameter '<p2>' in template '<t1>'");
    MESSAGES.put("p1-pf1", "sub-message parameter '<p1>' of post formatter '<pf1>'");
    MESSAGES.put("p1-pf1-p2", "sub-message parameter '<p1>' of post formatter '<pf1>' in mapped message of parameter '<p2>'");
    MESSAGES.put("p1-pf1-t1", "failed to format sub-message parameter '<p1>' of post formatter '<pf1>' in template '<t1>'");
    MESSAGES.put("p1-t1", "failed to format parameter '<p1>' in template '<t1>'");
    MESSAGES.put("p1-t1-p2", "failed to format parameter '<p1>' in template '<t1>' for mapped message of parameter '<p2>'");
    MESSAGES.put("pf1", "failed to format post formatter '<pf1>'");
    MESSAGES.put("pf1-p1", "failed to format post formatter '<pf1>' in mapped message of parameter '<p1>'");
    MESSAGES.put("pf1-pf2", "failed to format sub-message post formatter '<pf1>' of post formatter '<pf2>'");
    MESSAGES.put("pf1-t1", "failed to format post formatter '<pf1>' in template '<t1>'");
    MESSAGES.put("t1", "failed to format template '<t1>'");
    MESSAGES.put("t1-pf1", "failed to format sub-message template '<t1>' of post formatter '<pf1>'");

/*
    MESSAGES.put("p1-pf1-pf2", "message ...");
    MESSAGES.put("p1-t1-pf1", "message ...");
    MESSAGES.put("pf1-p1-p2", "message ...");
    MESSAGES.put("pf1-p1-pf2", "message ...");
    MESSAGES.put("pf1-p1-t1", "message ...");
    MESSAGES.put("pf1-pf2-p1", "message ...");
    MESSAGES.put("pf1-pf2-t1", "message ...");
    MESSAGES.put("pf1-t1-p1", "message ...");
    MESSAGES.put("pf1-t1-pf2", "message ...");
    MESSAGES.put("t1-pf1-pf2", "message ...");
 */
  }


  /** Formatting context in the order it was recorded, innermost first. Contains at most 1 code and 1 locale. */
  private final List<Context> context;

  /** Detail message, or {@code null} if not yet created. It is not serialized and is recreated on demand. */
  private transient volatile String message;


  /**
   * Constructs a new message format exception with the specified cause.
   * <p>
   * Note that the detail message associated with {@code cause} is <i>not</i> incorporated in this runtime exception's
   * detail message.
   *
   * @param cause  the cause (which is saved for later retrieval by the {@link #getCause()} method). (A {@code null}
   *               value is permitted and indicates that the cause is nonexistent or unknown.)
   */
  protected MessageFormatException(Throwable cause) {
    this(List.of(), cause);
  }


  /**
   * Constructs a new message format exception with the specified recorded context and cause.
   *
   * @param context  formatting context, innermost first, not {@code null}
   * @param cause    the cause, or {@code null} if the cause is nonexistent or unknown
   */
  private MessageFormatException(@NotNull @Unmodifiable List<Context> context, Throwable cause)
  {
    super(cause);

    this.context = context;
  }


  /**
   * {@inheritDoc}
   * <p>
   * The returned message describes which message, template, parameter, post formatter and locale failed to format, as
   * far as they are known, honoring the order in which they have been nested. Generated message codes and template
   * names are omitted.
   * <p>
   * The context keys are concatenated into a sequence (e.g. {@code l-c-p1-t1}) and the longest registered key
   * sequence that is a prefix of it selects the message text.
   *
   * @return  detail message, never {@code null}
   */
  @Override
  public String getMessage()
  {
    var msg = message;
    if (msg == null)
      message = msg = createMessage();

    return msg;
  }


  /**
   * Creates the detail message from the recorded formatting context.
   *
   * @return  detail message, never {@code null}
   *
   * @see #getMessage()
   */
  @Contract(value = "-> new", pure = true)
  private @NotNull String createMessage()
  {
    var sequence = getContextKey();
    var message = MESSAGES.get(sequence);
    if (message == null && sequence.startsWith(Kind.LOCALE.prefix + '-'))
      message = MESSAGES.get(sequence = sequence.substring(2));
    if (message == null && sequence.startsWith(Kind.CODE.prefix + '-'))
      message = MESSAGES.get(sequence = sequence.substring(2));
    if (message == null)
    {
      final var sequence_ = sequence;

      message = MESSAGES
          .entrySet()
          .stream()
          .filter(m -> {
            final var key = m.getKey();
            return sequence_.startsWith(key + '-');
          })
          .map(Entry::getValue)
          .findFirst()
          .orElseGet(() -> MESSAGES.get(UNRECOGNIZED_KEY).replace("<key>", sequence_));
    }

    final var msg = new StringBuilder(message);

    if (msg.indexOf("failed to format ") < 0)
      msg.insert(0, "failed to format ");
    if (msg.indexOf("<c>") == -1 && get(Kind.CODE) != null)
      msg.insert(0, "message '<c>' ");
    if (get(Kind.LOCALE) != null)
      msg.append(" using locale <l>");

    for(var ctx: context)
    {
      final var key = '<' + ctx.key + '>';
      final var idx = msg.indexOf(key);

      if (idx >= 0)
      {
        msg.delete(idx, idx + key.length());
        msg.insert(idx, ctx.valueAsString());
      }
    }

    return msg.toString();
  }


  /**
   * Returns the context key sequence for the recorded formatting context, e.g. {@code l-c-p1-t1}.
   *
   * @return  context key sequence, never {@code null}
   */
  @Contract(value = "-> new", pure = true)
  public @NotNull String getContextKey()
  {
    final var keys = new ArrayList<String>();
    var hasLocale = false;
    var hasCode = false;

    for(var ctx: context)
      switch(ctx.kind)
      {
        case LOCALE -> hasLocale = true;
        case CODE -> hasCode = true;
        default -> keys.add(ctx.key);
      }

    if (hasCode)
      keys.addFirst(Kind.CODE.prefix);
    if (hasLocale)
      keys.addFirst(Kind.LOCALE.prefix);

    return String.join("-", keys);
  }


  /**
   * Returns the context key sequences for which a detail message has been registered. This method is intended for
   * testing purposes only.
   *
   * @return  unmodifiable set of registered context key sequences, never {@code null}
   *
   * @since 0.25.0
   */
  @Contract(pure = true)
  static @NotNull @Unmodifiable Set<String> getContextKeys()
  {
    return MESSAGES
        .keySet()
        .stream()
        .filter(key -> !key.equals(UNRECOGNIZED_KEY) && !key.isEmpty())
        .collect(toUnmodifiableSet());
  }


  /**
   * Returns the message code, if available.
   *
   * @return  message code, or {@code null}
   */
  @Contract(pure = true)
  public String getCode() {
    return (String)get(Kind.CODE);
  }


  /**
   * Returns the template name, if available. If multiple templates have been recorded, the innermost one is returned.
   *
   * @return  template name, or {@code null}
   */
  @Contract(pure = true)
  public String getTemplate() {
    return (String)get(Kind.TEMPLATE);
  }


  /**
   * Returns the message locale, if available.
   *
   * @return  message locale, or {@code null}
   */
  @Contract(pure = true)
  public Locale getLocale() {
    return (Locale)get(Kind.LOCALE);
  }


  /**
   * Returns the message parameter name, if available. If multiple parameters have been recorded, the innermost one is
   * returned.
   *
   * @return  message parameter name, or {@code null}
   */
  @Contract(pure = true)
  public String getParameter() {
    return (String)get(Kind.PARAMETER);
  }


  /**
   * Returns the name of the post formatter being applied, if available. If multiple post formatters have been
   * recorded, the innermost one is returned.
   *
   * @return  post formatter name, or {@code null}
   *
   * @since 0.25.0
   */
  @Contract(pure = true)
  public String getPostFormatter() {
    return (String)get(Kind.POST_FORMATTER);
  }


  /**
   * Returns a copy of this exception with the given {@code code} added to the formatting context.
   * <p>
   * The code is recorded only once; if a code is already available or the given code is generated, it is ignored
   * and this exception is returned.
   *
   * @param code  code, not {@code null}
   *
   * @return  new exception based on the current exception with the code added or this exception, never {@code null}
   */
  public @NotNull MessageFormatException withCode(@NotNull String code) {
    return isGeneratedCode(code) || get(Kind.CODE) != null ? this : copyWith(Kind.CODE, code);
  }


  /**
   * Returns a copy of this exception with the given {@code template} added to the formatting context.
   * <p>
   * Generated template names are not recorded; in that case this exception is returned.
   *
   * @param template  template, not {@code null}
   *
   * @return  new exception based on the current exception with the template added or this exception,
   *          never {@code null}
   */
  public @NotNull MessageFormatException withTemplate(@NotNull String template)
  {
    return isGeneratedCode(template) || template.equals(getLast(Kind.TEMPLATE))
        ? this : copyWith(Kind.TEMPLATE, template);
  }


  /**
   * Returns a copy of this exception with the given {@code locale} added to the formatting context.
   * <p>
   * The locale is recorded only once; if a locale is already available, the given locale is ignored and this
   * exception is returned.
   *
   * @param locale  locale, not {@code null}
   *
   * @return  new exception based on the current exception with the locale added or this exception, never {@code null}
   */
  public @NotNull MessageFormatException withLocale(@NotNull Locale locale) {
    return get(Kind.LOCALE) != null ? this : copyWith(Kind.LOCALE, locale);
  }


  /**
   * Returns a copy of this exception with the given {@code parameter} added to the formatting context.
   *
   * @param parameter  parameter, not {@code null}
   *
   * @return  new exception based on the current exception with the parameter added, never {@code null}
   */
  public @NotNull MessageFormatException withParameter(@NotNull String parameter) {
    return parameter.equals(getLast(Kind.PARAMETER)) ? this : copyWith(Kind.PARAMETER, parameter);
  }


  /**
   * Returns a copy of this exception with the given {@code postFormatter} added to the formatting context.
   *
   * @param postFormatter  post formatter name, not {@code null}
   *
   * @return  new exception based on the current exception with the post formatter added, never {@code null}
   *
   * @since 0.25.0
   */
  public @NotNull MessageFormatException withPostFormatter(@NotNull String postFormatter) {
    return postFormatter.equals(getLast(Kind.POST_FORMATTER)) ? this : copyWith(Kind.POST_FORMATTER, postFormatter);
  }


  /**
   * Creates a copy of this exception with the given context value added. The copy keeps the cause and the stack trace
   * of this exception, so the location where the error was originally reported is not lost.
   *
   * @param kind   context kind, not {@code null}
   * @param value  context value, not {@code null}
   *
   * @return  new exception with the given context added, never {@code null}
   */
  @Contract("_, _ -> new")
  private @NotNull MessageFormatException copyWith(@NotNull Kind kind, @NotNull Object value)
  {
    final var newContext = new ArrayList<>(context);

    if (kind == Kind.LOCALE || kind == Kind.CODE)
      newContext.add(new Context(kind, kind.prefix, value));
    else
    {
      final var number = newContext.stream().filter(ctx -> ctx.kind == kind).count() + 1;
      newContext.add(new Context(kind, kind.prefix + number, value));
    }

    final var copy = new MessageFormatException(List.copyOf(newContext), getCause());

    copy.setStackTrace(getStackTrace());

    return copy;
  }


  /**
   * Returns the value of the first recorded context of the given kind.
   *
   * @param kind  context kind, not {@code null}
   *
   * @return  context value, or {@code null} if no context of this kind has been recorded
   */
  @Contract(pure = true)
  private Object get(@NotNull Kind kind)
  {
    for(var ctx: context)
      if (ctx.kind == kind)
        return ctx.value;

    return null;
  }


  /**
   * Returns the value of the last recorded context of the given kind.
   *
   * @param kind  context kind, not {@code null}
   *
   * @return  context value, or {@code null} if no context of this kind has been recorded
   */
  @Contract(pure = true)
  private Object getLast(@NotNull Kind kind)
  {
    for(int n = context.size(); n-- > 0;)
      if (context.get(n).kind == kind)
        return context.get(n).value;

    return null;
  }


  /**
   * Returns {@code ex} if it is a {@code MessageFormatException}, or a new {@code MessageFormatException} with
   * {@code ex} as its cause otherwise.
   *
   * @param ex  exception, or {@code null}
   *
   * @return  {@code MessageFormatException} instance, never {@code null}
   */
  @Contract(pure = true)
  public static @NotNull MessageFormatException of(Exception ex)
  {
    return ex instanceof MessageFormatException
        ? (MessageFormatException)ex
        : new MessageFormatException(ex);
  }




  /**
   * Kinds of formatting context that can be recorded for an exception.
   *
   * @since 0.25.0
   */
  private enum Kind
  {
    /** Name of the parameter being formatted. */
    PARAMETER("p"),

    /** Name of the post formatter being applied. */
    POST_FORMATTER("pf"),

    /** Name of the template being formatted. */
    TEMPLATE("t"),

    /** Code of the message being formatted. */
    CODE("c"),

    /** Locale used for formatting. */
    LOCALE("l");


    /** Prefix of the context keys of this kind. */
    private final String prefix;


    /**
     * Constructs a context kind with the given context key prefix.
     *
     * @param prefix  context key prefix, not {@code null}
     */
    Kind(@NotNull String prefix) {
      this.prefix = prefix;
    }
  }




  /**
   * Recorded formatting context.
   *
   * @param kind   context kind, not {@code null}
   * @param key    context key (e.g. {@code p1}), not {@code null}
   * @param value  context value ({@link String} or {@link Locale}), not {@code null}
   *
   * @since 0.25.0
   */
  private record Context(@NotNull Kind kind, @NotNull String key, @NotNull Object value) implements Serializable
  {
    /**
     * Returns the context value as it is shown in the detail message. Locales are shown by their display name in
     * English, or as {@code ROOT} for the root locale.
     *
     * @return  context value as text, never {@code null}
     */
    @Contract(pure = true)
    private @NotNull String valueAsString()
    {
      return value instanceof Locale locale
          ? ROOT.equals(locale) ? "ROOT" : locale.getDisplayName(UK)
          : value.toString();
    }
  }
}
