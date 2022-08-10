/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.indices.replication.checkpoint.crosscluster;

import org.opensearch.client.Client;
import org.opensearch.cluster.node.DiscoveryNode;

public class RemoteClusterConfig {
    private final Client client;
    private final DiscoveryNode targetNode;
    private final String remoteClusterAlias;

    public RemoteClusterConfig(Client client, DiscoveryNode targetNode, String remoteClusterAlias) {
        this.client = client;
        this.targetNode = targetNode;
        this.remoteClusterAlias = remoteClusterAlias;
    }

    public Client getRemoteClient() {
        return client;
    }

    public DiscoveryNode getTargetNode() {
        return targetNode;
    }

    public String remoteClusterAlias() {
        return remoteClusterAlias;
    }
}
