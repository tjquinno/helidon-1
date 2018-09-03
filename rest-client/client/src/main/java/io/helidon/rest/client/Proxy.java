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
 * TODO javadoc.
 */
public interface Proxy {
    static Builder builder() {
        return new Builder();
    }

    static Proxy noProxy() {
        return builder().build();
    }

    static Proxy create(Config config) {
        return null;
    }

    /**
     * Create from system props
     *
     * @return
     */
    static Proxy create() {
        return null;
    }

    class Builder implements io.helidon.common.Builder<Proxy> {
        @Override
        public Proxy build() {
            return null;
        }

        public Builder http(URI proxyUri) {
            return this;
        }

        public Builder https(URI proxyUri) {
            return this;
        }

        public Builder addNoProxy(String pattern) {
            return this;
        }
    }
}
