/*
 * Copyright 2026 Jeroen Gremmen
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

/**
 * Concrete {@link de.sayayi.lib.message.annotation.adopter.AnnotationAdopter AnnotationAdopter} implementations
 * backed by different bytecode analysis libraries:
 * <ul>
 *   <li>{@link de.sayayi.lib.message.annotation.adopter.lib.AsmAnnotationAdopter AsmAnnotationAdopter} –
 *       uses the standalone <a href="https://asm.ow2.io/">ASM</a> library</li>
 *   <li>{@link de.sayayi.lib.message.annotation.adopter.lib.ByteBuddyAnnotationAdopter ByteBuddyAnnotationAdopter} –
 *       uses the ASM library bundled with <a href="https://bytebuddy.net/">Byte Buddy</a></li>
 *   <li>{@link de.sayayi.lib.message.annotation.adopter.lib.SpringAnnotationAdopter SpringAnnotationAdopter} –
 *       uses the ASM library bundled with <a href="https://spring.io/projects/spring-framework">Spring Framework</a></li>
 * </ul>
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 */
package de.sayayi.lib.message.annotation.adopter.lib;
