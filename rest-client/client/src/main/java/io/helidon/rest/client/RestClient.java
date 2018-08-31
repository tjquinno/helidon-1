/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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
 */
package io.helidon.rest.client;

import io.helidon.common.rest.ContextualRegistry;

/**
 * TODO javadoc.
 */
public interface RestClient {
    static RestClient create() {
        return builder().build();
    }

    static Builder builder() {
        return builder(ContextualRegistry.create());
    }

    static Builder builder(ContextualRegistry registry) {
        return new Builder(registry);
    }

    RequestBuilder put(String url);

    RequestBuilder get(String url);

    RequestBuilder request(String url);


    final class Builder implements io.helidon.common.Builder<RestClient> {
        private final ContextualRegistry registry;

        private Builder(ContextualRegistry registry) {

            this.registry = registry;
        }

        @Override
        public RestClient build() {
            return new RestClientImpl(this);
        }

        public Builder register(ClientService service) {
            return this;
        }
    }
}
