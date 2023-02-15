package example;

import com.google.inject.AbstractModule;

/**
 * A Guice module that initialises one or more concrete Listener implementations.
 * <p>
 * In a real application, this code could be in a completely different part of the
 * application from the Publisher class. And if the listener1 class has an implicit
 * Guice binding, then this code isn't needed. However Guice does need to know about
 * the existence of the Listener1 class in order for the "auto-detection" done in
 * the PublisherModule to work.
 */
public class ListenerModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(Listener1.class);
  }
}
