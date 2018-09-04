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

import io.helidon.common.http.Content;
import io.helidon.common.http.Http;

/**
 * Response from a server that was created for our request.
 * An instance is created only if we receive a real response
 * over HTTP. For other cases (e.g. a timeout) the flow ends
 * exceptionally.
 */
public interface ClientResponse {
    /**
     * Status of this response.
     *
     * @return HTTP status
     */
    Http.ResponseStatus status();

    /**
     * Content to access entity.
     * The content is never null, though it may be empty (e.g.
     * for HTTP PUT method, we do not get any entity back).
     *
     * @return content
     */
    Content content();

    /**
     * Headers of the HTTP response.
     *
     * @return headers that were present in the response from server
     */
    ClientResponseHeaders headers();
}
