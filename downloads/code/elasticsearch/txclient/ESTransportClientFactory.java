// Author: Simon Kitching
// This code is in the public domain
package net.vonos.elasticsearch;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import vwg.audi.tracestore.common.exception.CommonMessages;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * A factory for ElasticSearch TransportClient instances.
 * <p>
 * Normally, the getClient() method is called only once, during application startup.
 * </p>
 */
@Configuration
public class ESTransportClientFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ESTransportClientFactory.class);

    @Value("${es.hostList}")
    private String[] esHostList;

    @Value("${es.port}")
    private int esPort;

    @Value("${es.clusterName}")
    private String esClusterName;

    @Value("${es.sniff}")
    private boolean esSniff;

    /**
     * Return a singleton connection to elastic-search.
     */
    @Bean
    public Client getClient() {
        Settings settings = Settings.builder()
            .put("cluster.name", esClusterName)
            .put("client.transport.sniff", esSniff)
            .build();
        try {
            TransportClient client = new BasicTransportClient(settings);
            LOG.info("Created client instance '{}'", System.identityHashCode(client));
            for (String host : esHostList) {
                client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host), esPort));
            }
            return client;
        } catch (UnknownHostException uhe) {
            throw new RuntimeException(String.format(CommonMessages.GENERIC_LOG_MSG_WITH_ORIG_EXC,
                    CommonMessages.ES_UNKNOWN_HOST_ERR_CODE, CommonMessages
                            .ES_UNKNOWN_HOST_ERR_MSG), uhe);
        }
    }
}
