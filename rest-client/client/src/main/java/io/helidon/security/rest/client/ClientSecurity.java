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
package io.helidon.security.rest.client;

import java.util.UUID;

import io.helidon.rest.client.ClientService;
import io.helidon.rest.client.ClientServiceRequest;
import io.helidon.security.EndpointConfig;
import io.helidon.security.Security;
import io.helidon.security.SecurityContext;
import io.helidon.security.SecurityEnvironment;

/**
 * TODO javadoc.
 */
public class ClientSecurity implements ClientService {
    public static final String PROVIDER_NAME = "io.helidon.security.rest.client.security.providerName";

    private Security security;

    public static ClientSecurity create() {
        return null;
    }

    public static ClientSecurity create(Security security) {
        return null;
    }

    @Override
    public void apply(ClientServiceRequest request) {
        //todo security either from request or from field
        // context either from request or create a new one
        SecurityContext context = request.context().get(SecurityContext.class).orElseGet(() -> createContext(request));
    }

    private SecurityContext createContext(ClientServiceRequest request) {
        return security.contextBuilder(UUID.randomUUID().toString())
                .endpointConfig(EndpointConfig.builder()
                                        .build())
                .env(SecurityEnvironment.builder()
                             .path(request.path().toString())
                             //TODO everything else
                             .build())
                .tracingTracer()
                .tracingSpan()
                .build();
    }
}
