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

import io.helidon.common.http.ContextualRegistry;
import io.helidon.common.http.HttpRequest;
import io.helidon.common.http.Parameters;

/**
 * Request to SPI {@link io.helidon.rest.client.spi.ClientService} that supports modification of the outgoing request.
 */
public interface ClientServiceRequest extends HttpRequest {
    /**
     * Configured request headers.
     *
     * @return headers (mutable)
     */
    ClientRequestHeaders headers();

    /**
     * Registry that can be used to propagate information from server (e.g. security context, tracing spans etc.).
     *
     * @return registry propagated by the user
     */
    ContextualRegistry context();

    /**
     * Completes when the request part of this request is done (e.g. we have sent all headers and bytes).
     *
     * @return completion stage that finishes when we fully send request (including entity) to server
     */
    CompletionStage<ClientServiceRequest> whenSent();

    /**
     * Completes when the full processing of this request is done (e.g. we have received a full response).
     *
     * @return completion stage that finishes when we receive and fully read response from the server
     */
    CompletionStage<ClientServiceRequest> whenComplete();

    /**
     * Properties configured by user when creating this client request.
     *
     * @return properties that were configured
     * @see RequestBuilder#property(String, Object)
     */
    Parameters properties();
}
