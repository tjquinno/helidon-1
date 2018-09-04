/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package io.helidon.rest.client;

import java.util.concurrent.CompletionStage;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.Headers;
import io.helidon.common.http.Parameters;
import io.helidon.common.reactive.Flow;
import io.helidon.webserver.Handler;

/**
 * Fluent API builder that is used by {@link RestClient} to create an outgoing request.
 */
public interface RequestBuilder {
    /**
     * Add a property to be used by a {@link io.helidon.rest.client.spi.ClientService}
     *
     * @param propertyName
     * @param propertyValue
     * @return
     */
    RequestBuilder property(String propertyName, Object propertyValue);

    /**
     * Add a request header.
     *
     * @param header
     * @param values
     * @return
     *
     * @see #headers()
     */
    RequestBuilder header(String header, String... values);

    /**
     * Get a (mutable) instance of outgoing headers
     *
     * @return
     */
    ClientRequestHeaders headers();

    /**
     * Configure request method (will override existing method).
     *
     * @param method
     * @return
     */
    RequestBuilder method(String method);

    /**
     * Add a query parameter.
     * @param name
     * @param values
     *
     * @return
     */
    RequestBuilder queryParam(String name, String... values);

    /**
     * Override client proxy configuration
     *
     * @param proxy
     * @return
     */
    RequestBuilder proxy(Proxy proxy);

    /**
     * Build the request and send it to the server.
     *
     * @return a completion stage that is completed when we receive response from the server (entity may not be yet fully received)
     */
    CompletionStage<ClientResponse> send();

    /**
     * Build the request and send it with an entity to the server.
     *
     * @param entity entity of a type supported by one of TODO configured request writers
     * @param <E>
     * @return a completion stage that is completed when we receive response from the server (entity may not be yet fully received)
     */
    <E> CompletionStage<ClientResponse> send(E entity);

    /**
     * Build the request and send it with an entity to the server.
     *
     * @param content publisher of entity {@link DataChunk DataChunks}
     * @return a completion stage that is completed when we receive response from the server (entity may not be yet fully
     * received)
     */
    CompletionStage<ClientResponse> send(Flow.Publisher<DataChunk> content);

    /**
     * Configure headers. Copy all headers from supplied {@link Headers} instance.
     *
     * @param headers to copy
     * @return updated builder instance
     */
    RequestBuilder headers(Headers headers);

    /**
     * Configure query parameters.
     * Copy all query parameters from supplied {@link Parameters} instance.
     *
     * @param queryParams to copy
     * @return udpated builder instance
     */
    RequestBuilder queryParams(Parameters queryParams);

    /**
     * Register an entity handler.
     * TODO not sure whether to register entity writer and reader separately, all configure a single (or multiple) handlers
     * TODO the Handler used right now is from WebServer - and we should probably use either the same ones or similar ones
     * if this can be implemented (e.g. it would be nice to have a single JsonHandler for client and for server.
     *
     * @param handler
     * @return
     */
    RequestBuilder register(Handler handler);
}
