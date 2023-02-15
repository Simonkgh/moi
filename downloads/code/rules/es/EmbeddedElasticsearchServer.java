// Author: Simon Kitching
// This code is in the public domain
package net.vonos.testsupport.elastic;

import org.apache.commons.io.FileUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.node.Node;

import java.io.File;
import java.io.IOException;

/**
 * Test helper class which starts up an Elasticsearch instance in the current JVM.
 */
public class EmbeddedElasticsearchServer {

    // Suitable location for use with Maven
    private static final String DEFAULT_HOME_DIRECTORY = "target/elasticsearch-home";

    // The embedded ES instance
    private final Node node;

    // Setting "path.home" should point to the directory in which Elasticsearch is installed.
    private final String homeDirectory;

    /**
     * Default Constructor.
     */
    public EmbeddedElasticsearchServer() {
        this(DEFAULT_HOME_DIRECTORY);
    }

    /**
     * Explicit Constructor.
     */
    public EmbeddedElasticsearchServer(String homeDirectory) {
        try {
            FileUtils.deleteDirectory(new File(homeDirectory));
        } catch(IOException e) {
            throw new RuntimeException("Unable to clean embedded elastic-search home dir", e);
        }

        this.homeDirectory = homeDirectory;

        Settings.Builder elasticsearchSettings = Settings.builder()
            .put(Node.NODE_NAME_SETTING.getKey(), "testNode")
            .put(NetworkModule.TRANSPORT_TYPE_KEY, "local")
            .put(ClusterName.CLUSTER_NAME_SETTING.getKey(), "testCluster")
            .put(Environment.PATH_HOME_SETTING.getKey(), homeDirectory)
            .put(NetworkModule.HTTP_ENABLED.getKey(), false)
            .put("discovery.zen.ping_timeout", 0); // make startup faster

        this.node = new Node(elasticsearchSettings.build());
    }

    public void start() throws Exception {
        this.node.start();
    }

    public Client getClient() {
        return node.client();
    }

    public void shutdown() throws IOException {
        node.close();

        try {
            FileUtils.deleteDirectory(new File(homeDirectory));
        } catch (IOException e) {
            throw new RuntimeException("Could not delete home directory of embedded elasticsearch server", e);
        }
    }
}
