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
package io.helidon.examples.metrics.micrometer;

import io.helidon.media.jsonp.JsonpSupport;
import io.helidon.webclient.WebClient;
import io.helidon.webserver.WebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class MainTest {

    private static WebServer webServer;
    private static WebClient webClient;

    private static final JsonBuilderFactory JSON_BF = Json.createBuilderFactory(Collections.emptyMap());
    private static final JsonObject TEST_JSON_OBJECT;

    static {
        TEST_JSON_OBJECT = JSON_BF.createObjectBuilder()
                .add("greeting", "Hola")
                .build();
    }

    @BeforeAll
    public static void startTheServer() throws Exception {
        webServer = Main.startServer();

        long timeout = 2000; // 2 seconds should be enough to start the server
        long now = System.currentTimeMillis();

        while (!webServer.isRunning()) {
            Thread.sleep(100);
            if ((System.currentTimeMillis() - now) > timeout) {
                Assertions.fail("Failed to start webserver");
            }
        }

        webClient = WebClient.builder()
                .baseUri("http://localhost:" + webServer.port())
                .addMediaSupport(JsonpSupport.create())
                .build();
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
    public void testHelloWorld() throws Exception {
        webClient.get()
                .path("/greet")
                .request(JsonObject.class)
                .thenAccept(jsonObject -> Assertions.assertEquals("Hello World!", jsonObject.getString("greeting")))
                .toCompletableFuture()
                .get();

        webClient.get()
                .path("/greet/Joe")
                .request(JsonObject.class)
                .thenAccept(jsonObject -> Assertions.assertEquals("Hello Joe!", jsonObject.getString("greeting")))
                .toCompletableFuture()
                .get();

        webClient.put()
                .path("/greet/greeting")
                .submit(TEST_JSON_OBJECT)
                .thenAccept(response -> Assertions.assertEquals(204, response.status().code()))
                .thenCompose(nothing -> webClient.get()
                        .path("/greet/Joe")
                        .request(JsonObject.class))
                .thenAccept(jsonObject -> Assertions.assertEquals("Hola Joe!", jsonObject.getString("greeting")))
                .toCompletableFuture()
                .get();

        webClient.get()
                .path("/micrometer")
                .request()
                .thenAccept(response -> {
                    Assertions.assertEquals(200, response.status()
                            .code());
                    try {
                        String output = response.content()
                                .as(String.class)
                                .get();
                        Assertions.assertTrue(output.contains("gets total 3.0"), "Unable to find expected " +
                                "result 3.0"); // 3 gets; the put is not tallied in the Counter
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }

                    response.close();
                });
    }
}