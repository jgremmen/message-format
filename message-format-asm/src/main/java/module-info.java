module de.sayayi.lib.message.asm {

  requires transitive de.sayayi.lib.message;
  requires transitive org.objectweb.asm;

  requires static org.jetbrains.annotations;

  exports de.sayayi.lib.message.adopter.asm;
}