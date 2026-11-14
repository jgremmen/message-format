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
package de.sayayi.lib.message.util;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.function.Executable;

import java.lang.ref.WeakReference;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Assertions shared by the tests of several classes.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
public final class AssertionTestSupport
{
  /** Maximum number of garbage collections to wait for a weak reference to be cleared. */
  private static final int MAX_GC_RUNS = 50;


  private AssertionTestSupport() {
  }


  /**
   * Asserts that the referent of {@code reference} is garbage collected. Garbage collection is requested up to
   * {@value #MAX_GC_RUNS} times until the reference is cleared.
   * <p>
   * The caller must not hold a strong reference to the referent, so it is best created by a separate method which
   * returns the weak reference only.
   *
   * @param reference  weak reference to the object which must be garbage collected, not {@code null}
   * @param message    failure message, not {@code null}
   *
   * @throws InterruptedException  if the current thread is interrupted while waiting for the garbage collector
   */
  @SuppressWarnings("BusyWait")
  public static void assertGarbageCollected(@NotNull WeakReference<?> reference, @NotNull String message)
      throws InterruptedException
  {
    for(var n = 0; n < MAX_GC_RUNS && reference.get() != null; n++)
    {
      System.gc();
      Thread.sleep(10);
    }

    assertNull(reference.get(), message);
  }


  /**
   * Asserts that {@code executable} throws a {@link NullPointerException} with the given message.
   *
   * @param expectedMessage  expected exception message, not {@code null}
   * @param executable       code which must reject a {@code null} value, not {@code null}
   */
  public static void assertNullRejected(@NotNull String expectedMessage, @NotNull Executable executable) {
    assertEquals(expectedMessage, assertThrowsExactly(NullPointerException.class, executable).getMessage());
  }


  /**
   * Asserts the {@code equals} and {@code hashCode} contract: {@code value} equals itself and {@code equal} in both
   * directions, both have the same hash code, {@code value} does not equal {@code null}, and each of {@code notEqual}
   * differs from {@code value} in both directions.
   *
   * @param value     value to check, not {@code null}
   * @param equal     value equal to {@code value}, but preferably a different instance, not {@code null}
   * @param notEqual  values not equal to {@code value}, not {@code null}
   */
  @SuppressWarnings({"EqualsWithItself", "ConstantValue", "SimplifiableAssertion"})
  public static void assertEqualsContract(@NotNull Object value, @NotNull Object equal, @NotNull Object... notEqual)
  {
    assertEquals(value, value);
    assertEquals(value, equal);
    assertEquals(equal, value);
    assertEquals(value.hashCode(), equal.hashCode());
    assertFalse(value.equals(null));

    for(var other: notEqual)
    {
      assertNotEquals(value, other);
      assertNotEquals(other, value);
    }
  }
}
