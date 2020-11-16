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
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;

public class Neo4JSupport implements Service {

    private static final Metadata WRAPPER_COUNTER_METADATA = Metadata.builder()
            .withType(MetricType.COUNTER)
            .notReusable()
            .withName("TBD")
            .build();

    private Neo4JSupport(Builder builder) {
        initNeo4JMetrics(builder.config);
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

    private static void initNeo4JMetrics(Config config) {
        // I am assuming for the moment that VENDOR is the correct registry to use.
        MetricRegistry neo4JMetricRegistry = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.VENDOR);

        // Use better names.
        Metadata metadata1 = Metadata.builder(WRAPPER_COUNTER_METADATA)
                .withName("Neo4J-processing")
                .build();
        Metadata metadata2 = Metadata.builder(WRAPPER_COUNTER_METADATA)
                .withName("Neo4J-processed")
                .build();

        neo4JMetricRegistry.register(metadata1, new Neo4JCounterWrapper(null));
        neo4JMetricRegistry.register(metadata2, new Neo4JCounterWrapper(null));
    }

    private static class Neo4JCounterWrapper implements Counter {

        private final Object driver;
        private int demoValue = 0;

        private Neo4JCounterWrapper(Object driver) {
            this.driver= driver;
        }

        @Override
        public void inc() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void inc(long n) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getCount() {
            // Use driver to fetch actual value.
            return demoValue +=2 ;
        }
    }
}
