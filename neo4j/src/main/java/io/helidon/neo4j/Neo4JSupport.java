/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.helidon.neo4j;

import io.helidon.config.Config;
import io.helidon.metrics.RegistryFactory;
import io.helidon.webserver.Routing;
import io.helidon.webserver.Service;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.neo4j.driver.ConnectionPoolMetrics;
import org.neo4j.driver.Driver;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Map.entry;

public class Neo4JSupport implements Service {

    private static final boolean isMetricsPresent = checkForMetrics();
    private static boolean checkForMetrics() {
        try {
            Class.forName("io.helidon.metrics.RegistryFactory");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private Neo4JSupport(Builder builder) {
        if (isMetricsPresent) {
            new Neo4JMetricsHelper().initNeo4JMetrics();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Neo4JSupport create() {
        return builder().build();
    }

    public static Neo4JSupport create(Config config) {
        return builder().config(config).build();
    }

    @Override
    public void update(Routing.Rules rules) {
        // If Neo4J support in Helidon adds no new endpoints,
        // then we do not need to do anything here.
    }

    public static class Builder implements io.helidon.common.Builder<Neo4JSupport> {

        private Config config; //

        private Builder() {

        }

        @Override
        public Neo4JSupport build() {
            return new Neo4JSupport(this);
        }

        public Builder config(Config config) {
            // harvest Neo4J config information from Helidon config.
            this.config = config;
            return this;
        }
    }
}
