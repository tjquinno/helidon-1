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

import io.helidon.common.rest.ContextualRegistry;
import io.helidon.common.rest.HttpRequest;
import io.helidon.common.rest.Parameters;

/**
 * TODO javadoc.
 */
public interface ClientServiceRequest extends HttpRequest {
    ClientRequestHeaders headers();

    ContextualRegistry context();

    void next();

    /**
     * Completes when the request part of this request is done (e.g. we have sent all headers and bytes).
     *
     * @return
     */
    CompletionStage<ClientServiceRequest> whenSent();

    /**
     * Completes when the full processing of this request is done (e.g. we have received a full response).
     *
     * @return
     */
    CompletionStage<ClientServiceRequest> whenComplete();

    Parameters properties();
}
