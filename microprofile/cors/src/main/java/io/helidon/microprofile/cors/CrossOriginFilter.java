/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates.
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

package io.helidon.microprofile.cors;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Priority;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import io.helidon.common.HelidonFeatures;
import io.helidon.common.HelidonFlavor;
import io.helidon.config.Config;
import io.helidon.microprofile.cors.CorsSupportMp.RequestAdapterMp;
import io.helidon.microprofile.cors.CorsSupportMp.ResponseAdapterMp;
import io.helidon.webserver.cors.CorsSupport;
import io.helidon.webserver.cors.CrossOriginConfig;

import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Class CrossOriginFilter.
 */
@Priority(Priorities.HEADER_DECORATOR)
class CrossOriginFilter implements ContainerRequestFilter, ContainerResponseFilter {

    /**
     * Key used for retrieving CORS-related configuration from MP configuration.
     */
    public static final String CORS_CONFIG_KEY = "cors";

    static {
        HelidonFeatures.register(HelidonFlavor.MP, "CORS");
    }

    @Context
    private ResourceInfo resourceInfo;

    @Context
    Application application;

    @Context
    UriInfo uriInfo;

    private final CorsSupportMp cors;

    private final CorsSupportManager manager = new CorsSupportManager();

    CrossOriginFilter() {
        Config config = (Config) ConfigProvider.getConfig();

        CorsSupportMp.Builder corsBuilder = CorsSupportMp.builder();
        config.get(CORS_CONFIG_KEY).ifExists(corsBuilder::mappedConfig);
        cors = corsBuilder
                .secondaryLookupSupplier(this::crossOriginFromAnnotationSupplier)
                .build();

    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        CorsSupportMp cs = manager.get(application, resourceInfo, uriInfo);
        Optional<Response> response = cs.processRequest(new RequestAdapterMp(requestContext), new ResponseAdapterMp());
        response.ifPresent(requestContext::abortWith);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        CorsSupportMp cs = manager.get(application, resourceInfo, uriInfo);

        cs.prepareResponse(new RequestAdapterMp(requestContext), new ResponseAdapterMp(responseContext));
    }

    Optional<CrossOriginConfig> crossOriginFromAnnotationSupplier() {

        // If not found, inspect resource matched
        Method resourceMethod = resourceInfo.getResourceMethod();
        Class<?> resourceClass = resourceInfo.getResourceClass();

        // Not available if matching failed and error response is returned
        if (resourceClass == null || resourceMethod == null) {
            return Optional.empty();
        }

        CrossOrigin corsAnnot;
        OPTIONS optsAnnot = resourceMethod.getAnnotation(OPTIONS.class);
        Path pathAnnot = resourceMethod.getAnnotation(Path.class);
        if (optsAnnot != null) {
            corsAnnot = resourceMethod.getAnnotation(CrossOrigin.class);
        } else {
            Optional<Method> optionsMethod = Arrays.stream(resourceClass.getDeclaredMethods())
                    .filter(m -> {
                        OPTIONS optsAnnot2 = m.getAnnotation(OPTIONS.class);
                        Path pathAnnot2 = m.getAnnotation(Path.class);
                        if (optsAnnot2 != null) {
                            if (pathAnnot != null) {
                                return pathAnnot2 != null && pathAnnot.value()
                                        .equals(pathAnnot2.value());
                            }
                            return pathAnnot2 == null;
                        }
                        return false;
                    })
                    .findFirst();
            corsAnnot = optionsMethod.map(m -> m.getAnnotation(CrossOrigin.class))
                    .orElse(null);
        }
        return Optional.ofNullable(corsAnnot == null ? null : annotationToConfig(corsAnnot));
    }

    static CrossOriginConfig annotationToConfig(CrossOrigin crossOrigin) {
        return CrossOriginConfig.builder()
            .allowOrigins(crossOrigin.value())
            .allowHeaders(crossOrigin.allowHeaders())
            .exposeHeaders(crossOrigin.exposeHeaders())
            .allowMethods(crossOrigin.allowMethods())
            .allowCredentials(crossOrigin.allowCredentials())
            .maxAgeSeconds(crossOrigin.maxAge())
            .build();
    }
}
