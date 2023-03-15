/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.xreplication.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.common.settings.Settings;

import java.util.List;
import java.util.stream.Collectors;

import static org.opensearch.transport.SniffConnectionStrategy.REMOTE_CLUSTER_SEEDS;

public class ReplicationHelper {
    private static final Logger logger = LogManager.getLogger(ReplicationHelper.class);
    public static List<String> getAllClusterAliases(Settings settings) {
        List<String> aliases = REMOTE_CLUSTER_SEEDS.getAllConcreteSettings(settings).map(s -> s.getKey()).collect(Collectors.toList());
        logger.info("[ankikala]: New Follower aliases {}", aliases.toString());
        getAllClusterAliases3(settings);
        return aliases;
    }

    private static void getAllClusterAliases3(Settings settings) {
        logger.info("[ankikala] manual stream", settings.keySet().stream().filter(s -> s.startsWith("cluster.remote")).collect(Collectors.toList()));
        logger.info("settings.keySet(): {}", settings.keySet().toString());
    }

    public static List<String> getAllClusterAliases2(Settings settings) {
        List<String> aliases = REMOTE_CLUSTER_SEEDS.getNamespaces(settings).stream().collect(Collectors.toList());
        logger.info("[ankikala]: Follower aliases {}", aliases.toString());
        return aliases;
    }
}
