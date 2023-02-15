// Author: Simon Kitching
// This code is in the public domain
package net.vonos.testsupport.elastic;

import org.elasticsearch.client.Client;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.util.Collections;

/**
 * A JUnit rule which starts an embedded elastic-search instance.
 * <p>
 * Tests which use this rule will run relatively slowly, and should only be used when more conventional unit tests are
 * not sufficient - eg when testing DAO-specific code.
 * </p>
 */
public class ESRule implements TestRule {
    /** An elastic-search cluster consisting of one node. */
    private EmbeddedElasticsearchServer eserver;

    /** The internal-transport client that talks to the local node. */
    private Client client;

    /**
     * Return a closure which starts an embedded ES instance, executes the unit-test, then shuts down the
     * ES instance.
     */
    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                eserver = new EmbeddedElasticSearchServer();
                eserver.start();

                client = eserver.getClient();
                loader = new ESIndicesLoader(client, 1, 1);
                try {
                    base.evaluate(); // execute the unit test
                } finally {
                    eserver.shutdown();
                }
            }
        };
    }

    /** Return the object through which operations can be performed on the ES cluster. */
    public Client getClient() {
        return client;
    }

    /**
     * When data is added to an index, it is not visible in searches until the next "refresh" has been performed.
     * Refreshes are normally done every second, but this makes it explicit..
     */
    public void refresh(String index) {
        try {
            client.admin().indices().prepareRefresh(index).execute().get();
        } catch(Exception e) {
            throw new RuntimeException("Failed to refresh index", e);
        }
    }
}
