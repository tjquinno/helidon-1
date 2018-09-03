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
package io.helidon.metrics.rest.client;

import io.helidon.metrics.RegistryFactory;
import io.helidon.rest.client.ClientService;
import io.helidon.rest.client.ClientServiceRequest;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;

/**
 * Creates the following metrics:
 * <ul>
 * <li>A {@link org.eclipse.microprofile.metrics.Timer} named io.helidon.rest.client.timer.${hostname} where hostname is replaced
 * with host provided in outbound URI. It can be overridden with {@link #ENDPOINT_NAME} property in request.
 * The name of the timer can be overridden with {@link #TIMER_NAME} property in request.</li>
 * <li>A {@link org.eclipse.microprofile.metrics.Counter} named io.helidon.rest.client.counter.${hostname} where hostname
 * is replaced with host provided in outbound URI. It can be overridden with {@link #ENDPOINT_NAME} property in request.
 * The name of the counter can be overridden with {@link #COUNTER_NAME} property in request. The counter is counting the number
 * of in progress requests.</li>
 * </ul>
 */
public class ClientMetrics implements ClientService {
    // TODO maybe handle error counts, only time success etc.
    public static final String COUNTER_NAME = "io.helidon.rest.client.metrics.counterName";
    public static final String TIMER_NAME = "io.helidon.rest.client.metrics.meterName";
    public static final String ENDPOINT_NAME = "io.helidon.rest.client.metrics.endpointName";

    public static ClientMetrics create() {
        return null;
    }

    public static ClientMetrics create(RegistryFactory factory, MetricRegistry registry) {
        return null;
    }

    @Override
    public void apply(ClientServiceRequest request) {
        Counter counter = getCounter(request);
        Timer timer = getTimer(request);

        counter.inc();
        Timer.Context time = timer.time();

        request.whenComplete()
                .thenAccept(clientServiceRequest -> {
                    counter.dec();
                    time.close();
                });
    }

    private Counter getCounter(ClientServiceRequest request) {
        String name = request.properties().first(COUNTER_NAME)
                .orElseGet(() -> "io.helidon.rest.client.counter." + request.uri().getHost());
        // todo if registry not in request context and not configured for this instance, fail?
        //request.context().get(MetricRegistry.class).orElse(defaultRegistry)
        return null;
    }

    private Timer getTimer(ClientServiceRequest request) {
        return null;
    }
}
