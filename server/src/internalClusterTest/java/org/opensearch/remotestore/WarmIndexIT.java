/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.remotestore;

import org.opensearch.action.admin.indices.get.GetIndexRequest;
import org.opensearch.action.admin.indices.get.GetIndexResponse;
import org.opensearch.cluster.metadata.IndexMetadata;
import org.opensearch.common.settings.Settings;
import org.opensearch.index.IndexSettings;
import org.opensearch.indices.replication.common.ReplicationType;

import static org.opensearch.cluster.metadata.IndexMetadata.INDEX_TYPE_WARM_ENABLED;
import static org.opensearch.cluster.metadata.IndexMetadata.SETTING_REMOTE_SEGMENT_STORE_REPOSITORY;
import static org.opensearch.cluster.metadata.IndexMetadata.SETTING_REMOTE_STORE_ENABLED;
import static org.opensearch.cluster.metadata.IndexMetadata.SETTING_REMOTE_TRANSLOG_STORE_REPOSITORY;
import static org.opensearch.cluster.metadata.IndexMetadata.SETTING_REPLICATION_TYPE;
import static org.opensearch.index.IndexSettings.INDEX_REMOTE_TRANSLOG_BUFFER_INTERVAL_SETTING;
import static org.opensearch.test.hamcrest.OpenSearchAssertions.assertAcked;

public class WarmIndexIT extends RemoteStoreBaseIntegTestCase {
    public void testWarmIndexCreate() throws Exception {
        Settings settings = Settings.builder()
            .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1)
            .put(IndexMetadata.INDEX_TYPE_WARM_ENABLED, true)
            .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
            .build();
        assertAcked(client().admin().indices().prepareCreate("test-idx-1").setSettings(settings).get());
        GetIndexResponse getIndexResponse = client().admin()
            .indices()
            .getIndex(new GetIndexRequest().indices("test-idx-1").includeDefaults(true))
            .get();
        Settings indexSettings = getIndexResponse.settings().get("test-idx-1");
        assertEquals(ReplicationType.SEGMENT.toString(), indexSettings.get(SETTING_REPLICATION_TYPE));
        assertEquals("true", indexSettings.get(SETTING_REMOTE_STORE_ENABLED));
        assertEquals(REPOSITORY_NAME, indexSettings.get(SETTING_REMOTE_SEGMENT_STORE_REPOSITORY));
        assertEquals(REPOSITORY_2_NAME, indexSettings.get(SETTING_REMOTE_TRANSLOG_STORE_REPOSITORY));
        assertEquals("true", indexSettings.get(INDEX_TYPE_WARM_ENABLED));
    }
}
