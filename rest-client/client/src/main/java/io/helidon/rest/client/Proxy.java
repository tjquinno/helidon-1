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

import java.net.URI;

import io.helidon.config.Config;

/**
 * A definition of a proxy server to use for outgoing requests.
 */
public interface Proxy {
    Proxy NO_PROXY = builder().build();

    /**
     * Fluent API builder for new instances.
     *
     * @return a new builder
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * A Proxy instance that does not proxy requests.
     *
     * @return a new instance with no proxy definition
     */
    static Proxy noProxy() {
        return NO_PROXY;
    }

    /**
     * Create a new proxy instance from configuration.
     * {@code
     * proxy:
     *   http:
     *     uri: https://www.example.org
     *   https:
     *     uri: https://www.example.org
     *   no-proxy: ["*.example.org", "localhost"]
     * }
     *
     * @param config configuration, should be located on a key that has proxy as a subkey
     * @return
     */
    static Proxy create(Config config) {
        return null;
    }

    /**
     * Create from system properties.
     *
     * @return a proxy instance configured based on this system settings
     */
    static Proxy create() {
        return null;
    }

    /**
     * Fluent API builder for {@link Proxy}.
     */
    class Builder implements io.helidon.common.Builder<Proxy> {
        @Override
        public Proxy build() {
            return null;
        }

        /**
         * Configure http proxy URI.
         *
         * @param proxyUri
         * @return
         */
        public Builder http(URI proxyUri) {
            return this;
        }

        /**
         * Configure https proxy URI
         * @param proxyUri
         * @return
         */
        public Builder https(URI proxyUri) {
            return this;
        }

        /**
         * Configure a host pattern that is not going through a proxy (usign glob - e.g. *.example.org).
         *
         * @param pattern
         * @return
         */
        public Builder addNoProxy(String pattern) {
            return this;
        }
    }
}
