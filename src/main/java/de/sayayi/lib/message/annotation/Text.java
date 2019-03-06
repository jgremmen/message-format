package de.sayayi.lib.message.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Text
{
  /** Message locale. Either the language code (de, es) or the language with country (de_DE, fr_CA) */
  String locale() default "";

  /** Localized message text */
  String text();
}
