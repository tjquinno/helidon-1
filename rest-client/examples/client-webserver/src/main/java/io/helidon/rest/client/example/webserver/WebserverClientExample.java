/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
 */
package io.helidon.rest.client.example.webserver;

import io.helidon.common.OptionalHelper;
import io.helidon.common.rest.Http;
import io.helidon.config.Config;
import io.helidon.metrics.rest.client.ClientMetrics;
import io.helidon.rest.client.ClientException;
import io.helidon.rest.client.ClientResponse;
import io.helidon.rest.client.Proxy;
import io.helidon.rest.client.RestClient;
import io.helidon.security.rest.client.ClientSecurity;
import io.helidon.tracing.rest.client.ClientTracing;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.WebServer;

/**
 * TODO javadoc.
 */
public class WebserverClientExample {
    private static RestClient restClient;

    public static void main(String[] args) {
        Config config = Config.create();

        restClient = RestClient.builder()
                // default configuration of client metrics
                // REQUIRES: metrics registry configured on request context (injected by MetricsSupport)
                .register(ClientMetrics.create())
                // default configuration of tracing
                // REQUIRES: span context configured on request context (injected by future TracingSupport)
                .register(ClientTracing.create())
                // default configuration of client security - invokes outbound provider(s) and updates headers
                // REQUIRES: security and security context configured on request context (injected by WebSecurity)
                .register(ClientSecurity.create())
                .proxy(Proxy.create(config))
                .build();

        WebServer server = WebServer.create(Routing.builder()
                                                    .get("/hello", WebserverClientExample::hello)
                                                    .post("/put", WebserverClientExample::put)
                                                    .build());

        server.start()
                .thenAccept(webServer -> System.out.println("Webserver started on http://localhost:" + server.port()));
    }

    private static void put(ServerRequest req, ServerResponse res) {
        restClient.put(req.context(), "http://www.google.com")
                .send(req.content())
                .thenCompose(res::send)
                .thenAccept(aResponse -> {
                })
                .exceptionally(throwable -> handleException(res, throwable));
    }

    // TODO how about this use case
    // TODO requires that server depends on client
    // TODO what about - we need to change a bit (header, cookie etc.)
    // e.g. to implement a side car
    private static void proxyResponse(ServerRequest req, ServerResponse res) {
        restClient.get(req.context(), "http://lll")
                .send()
                .thenAccept(clientResponse -> {
                    res.headers().add("CUSTOM_RESPONSE", "HEADER");
                    res.proxy(clientResponse);
                });
    }

    private static void proxyRequestAndResponse(ServerRequest req, ServerResponse res) {
        restClient.get(req.context(), "http://LLLdaůsf")
                .queryParams(req.queryParams())
                .headers(req.headers())
                .send(req.content())
                .thenAccept(res::proxy)
                .exceptionally(throwable -> handleException(res, throwable));

    }

    private static Void handleException(ServerResponse res, Throwable throwable) {
        if (throwable instanceof ClientException) {
            ClientException e = (ClientException) throwable;
            OptionalHelper.from(e.response())
                    .ifPresentOrElse(clientResponse -> {
                        res.status(clientResponse.status());
                        res.send(clientResponse.content());
                    }, () -> {
                        res.status(Http.Status.INTERNAL_SERVER_ERROR_500);
                        res.send();
                    });

        } else {
            // TODO log and send entity with stacktrace if in debug mode
            res.status(Http.Status.INTERNAL_SERVER_ERROR_500);
            res.send();
        }

        return null;
    }

    private static void hello(ServerRequest req, ServerResponse res) {
        restClient.get(req.context(), "http://www.google.com")
                .send()
                .thenApply(ClientResponse::content)
                .thenAccept(res::send)
                .exceptionally(throwable -> handleException(res, throwable));
    }

}
