/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.index.engine;

/**
 * Engine Factory implementation used with Segment Replication that wires up replica shards with an ${@link NRTReplicationEngine}
 * and primary with an ${@link InternalEngine}
 *
 * @opensearch.internal
 */
public class NRTReplicationEngineFactory implements EngineFactory {
    @Override
    public Engine newReadWriteEngine(EngineConfig config) {
        // Load NRTReplicationEngine for primaries on CCR Follower as well.
        if (config.isReadOnlyReplica() || config.isReadOnlyPrimary()) {
            return new NRTReplicationEngine(config);
        }
        return new InternalEngine(config);
    }
}
