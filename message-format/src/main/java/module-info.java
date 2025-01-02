module de.sayayi.lib.message {

  requires de.sayayi.lib.antlr;
  requires org.antlr.antlr4.runtime;
  requires java.sql;

  requires static java.xml;
  requires static lombok;
  requires static org.jetbrains.annotations;

  exports de.sayayi.lib.message;
  exports de.sayayi.lib.message.adopter;
  exports de.sayayi.lib.message.annotation;
  exports de.sayayi.lib.message.exception;
  exports de.sayayi.lib.message.formatter;
  exports de.sayayi.lib.message.formatter.named;
  exports de.sayayi.lib.message.formatter.named.extra;
  exports de.sayayi.lib.message.formatter.runtime;
  exports de.sayayi.lib.message.formatter.runtime.extra;
  exports de.sayayi.lib.message.part;
  exports de.sayayi.lib.message.part.normalizer;
  exports de.sayayi.lib.message.part.parameter;
  exports de.sayayi.lib.message.part.parameter.key;
  exports de.sayayi.lib.message.part.parameter.value;
  exports de.sayayi.lib.message.util;

  uses de.sayayi.lib.message.formatter.ParameterFormatter;
}