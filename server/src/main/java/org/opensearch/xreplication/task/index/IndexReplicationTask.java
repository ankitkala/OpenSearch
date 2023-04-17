/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.xreplication.task.index;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.admin.cluster.state.ClusterStateRequest;
import org.opensearch.action.admin.indices.alias.Alias;
import org.opensearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.opensearch.action.admin.indices.create.CreateIndexRequest;
import org.opensearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.opensearch.action.support.IndicesOptions;
import org.opensearch.client.Client;
import org.opensearch.cluster.ClusterState;
import org.opensearch.cluster.metadata.AliasMetadata;
import org.opensearch.cluster.metadata.IndexMetadata;
import org.opensearch.cluster.metadata.MappingMetadata;
import org.opensearch.cluster.routing.allocation.decider.EnableAllocationDecider;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.settings.Settings;
import org.opensearch.index.IndexSettings;
import org.opensearch.persistent.AllocatedPersistentTask;
import org.opensearch.persistent.PersistentTasksCustomMetadata;
import org.opensearch.tasks.TaskId;
import org.opensearch.xreplication.services.XReplicationLeaderService;

import java.util.*;

public class IndexReplicationTask extends AllocatedPersistentTask {
    private String indexName;
    private Client client;
    private ClusterService clusterService;
    private XReplicationLeaderService leaderService;
    private String[] followers;
    private final Logger logger = LogManager.getLogger(IndexReplicationTask.class);

    private static final List<String> blockedSettings = Arrays.asList(
        IndexMetadata.INDEX_READ_ONLY_SETTING.getKey(),
        IndexMetadata.INDEX_BLOCKS_READ_SETTING.getKey(),
        IndexMetadata.INDEX_BLOCKS_WRITE_SETTING.getKey(),
        IndexMetadata.INDEX_BLOCKS_METADATA_SETTING.getKey(),
        IndexMetadata.INDEX_BLOCKS_READ_ONLY_ALLOW_DELETE_SETTING.getKey(),
        EnableAllocationDecider.INDEX_ROUTING_REBALANCE_ENABLE_SETTING.getKey(),
        EnableAllocationDecider.INDEX_ROUTING_ALLOCATION_ENABLE_SETTING.getKey(),
        IndexSettings.INDEX_SOFT_DELETES_RETENTION_LEASE_PERIOD_SETTING.getKey(),
        IndexMetadata.SETTING_CREATION_DATE,
        IndexMetadata.SETTING_INDEX_PROVIDED_NAME,
        IndexMetadata.SETTING_INDEX_UUID,
        IndexMetadata.SETTING_CREATION_DATE,
        IndexMetadata.SETTING_VERSION_CREATED
        );

    public IndexReplicationTask(long id, String type, String action, TaskId parentTaskId,
                                PersistentTasksCustomMetadata.PersistentTask<IndexReplicationParams> taskInProgress,
                                Map<String, String> headers, Client client, ClusterService clusterService,
                                XReplicationLeaderService leaderService) {
        super(id, type, action, String.format("FCR:IndexReplicationTask:{}", taskInProgress.getParams().getIndexName()), parentTaskId, headers);
        this.indexName = taskInProgress.getParams().getIndexName();
        this.leaderService = leaderService;
        this.clusterService = clusterService;
        this.client = client;
        this.followers = taskInProgress.getParams().getFollowers();
    }

    public void execute() {
        //List<String> followerAliases = ReplicationHelper.getAllClusterAliases(clusterService.getSettings());
        logger.info("[ankikala] Creating index {} on these followers: {}", indexName, Arrays.asList(followers));
        createFollowerIndex();
    }

    private void createFollowerIndex() {
        ClusterState state = getClusterState(indexName);
        Settings leaderIndexSettings = state.metadata().index(indexName).getSettings();
        logger.info("Leader index settings: {}", leaderIndexSettings);
        //TODO: Add FCR specific settings if required
        Settings.Builder followerSettingsBuilder = Settings.builder().put(leaderIndexSettings);
        blockedSettings.forEach(followerSettingsBuilder::remove);
        followerSettingsBuilder.put(IndexMetadata.CCR_REPLICATING_FROM_INDEX_SETTING.getKey(), leaderIndexSettings.get(IndexMetadata.SETTING_INDEX_UUID));
        CreateIndexRequest request = new CreateIndexRequest(indexName, followerSettingsBuilder.build());
        logger.info("follower index settings: {}", request.settings());

        // mappings
        request.mapping(getMappings().source().string());

        //aliases
        getAliases().stream().map(
            aliasMetadata -> new Alias(aliasMetadata.alias())
                .searchRouting(aliasMetadata.searchRouting())
                .indexRouting(aliasMetadata.indexRouting())
                .writeIndex(aliasMetadata.writeIndex())
                .isHidden(aliasMetadata.isHidden()))
        .forEach(request::alias);

        leaderService.createFollowerIndex(request, List.of(followers));
        return;
    }

    private List<AliasMetadata> getAliases() {
        GetAliasesRequest request = new GetAliasesRequest().indices(indexName);
        return client.admin().indices().getAliases(request).actionGet().getAliases().get(indexName);
    }

    private MappingMetadata getMappings() {
        GetMappingsRequest getMappingsRequest = new GetMappingsRequest();
        getMappingsRequest.indices(indexName);
        return client.admin().indices().getMappings(getMappingsRequest).actionGet().getMappings().get(indexName);

    }


    private ClusterState getClusterState(String... indices) {
        ClusterStateRequest request = client.admin().cluster().prepareState().clear()
            .setIndices(indices)
            .setMetadata(true)
            .setNodes(false) // TODO: Check if required
            .setRoutingTable(false) // TODO: Check if required
            .setIndicesOptions(IndicesOptions.strictSingleIndexNoExpandForbidClosed()).request();
        return client.admin().cluster().state(request).actionGet().getState(); // TODO: add timeouts to the actionGet
    }
}
