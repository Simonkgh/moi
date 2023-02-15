package example;

import java.util.ArrayList;

import example.GuiceSubclassMatcher;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matcher;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

/**
 * A Guice module that initialises an instance of the Publisher class, and automatically
 * subscribes all known Listener implementations to it.
 */
public class PublisherModule extends AbstractModule
{
  /** Class invoked every time Guice encounters a binding to something that implements Listener.  */
  static class ListenerRegistry
      implements TypeListener
  {
    /** List of providers for all Listener subclasses known to Guice. */
    private ArrayList<Provider<? extends Listener>> providers = new ArrayList<Provider<? extends Listener>>(10);

    /**
     * As each Listener subclass is found by Guice, store a provider for that type in the providers list.
     */
    public <I> void hear(TypeLiteral<I> paramTypeLiteral, TypeEncounter<I> paramTypeEncounter)
    {
      // This cast is ugly; there is probably a way to avoid it using correct generics signatures....
      @SuppressWarnings("unchecked")
      Class<? extends Listener> clazz = (Class<? extends Listener>) paramTypeLiteral.getType();
      Provider<? extends Listener> prov = paramTypeEncounter.getProvider(clazz);
      providers.add(prov);
    }
  }

  /** Registry of providers. */
  private ListenerRegistry listenerRegistry = new ListenerRegistry();

  @Override
  protected void configure()
  {
    Matcher<? super TypeLiteral<?>> matcher = new GuiceSubclassMatcher<TypeLiteral<?>>(Listener.class);
    bindListener(matcher, listenerRegistry);
  }

  /**
   * Create a new Publisher instance.
   * <p>
   * In this case, the Publisher is a singleton - but it doesn't have to be. The listener objects
   * will be singletons if they have been declared so, otherwise they will be new instances; the
   * Guice Provider<Listener> functionality handles that automatically.
   */
  @Provides
  @Singleton
  Publisher getPublisher()
  {
    Publisher p = new Publisher();
    for (Provider<? extends Listener> provider : listenerRegistry.providers)
    {
      Listener listener = provider.get();
      p.subscribe(listener);
    }
    return p;
  }
}
