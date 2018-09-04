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

import io.helidon.common.http.Headers;
import io.helidon.common.http.MediaType;

/**
 * Headers that can be modified (until request is sent) for
 * outbound request.
 */
public interface ClientRequestHeaders extends Headers {
    /**
     * Set a header. This will override the existing values
     * of the header (if any).
     *
     * @param name   header name
     * @param values header value(s)
     */
    void setHeader(String name, String... values);

    /**
     * Remove a header if set.
     *
     * @param name header name
     */
    void unsetHeader(String name);

    /**
     * Add a cookie to the request.
     *
     * @param name  cookie name
     * @param value cookie value
     */
    void addCookie(String name, String value);

    /**
     * Set a content type. This method is optional if you use
     * a writer for a specific type.
     * If the content type is explicitly defined, writer will NOT override it.
     * TODO reference the method that supports this
     *
     * @param contentType content type of the request
     */
    void contentType(MediaType contentType);

    /**
     * Set a content length. This method is optional.
     * Use only when you know the exact length of entity in bytes.
     *
     * @param length content length of entity
     */
    void contentLength(long length);

    /**
     * Add accepted {@link MediaType}. Supports quality factor and wildcards.
     * Ordered by invocation order.
     *
     * @param mediaType media type to accept, with optional quality factor
     */
    void addAccept(MediaType mediaType);

    // TODO add the rest of known request headers
}
