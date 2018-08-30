/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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
 */
package io.helidon.rest.client.example.basic;

import java.util.concurrent.CompletionStage;

import io.helidon.common.rest.ContextualRegistry;
import io.helidon.config.Config;
import io.helidon.metrics.RegistryFactory;
import io.helidon.rest.client.ClientResponse;
import io.helidon.rest.client.RestClient;
import io.helidon.security.Security;
import io.helidon.security.rest.client.ClientWebSecurity;

import io.opentracing.SpanContext;
import org.eclipse.microprofile.metrics.MetricRegistry;

/**
 * A standalone REST client.
 */
public class StandaloneClientExample {
    public static void main(String[] args) {
        Config config = Config.create();
        Security security = Security.fromConfig(config);
        RegistryFactory seMetricFactory = RegistryFactory.createSeFactory(config);

        ContextualRegistry registry = ContextualRegistry.create();
        // register the parent span context
        registry.register(SpanContext.class, null);
        // register the metrics registry factory
        registry.register(seMetricFactory);
        // and the application registry
        registry.register(seMetricFactory.getRegistry(MetricRegistry.Type.APPLICATION));

        RestClient client = RestClient.builder(registry)
                .register(ClientWebSecurity.from(config, security))
                .register(ClientTracing.from(config))
                .register(ClientMetrics.from(config))
                .build();

        // put
        CompletionStage<ClientResponse> response = client.put("http://www.google.com")
                // override tracing span
                .property(ClientTracing.SPAN_NAME, "myspan")
                .property(ClientMetrics.ENDPOINT_NAME, "aServiceName")
                .property(ClientWebSecurity.PROVIDER_NAME, "http-basic-auth")
                .property("io.helidon.security.outbound.username", "aUser")
                .header("MY_HEADER", "Value")
                .send("Entity content as the correct type");

        response.thenApply(ClientResponse::status)
                .thenAccept(System.out::println)
                .toCompletableFuture()
                .join();

        // and now get
        client.get("http://www.google.com")
                .send()
                .thenApply(ClientResponse::content)
                .thenCompose(content -> content.as(String.class))
                .thenAccept(System.out::println)
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    return null;
                });
    }
}
