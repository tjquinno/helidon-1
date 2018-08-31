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

import io.helidon.common.reactive.Flow;
import io.helidon.common.rest.ResponseChunk;

/**
 * TODO javadoc.
 */
public interface RequestBuilder {
    RequestBuilder property(String propertyName, Object propertyValue);

    RequestBuilder header(String header, String... values);

    RequestBuilder method(String method);

    RequestBuilder queryParam(String name, String... values);

    /**
     * Override client proxy configuration
     *
     * @param proxy
     * @return
     */
    RequestBuilder proxy(Proxy proxy);

    CompletionStage<ClientResponse> send();

    <E> CompletionStage<ClientResponse> send(E entity);

    CompletionStage<ClientResponse> send(Flow.Publisher<ResponseChunk> content);
}
