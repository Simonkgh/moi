// Author: Simon Kitching
// This code is in the public domain
package net.vonos.elastic;

import net.vonos.elastic.ESRule;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Integration tests for class ESUtils.
 */
public class ESUtilsITest {

    private static final String MAPPING1 = "{'properties':{'name':{'type':'keyword'},'val':{'type':'integer'}}}";
    private static final String MAPPING2 = "{'properties':{'name':{'type':'keyword'},'val':{'type':'keyword'}}}";
    private static final String MAPPING3 =
        "{'dynamic':'strict','properties':{'name':{'type':'keyword'},'xyz':{'type':'keyword'}}}";

    @Rule
    public final ESRule esRule = new ESRule();

    @Test
    public void testBasics() throws Exception {
        Client client = esRule.getClient();

        Map<String, Object> doc1 = new HashMap<>();
        doc1.put("name", "fred");
        doc1.put("val", Integer.valueOf(33));

        boolean result;

        // Verify the index does not exist
        Assert.assertEquals(false, ESLoaderUtils.existsIndex(client, "foo"));

        // Define the index
        ESLoaderUtils.createIndex(client, 2, 1, "foo");
        Assert.assertEquals(true, ESLoaderUtils.existsIndex(client, "foo"));

        // Define another index
        ESLoaderUtils.createIndex(client, 2, 1, "bar");
        Assert.assertEquals(true, ESLoaderUtils.existsIndex(client, "foo"));

        // Add a mapping to foo
        result = ESLoaderUtils.putMapping(client, "foo", "type1", MAPPING1.replace('\'', '"'));
        Assert.assertEquals(true, result);

        // Add the same mapping again
        result = ESLoaderUtils.putMapping(client, "foo", "type1", MAPPING1.replace('\'', '"'));
        Assert.assertEquals(true, result);

        // Add the same mapping again under a different doctype
        result = ESLoaderUtils.putMapping(client, "foo", "type2", MAPPING1.replace('\'', '"'));
        Assert.assertEquals(true, result);

        // Update a mapping with an incompatible mapping -- should fail
        result = ESLoaderUtils.putMapping(client, "foo", "type2", MAPPING2.replace('\'', '"'));
        Assert.assertEquals(false, result);

        // create alias
        ESLoaderUtils.createAlias(client, "foo", "aliasFoo");
        Assert.assertEquals(true, ESLoaderUtils.existsIndex(client, "aliasFoo"));

        // Insert a record into foo and count foo
        Assert.assertEquals(0, ESLoaderUtils.countIndex(client, "foo"));
        ESLoaderUtils.saveSync(client, "foo", "type1", doc1);
        Assert.assertEquals(1, ESLoaderUtils.countIndex(client, "foo"));

        // copy index foo to bar - should work
        Assert.assertEquals(0, ESLoaderUtils.countIndex(client, "bar"));
        ESLoaderUtils.copyIndex(client, TimeValue.timeValueMinutes(2), "foo", "bar");
        ESLoaderUtils.refreshIndex(client, "bar");
        Assert.assertEquals(1, ESLoaderUtils.countIndex(client, "bar"));

        // Define incompatible mappings for the same doctype in two different indexes, then try to copy - should fail
        // as a document registered with doctype=type1 in index foo cannot be written as doctype=type1 in index baz.
        //
        // Note: MAPPING2 cannot be added to index foo or bar at all, even under a different doctype, as ES (lucene)
        // does not allow two types for the same field-name in different mappings of the same index. However if
        // MAPPING2 is added to index baz, then the copyIndex succeeds - because by default ES automatically converts
        // integers into strings when necessary. Interestingly, this means MAPPING1 and MAPPING2 are not compatible
        // at the "put mapping" level, but are compatible at the "reindex" level..
        //
        // The document (doc1) of type "type1" already in index "foo" cannot be inserted into index "baz" as type1
        // because that means applying its source to MAPPING3 - but MAPPING3 is strict and does not define property
        // "val", so the insert fails.
        ESLoaderUtils.createIndex(client, 2, 1, "baz");
        ESLoaderUtils.putMapping(client, "baz", "type1", MAPPING3.replace('\'', '"'));

        try {
            ESLoaderUtils.copyIndex(client, TimeValue.timeValueMinutes(2), "foo", "baz");
            Assert.fail("Exception not thrown when expected");
        } catch(IOException e) {
            Assert.assertTrue(e.getMessage().contains("Reindex failed"));
        }

        Assert.assertEquals(0, ESLoaderUtils.countIndex(client, "baz"));

        // Drop index foo
        ESLoaderUtils.dropIndex(client, "foo");
        Assert.assertEquals(false, ESLoaderUtils.existsIndex(client, "foo"));
        Assert.assertEquals(false, ESLoaderUtils.existsIndex(client, "aliasFoo")); // alias for foo disappears too
        Assert.assertEquals(true, ESLoaderUtils.existsIndex(client, "bar"));
    }
}
