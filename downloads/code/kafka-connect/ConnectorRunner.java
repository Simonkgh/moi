package net.vonos.kafka.connect.example.runner;

import org.apache.kafka.common.utils.SystemTime;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.common.utils.Utils;
import org.apache.kafka.connect.runtime.Connect;
import org.apache.kafka.connect.runtime.Herder;
import org.apache.kafka.connect.runtime.Worker;
import org.apache.kafka.connect.runtime.rest.RestServer;
import org.apache.kafka.connect.runtime.rest.entities.ConnectorInfo;
import org.apache.kafka.connect.runtime.standalone.StandaloneConfig;
import org.apache.kafka.connect.runtime.standalone.StandaloneHerder;
import org.apache.kafka.connect.storage.FileOffsetBackingStore;
import org.apache.kafka.connect.util.Callback;
import org.apache.kafka.connect.util.FutureCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;

/**
 * Starts a standalone Kafka-connect instance in the current JVM.
 * <p>
 * Useful for interactively testing code during development; just start this class in debug mode from an IDE.
 * </p>
 * <p>
 * Derived from class org.apache.kafka.connect.cli.ConnectStandalone, which is the class used to start a
 * standalone-kafka-connector instance from the standard Kafka startup scripts.
 * </p>
 */
public class ConnectorRunner {
  private static final Logger log = LoggerFactory.getLogger(ConnectorRunner.class);

  public static final String WORKER_PROPS = "ConnectorRunnerWorker.properties";
  public static final String CONNECTOR_PROPS = "ConnectorRunnerConnector.properties";

  private ConnectorRunner() {
  }

  /**
   * Looks for file named ConnectorRunnerWorker.properties and ConnectorRunnerConnector.properties in the current working
   * directory.
   */
  public static void main(String[] args) throws Exception {

    Map<String, String> workerProps = Utils.propsToStringMap(Utils.loadProps(WORKER_PROPS));
    Map<String, String> connectorProps = Utils.propsToStringMap(Utils.loadProps(CONNECTOR_PROPS));

    StandaloneConfig config = new StandaloneConfig(workerProps);
    RestServer rest = new RestServer(config);
    URI advertisedUrl = rest.advertisedUrl();

    Time time = new SystemTime();
    String workerId = advertisedUrl.getHost() + ":" + advertisedUrl.getPort();
    Worker worker = new Worker(workerId, time, config, new FileOffsetBackingStore());
    Herder herder = new StandaloneHerder(worker);
    Connect connect = new Connect(herder, rest);

    // Start the framework
    try {
      connect.start();
    } catch (Throwable t) {
      log.error("Failed to start connector framework", t);
      System.exit(-1);
    }

    // Register the (single) connector.
    try {
      // Callback which will be invoked when connector startup completes sucessfully _or_ with error.
      FutureCallback<Herder.Created<ConnectorInfo>> cb = new FutureCallback(new Callback<Herder.Created<ConnectorInfo>>() {
        public void onCompletion(Throwable error, Herder.Created<ConnectorInfo> info) {
          if(error != null) {
            log.error("Failed to create connector.");
          } else {
            log.info("Created connector {}", ((ConnectorInfo)info.result()).name());
          }
        }
      });

      herder.putConnectorConfig((String)connectorProps.get("name"), connectorProps, false, cb);
      cb.get();
    } catch (Throwable t) {
      log.error("Failed to start connector", t);
      connect.stop();
    }

    connect.awaitStop();
  }
}


