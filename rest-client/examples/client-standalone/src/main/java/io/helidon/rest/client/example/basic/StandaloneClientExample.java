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

import java.net.URI;
import java.util.concurrent.CompletionStage;

import io.helidon.config.Config;
import io.helidon.metrics.RegistryFactory;
import io.helidon.metrics.rest.client.ClientMetrics;
import io.helidon.rest.client.ClientResponse;
import io.helidon.rest.client.Proxy;
import io.helidon.rest.client.RestClient;
import io.helidon.security.Security;
import io.helidon.security.rest.client.ClientSecurity;
import io.helidon.tracing.rest.client.ClientTracing;

import io.opentracing.SpanContext;
import org.eclipse.microprofile.metrics.MetricRegistry;

/**
 * A standalone REST client.
 */
public class StandaloneClientExample {
    static void simpleExample() throws Exception {
        // read content and write it ot standard output
        try (RestClient client = RestClient.create()) {
            client.get("http://www.test.com")
                    .send()
                    .thenApply(ClientResponse::content)
                    .thenCompose(content -> content.as(String.class))
                    .thenAccept(System.out::println);
        }
    }

    // todo how to handle entity processors (e.g. how to add JSON-P, JSON-B support)
    public static void main(String[] args) {
        /*
         * Prepare helidon stuff
         */
        Config config = Config.create();
        Security security = Security.fromConfig(config);
        RegistryFactory seMetricFactory = RegistryFactory.createSeFactory(config);


        /*
         * Client must be thread safe (basically a pre-configured container)
         */
        RestClient client = RestClient.builder()
                // default configuration of client metrics
                // REQUIRES: metrics registry configured on request context (injected by MetricsSupport)
                .register(ClientMetrics.create(seMetricFactory, seMetricFactory.getRegistry(MetricRegistry.Type.APPLICATION)))
                // default configuration of tracing
                // REQUIRES: span context configured on request context (injected by future TracingSupport)
                .register(ClientTracing.create())
                // default configuration of client security - invokes outbound provider(s) and updates headers
                // REQUIRES: security and security context configured on request context (injected by WebSecurity)
                .register(ClientSecurity.create(security))
                .proxy(Proxy.builder()
                               .http(URI.create("http://www-proxy.uk.oracle.com"))
                               .https(URI.create("https://www-proxy.uk.oracle.com"))
                               .addNoProxy("localhost")
                               .addNoProxy("*.oracle.com")
                               .build())
                .build();

        SpanContext spanContext = null;

        /*
         * Each request is created using a builder like fluent api
         */
        CompletionStage<ClientResponse> response = client.put("http://www.google.com")
                // parent span
                .property(ClientTracing.PARENT_SPAN, spanContext)
                // override tracing span
                .property(ClientTracing.SPAN_NAME, "myspan")
                // override metric name
                .property(ClientMetrics.ENDPOINT_NAME, "aServiceName")
                .property(ClientSecurity.PROVIDER_NAME, "http-basic-auth")
                // override security
                .property("io.helidon.security.outbound.username", "aUser")
                // add custom header
                .header("MY_HEADER", "Value")
                // override proxy configuration of client
                .proxy(Proxy.noProxy())
                // send entity (may be a publisher of chunks)
                // should support forms
                .send("Entity content as the correct type");

        response.thenApply(ClientResponse::status)
                .thenAccept(System.out::println)
                .toCompletableFuture()
                .join();

        // and now get
        client.get("http://www.google.com")
                // send as the common name for operation that ends construction of request
                // and send it over the network
                .send()
                // get content (probably should throw an exception if content not available (e.g. not successful status)
                .thenApply(ClientResponse::content)
                // get entity content as a type - returns a completion stage (completed when all the chunks are read)
                .thenCompose(content -> content.as(String.class))
                // print it out!
                .thenAccept(System.out::println)
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    return null;
                });

        // custom
        client.request("http://s.c.d")
                .method("CUSTOM")
                .queryParam("a", "b", "c")
                .send();

    }
}
