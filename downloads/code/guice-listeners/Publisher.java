package example; 

import java.util.ArrayList;
import java.util.List;

/**
 * Generates "events" that it passes on to every "listener" subscribed to this class.
 */
public class Publisher {

  /** List of objects to be notified of events. */
  private List<Listener> listeners = new ArrayList<Listener>(10);

  /** Non-public constructor; this class usually used via its singleton instance.  */
  protected Publisher() {
  }

  /** Add a listener which will be called whenever a new event occurs.  */
  public void subscribe(Listener listener) {
    listeners.add(p);
  }

  /** Process an event.  */
  public void sendEvent(Object event) {
    for (Listener listener : listeners) {
      listener.handleEvent(event);
    }
  }
}
