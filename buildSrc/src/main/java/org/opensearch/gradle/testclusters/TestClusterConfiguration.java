/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.gradle.testclusters;

import org.opensearch.gradle.FileSupplier;
import org.opensearch.gradle.PropertyNormalization;
import org.gradle.api.file.RegularFile;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.slf4j.Logger;

public interface TestClusterConfiguration {

    void setVersion(String version);

    void setVersions(List<String> version);

    void setTestDistribution(TestDistribution distribution);

    void extension(boolean extensionsEnabled);

    void plugin(Provider<RegularFile> plugin);

    void plugin(String pluginProjectPath);

    void upgradePlugin(List<Provider<RegularFile>> plugins);

    void module(Provider<RegularFile> module);

    void module(String moduleProjectPath);

    void keystore(String key, String value);

    void keystore(String key, Supplier<CharSequence> valueSupplier);

    void keystore(String key, File value);

    void keystore(String key, File value, PropertyNormalization normalization);

    void keystore(String key, FileSupplier valueSupplier);

    void keystorePassword(String password);

    void cliSetup(String binTool, CharSequence... args);

    void setting(String key, String value);

    void setting(String key, String value, PropertyNormalization normalization);

    void setting(String key, Supplier<CharSequence> valueSupplier);

    void setting(String key, Supplier<CharSequence> valueSupplier, PropertyNormalization normalization);

    void systemProperty(String key, String value);

    void systemProperty(String key, Supplier<CharSequence> valueSupplier);

    void systemProperty(String key, Supplier<CharSequence> valueSupplier, PropertyNormalization normalization);

    void environment(String key, String value);

    void environment(String key, Supplier<CharSequence> valueSupplier);

    void environment(String key, Supplier<CharSequence> valueSupplier, PropertyNormalization normalization);

    void jvmArgs(String... values);

    boolean isPreserveDataDir();

    void setPreserveDataDir(boolean preserveDataDir);

    void freeze();

    void start();

    void restart();

    void extraConfigFile(String destination, File from);

    void extraConfigFile(String destination, File from, PropertyNormalization normalization);

    void extraJarFile(File from);

    void user(Map<String, String> userSpec);

    String getHttpSocketURI();

    String getTransportPortURI();

    List<String> getAllHttpSocketURI();

    List<String> getAllTransportPortURI();

    void stop(boolean tailLogs);

    void setNameCustomization(Function<String, String> nameSupplier);

    default void waitForConditions(
        LinkedHashMap<String, Predicate<TestClusterConfiguration>> waitConditions,
        long startedAtMillis,
        long nodeUpTimeout,
        TimeUnit nodeUpTimeoutUnit,
        TestClusterConfiguration context
    ) {
        Logger logger = Logging.getLogger(TestClusterConfiguration.class);
        waitConditions.forEach((description, predicate) -> {
            long thisConditionStartedAt = System.currentTimeMillis();
            boolean conditionMet = false;
            Throwable lastException = null;
            while (System.currentTimeMillis() - startedAtMillis < TimeUnit.MILLISECONDS.convert(nodeUpTimeout, nodeUpTimeoutUnit)) {
                if (context.isProcessAlive() == false) {
                    throw new TestClustersException("process was found dead while waiting for " + description + ", " + this);
                }

                try {
                    if (predicate.test(context)) {
                        conditionMet = true;
                        break;
                    }
                } catch (TestClustersException e) {
                    throw e;
                } catch (Exception e) {
                    lastException = e;
                }
            }
            if (conditionMet == false) {
                String message = String.format(
                    "`%s` failed to wait for %s after %d %s",
                    context,
                    description,
                    nodeUpTimeout,
                    nodeUpTimeoutUnit
                );
                if (lastException == null) {
                    throw new TestClustersException(message);
                } else {
                    String extraCause = "";
                    Throwable cause = lastException;
                    while (cause != null) {
                        if (cause.getMessage() != null && cause.getMessage().isEmpty() == false) {
                            extraCause += "\n" + "  " + cause.getMessage();
                        }
                        cause = cause.getCause();
                    }
                    throw new TestClustersException(message + extraCause, lastException);
                }
            }
            logger.info("{}: {} took {} seconds", this, description, (System.currentTimeMillis() - thisConditionStartedAt) / 1000.0);
        });
    }

    default String safeName(String name) {
        return name.replaceAll("^[^a-zA-Z0-9]+", "").replaceAll("[^a-zA-Z0-9\\.]+", "-");
    }

    boolean isProcessAlive();
}
