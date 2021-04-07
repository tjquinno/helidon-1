/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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
package io.helidon.metrics;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonBuilderFactory;

import io.helidon.common.reactive.Single;
import io.helidon.media.jsonp.JsonpSupport;
import io.helidon.webclient.WebClient;
import io.helidon.webclient.WebClientResponse;
import io.helidon.webserver.WebServer;

import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;


public class ServerTest {

    private static WebServer webServer;
    private static WebClient webClient;
    private static MetricRegistry vendorRegistry;

    private static final JsonBuilderFactory JSON_BUILDER = Json.createBuilderFactory(Collections.emptyMap());

    @BeforeAll
    public static void startTheServer() {
        webServer = Main.startServer().await();

        webClient = WebClient.builder()
                .baseUri("http://localhost:" + webServer.port())
                .addMediaSupport(JsonpSupport.create())
                .build();

        vendorRegistry = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.VENDOR);
    }

    @AfterAll
    public static void stopServer() throws Exception {
        if (webServer != null) {
            webServer.shutdown()
                    .toCompletableFuture()
                    .get(10, TimeUnit.SECONDS);
        }
    }
    @Test
    public void testTotalRequests() {
        Counter requestsCounter = vendorRegistry.counter("requests.count");
        long before = requestsCounter.getCount();
        webClient.get()
                .path("/greet")
                .request()
                .await();
        assertThat("requests.count change across one endpoint access", requestsCounter.getCount() - before, is(1L));
    }

    @Test
    public void testInflightRequests() throws InterruptedException, ExecutionException {
        ConcurrentGauge inflightCounter = vendorRegistry.concurrentGauge("requests.inflight");
        long beforeRequest = inflightCounter.getCount();
        GreetService.initSlowRequest();
        Future<Single<String>> future = Executors.newSingleThreadExecutor().submit(() ->
                webClient.get()
                    .path("/greet/slow")
                    .request(String.class) );
        GreetService.awaitSlowRequestStarted();
        long duringRequest = inflightCounter.getCount();
        String response = future.get().await();
        assertThat("Slow response", response, containsString("Slowpoke"));
        long afterRequest = inflightCounter.getCount();

        assertThat("Inflight during slow request", duringRequest, is(beforeRequest + 1));
        assertThat("Inflight after slow request completes", afterRequest, is(beforeRequest));

    }
}
