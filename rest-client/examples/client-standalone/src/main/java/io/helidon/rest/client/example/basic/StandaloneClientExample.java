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

import io.helidon.common.rest.ContextualRegistry;
import io.helidon.config.Config;
import io.helidon.metrics.RegistryFactory;
import io.helidon.rest.client.ClientResponse;
import io.helidon.rest.client.Proxy;
import io.helidon.rest.client.RestClient;
import io.helidon.security.Security;
import io.helidon.security.rest.client.ClientWebSecurity;

import io.opentracing.SpanContext;
import org.eclipse.microprofile.metrics.MetricRegistry;

/**
 * A standalone REST client.
 */
public class StandaloneClientExample {
    // todo should we handle support for fault tolerance?

    public static void main(String[] args) {
        /*
         * Prepare helidon stuff
         */
        Config config = Config.create();
        Security security = Security.fromConfig(config);
        RegistryFactory seMetricFactory = RegistryFactory.createSeFactory(config);

        /*
         * Registry will have to be configured with some stuff when using standalone.
         * Maybe move this to explicit configuration on client builder to make is
         * nicer for users?
         */

        // TODO we need to create a client instance outside of scope of server request
        // TODO the registry is available within the scope of server request - so it should
        // TODO be used to configure a single client request rather than the client instance
        // TODO so how do we get the security instance etc. to configure a new client?
        // TODO we may need to make the client instance lightweight - but that would cause
        // TODO probably perf. issues as we could not manage resources
        ContextualRegistry registry = ContextualRegistry.create();
        // register the parent span context
        registry.register(SpanContext.class, null);
        // register the metrics registry factory
        registry.register(seMetricFactory);
        // and the application registry
        registry.register(seMetricFactory.getRegistry(MetricRegistry.Type.APPLICATION));

        /*
         * Client must be thread safe (basically a pre-configured container)
         */
        RestClient client = RestClient.builder()
                .register(ClientWebSecurity.from(config, security))
                .register(ClientTracing.from(config))
                .register(ClientMetrics.from(config))
                .proxy(Proxy.builder()
                               .http(URI.create("http://www-proxy.uk.oracle.com"))
                               .https(URI.create("https://www-proxy.uk.oracle.com"))
                               .addNoProxy("localhost")
                               .addNoProxy("*.oracle.com"))
                .build();

        /*
         * Each request is created using a builder like fluent api
         */
        CompletionStage<ClientResponse> response = client.put("http://www.google.com")
                // parent span
                .property(ClientTracing.PARENT_SPAM, aSpan)
                // override tracing span
                .property(ClientTracing.SPAN_NAME, "myspan")
                // override metric name
                .property(ClientMetrics.ENDPOINT_NAME, "aServiceName")
                .property(ClientWebSecurity.PROVIDER_NAME, "http-basic-auth")
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
