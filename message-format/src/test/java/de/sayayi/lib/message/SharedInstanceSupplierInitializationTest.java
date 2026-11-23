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
package de.sayayi.lib.message;

import de.sayayi.lib.message.formatter.DefaultFormatterService;
import de.sayayi.lib.message.formatter.post.PostFormatter;
import de.sayayi.lib.message.formatter.post.PostFormatterContext;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests the initialization of the shared instances ({@link MessageSupportFactory#shared()},
 * {@link DefaultFormatterService#getSharedInstance()}) by concurrent threads, with a service provider accessing the
 * shared message support while the shared formatter service instantiates it.
 * <p>
 * The shared instances are static, so each test loads the library with its own class loader to get uninitialized shared
 * instances. The service provider is only visible to that class loader and does not affect other tests.
 *
 * @author Jeroen Gremmen
 * @since 0.26.0
 */
@DisplayName("Shared instance initialization")
final class SharedInstanceSupplierInitializationTest
{
  @Test
  @DisplayName("Provider accessing the shared message support during concurrent initialization does not hang")
  void providerAccessingSharedMessageSupport(@TempDir Path serviceDir) throws Exception
  {
    final var servicesFile = serviceDir.resolve("META-INF/services/" + PostFormatter.class.getName());

    Files.createDirectories(servicesFile.getParent());
    Files.writeString(servicesFile, SharedSupportAccessingPostFormatter.class.getName());

    try(var classLoader = new LibraryClassLoader(serviceDir)) {
      final var messageSupportFactory = classLoader.loadClass(MessageSupportFactory.class.getName());
      final var defaultFormatterService = classLoader.loadClass(DefaultFormatterService.class.getName());

      final var formatterServiceFailure = new AtomicReference<Throwable>();
      final var messageSupportFailure = new AtomicReference<Throwable>();

      // initializes the shared formatter service, whose provider accesses the shared message support after a delay
      final var formatterServiceThread = startDaemon(formatterServiceFailure,
          () -> defaultFormatterService.getMethod("getSharedInstance").invoke(null));

      Thread.sleep(50);

      // initializes the shared message support, while the shared formatter service is being initialized
      final var messageSupportThread = startDaemon(messageSupportFailure,
          () -> messageSupportFactory.getMethod("shared").invoke(null));

      // the provider makes the initialization fail with a recursion error, but it must not hang
      formatterServiceThread.join(5000);
      messageSupportThread.join(5000);

      assertFalse(formatterServiceThread.isAlive(), "shared formatter service initialization hangs");
      assertFalse(messageSupportThread.isAlive(), "shared message support initialization hangs");

      // the provider has been instantiated and its recursive access is reported
      assertTrue(isRecursionError(formatterServiceFailure.get()), "recursion not reported");
    }
  }


  @Test
  @DisplayName("Provider class initializer accessing the shared formatter service on another thread does not deadlock")
  void providerClassInitializerAccessingSharedFormatterService(@TempDir Path serviceDir) throws Exception
  {
    final var servicesFile = serviceDir.resolve("META-INF/services/" + PostFormatter.class.getName());

    Files.createDirectories(servicesFile.getParent());
    Files.writeString(servicesFile, SharedServiceAccessingStaticPostFormatter.class.getName());

    try(var classLoader = new LibraryClassLoader(serviceDir)) {
      final var defaultFormatterService = classLoader.loadClass(DefaultFormatterService.class.getName());
      final var classInitResult = new AtomicReference<Object>();
      final var classInitFailure = new AtomicReference<Throwable>();
      final var sharedInstance = new AtomicReference<Object>();
      final var sharedInstanceFailure = new AtomicReference<Throwable>();

      // initializes the provider class, whose static initializer accesses the shared formatter service after a delay
      final var classInitThread = startDaemon(classInitFailure, () -> classInitResult.set(Class
          .forName(SharedServiceAccessingStaticPostFormatter.class.getName(), true, classLoader)
          .getField("SHARED_SERVICE")
          .get(null)));

      Thread.sleep(50);

      // creates the shared formatter service, which instantiates the provider while its class is being initialized
      final var sharedInstanceThread = startDaemon(sharedInstanceFailure,
          () -> sharedInstance.set(defaultFormatterService.getMethod("getSharedInstance").invoke(null)));

      classInitThread.join(5000);
      sharedInstanceThread.join(5000);

      assertFalse(classInitThread.isAlive(), "provider class initialization hangs");
      assertFalse(sharedInstanceThread.isAlive(), "shared formatter service initialization hangs");

      assertNull(classInitFailure.get(), "provider class initialization failed");
      assertNull(sharedInstanceFailure.get(), "shared formatter service initialization failed");

      // both threads see the same shared instance
      assertNotNull(sharedInstance.get());
      assertSame(sharedInstance.get(), classInitResult.get());
    }
  }


  private static @NotNull Thread startDaemon(@NotNull AtomicReference<Throwable> failure,
                                             @NotNull Initialization initialization)
  {
    final var thread = new Thread(() -> {
      try {
        initialization.run();
      } catch(Throwable ex) {
        failure.set(ex);
      }
    });

    thread.setDaemon(true);
    thread.start();

    return thread;
  }


  private static boolean isRecursionError(Throwable throwable)
  {
    for(; throwable != null; throwable = throwable.getCause())
    {
      if (throwable instanceof IllegalStateException &&
          "recursive shared instance initialization".equals(throwable.getMessage()))
        return true;
    }

    return false;
  }




  @FunctionalInterface
  private interface Initialization {
    void run() throws Exception;
  }




  /**
   * Post formatter service provider which accesses the shared message support in its constructor.
   */
  public static final class SharedSupportAccessingPostFormatter implements PostFormatter
  {
    public SharedSupportAccessingPostFormatter() throws InterruptedException
    {
      // give the other thread time to start initializing the shared message support
      Thread.sleep(200);

      MessageSupportFactory.shared();
    }


    @Override
    public @NotNull String getName() {
      return "shared-support-accessing";
    }


    @Override
    public @NotNull String format(@NotNull String string, @NotNull PostFormatterContext context) {
      return string;
    }
  }




  /**
   * Post formatter service provider whose static initializer accesses the shared formatter service.
   */
  public static final class SharedServiceAccessingStaticPostFormatter implements PostFormatter
  {
    public static final Object SHARED_SERVICE;


    static
    {
      try {
        // give the other thread time to start creating the shared formatter service
        Thread.sleep(200);
      } catch(InterruptedException ex) {
        Thread.currentThread().interrupt();
      }

      SHARED_SERVICE = DefaultFormatterService.getSharedInstance();
    }


    @Override
    public @NotNull String getName() {
      return "shared-service-accessing-static";
    }


    @Override
    public @NotNull String format(@NotNull String string, @NotNull PostFormatterContext context) {
      return string;
    }
  }




  /**
   * Class loader loading the library and test classes itself, instead of delegating to its parent, so the loaded
   * library has its own static state. Additional resources, such as service files, are taken from a directory.
   */
  private static final class LibraryClassLoader extends URLClassLoader
  {
    private static final String LIBRARY_PACKAGE_PREFIX = "de.sayayi.lib.message.";


    private LibraryClassLoader(@NotNull Path resourceDir) throws MalformedURLException
    {
      super(new URL[] {
          location(MessageSupportFactory.class),
          location(SharedInstanceSupplierInitializationTest.class),
          resourceDir.toUri().toURL()
      }, SharedInstanceSupplierInitializationTest.class.getClassLoader());
    }


    private static @NotNull URL location(@NotNull Class<?> type) {
      return type.getProtectionDomain().getCodeSource().getLocation();
    }


    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
      if (!name.startsWith(LIBRARY_PACKAGE_PREFIX))
        return super.loadClass(name, resolve);

      synchronized(getClassLoadingLock(name))
      {
        var type = findLoadedClass(name);

        if (type == null)
        {
          try {
            type = findClass(name);
          } catch(ClassNotFoundException ex) {
            // e.g. test fixtures, which are not part of the library
            return super.loadClass(name, resolve);
          }
        }

        if (resolve)
          resolveClass(type);

        return type;
      }
    }

  }
}
