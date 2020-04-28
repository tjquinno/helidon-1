/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
 *
 */
package io.helidon.microprofile.server;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.FeatureContext;

import io.helidon.config.Config;
import org.glassfish.jersey.internal.spi.AutoDiscoverable;
import org.glassfish.jersey.server.ServerProperties;

@Priority(AutoDiscoverable.DEFAULT_PRIORITY - 100)
@ConstrainedTo(RuntimeType.SERVER)
public class WadlChecker implements AutoDiscoverable {

    public static final String ALLOW_JERSEY_WADL_SUPPORT_KEY = "server.allow-jersey-wadl-support";

    private static final Logger LOGGER = Logger.getLogger(WadlChecker.class.getName());

    @Override
    public void configure(FeatureContext context) {
        context.register(WadlChecker.class);
        checkWadl(context);
    }
    private void checkWadl(FeatureContext context) {
        if (Config.create().get(ALLOW_JERSEY_WADL_SUPPORT_KEY).asBoolean().orElse(false)) {
            LOGGER.log(Level.CONFIG, "User configuration allows Jersey WADL support for default OPTIONS implementation");
            return;
        }
        try {
            Class.forName("com.sun.xml.bind.v2.ContextFactory");
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.WARNING, "Disabling Jersey WADL support: com.sun.xml.bind.v2.ContextFactory not found");
            context.property(ServerProperties.WADL_FEATURE_DISABLE, "true");
        }
    }
}