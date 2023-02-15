// Author: Simon Kitching
// This code is in the public domain
package net.vonos.elasticsearch;

import io.netty.util.ThreadDeathWatcher;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.transport.Netty4Plugin;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * An Elasticsearch Client implementation which communicates over TCP using the "node client" protocol.
 * <p>
 * This class is derived from org.elasticsearch.transport.client.PreBuiltTransportClient in artifact
 * "org.elasticsearch.client:transport:5.1.1".
 * </p>
 */
public class BasicTransportClient extends TransportClient {

    public BasicTransportClient(Settings settings) {
        super(settings, Settings.EMPTY, addPlugins(Collections.singletonList(Netty4Plugin.class)), null);
    }

    @Override
    public void close() {
        super.close();
        if (NetworkModule.TRANSPORT_TYPE_SETTING.exists(settings) == false
            || NetworkModule.TRANSPORT_TYPE_SETTING.get(settings).equals(Netty4Plugin.NETTY_TRANSPORT_NAME)) {
            try {
                GlobalEventExecutor.INSTANCE.awaitInactivity(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            try {
                ThreadDeathWatcher.awaitInactivity(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
