module de.sayayi.lib.message.spring {

  requires transitive de.sayayi.lib.message;
  requires transitive de.sayayi.lib.message.annotations;

  requires spring.core;
  requires spring.context;
  requires spring.expression;

  requires static org.jetbrains.annotations;

  exports de.sayayi.lib.message.spring;
  exports de.sayayi.lib.message.spring.adopter;
  exports de.sayayi.lib.message.spring.formatter;

}