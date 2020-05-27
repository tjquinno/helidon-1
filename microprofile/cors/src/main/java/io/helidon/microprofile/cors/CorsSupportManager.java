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
package io.helidon.microprofile.cors;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;

import io.helidon.config.Config;
import io.helidon.webserver.PathMatcher;
import io.helidon.webserver.cors.CrossOriginConfig;

/**
 * Manages the {@code CorsSupport} instances used for resources that are annotated with {@code CrossOrigin}
 * or set-up for CORS in configuration.
 *
 * Given a resource {@code Path} annotation, the manager returns the suitable
 * {@code CorsSupport} instance for that resource, creating it if necessary.
 *
 */
class CorsSupportManager {

    private static final Logger LOGGER = Logger.getLogger(CorsSupportManager.class.getName());

    // From resource path to corresponding CorsSupport object. This is declared static because Jersey instantiates
    // the filter multiple times which means this manager is instantiated multiple times.
    private static final MatchingMap<String, Optional<CorsSupportMp>> resources = MatchingMap.create();

    // based on the "cors" config node from the application's config
    private final Config corsConfig;
    private final boolean corsConfigIsPresent;

    private CorsSupportManager(Config corsConfig) {
        this.corsConfig = corsConfig;
        corsConfigIsPresent = corsConfig.exists();
    }

    static CorsSupportManager create(Config config) {
        return new CorsSupportManager(config);
    }

    /**
     * Retrieves the {@code Optional<CorsSupportMp>} associated with the first matcher that accepts
     * the specified request path.
     *
     * @param resourceClass
     * @param requestPath
     * @return
     */
    Optional<CorsSupportMp> corsSupport(Class<?> resourceClass, String requestPath) {
        if (resourceClass == null) {
            return Optional.empty();
        }

        requestPath = requestPath.startsWith("/") ? requestPath : "/" + requestPath;

        Optional<CorsSupportMp> result = resources.computeIfAbsent(requestPath,
                    k -> createCorsSupportsForResource(k, resourceClass));
        return result;
    }

    /**
     * Accumulates one or more lines of log output, possibly at varying levels, to be logged as a single
     * log record.
     */
    private static class BatchLogger {
        private final StringBuilder sb = new StringBuilder();
        private final Level effectiveLevel;

        BatchLogger(Level level, Supplier<String> initialMessageSupplier) {
            effectiveLevel = level;
            log(level, initialMessageSupplier);
        }

        BatchLogger log(Level level, Supplier<String> messageSupplier) {
            if (LOGGER.isLoggable(level)) {
                sb.append(messageSupplier.get());
            }
            return this;
        }

        void write() {
            if (sb.length() > 0) {
                LOGGER.log(effectiveLevel, () -> sb.toString());
            }
        }
    }

    private MatchingMap.PatternMatcherValue<String, Optional<CorsSupportMp>> createCorsSupportsForResource(
            String resourcePath, Class<?> resourceClass) {

        BatchLogger batchLogger = new BatchLogger(Level.FINE, () ->
                String.format("Creating CorsSupportMp for resource path %s from class %s%n",
                resourcePath, resourceClass.getName()));

        AtomicBoolean resourceBuilderHasCrossOriginFromAnnotation = new AtomicBoolean();
        String path = null;

        CorsSupportMp.Builder resourceBuilder = CorsSupportMp.builder();

        // Always start with potentially overriding config, if it is present.
        if (corsConfigIsPresent) {
            resourceBuilder.mappedConfig(corsConfig);
        }

        for (Method m : resourceClass.getDeclaredMethods()) {
            batchLogger.log(Level.FINER, () -> String.format("Checking method %s%n", m.getName()));
            if (m.getDeclaredAnnotation(OPTIONS.class) != null) {
                batchLogger.log(Level.FINER, () -> String.format("Has @OPTIONS%n", resourcePath));
                path = pathForMethod(resourceClass, m);
                PathMatcher.Result matchingResult = PathMatcher.create(path).match(resourcePath);
                if (matchingResult.matches()) {
                    for (CrossOrigin crossOriginAnnotation : m.getAnnotationsByType(CrossOrigin.class)) {
                        batchLogger.log(Level.FINER, () -> String.format("Processing @CrossOrigin %s%n",
                                crossOriginAnnotation.toString()));
                        resourceBuilder.addCrossOrigin(annotationToConfig(crossOriginAnnotation));
                        resourceBuilderHasCrossOriginFromAnnotation.set(true);
                    }
                    break;
                }
            }
        }

        if (LOGGER.isLoggable(Level.FINER)) {
            if (!resourceBuilderHasCrossOriginFromAnnotation.get()) {
                if (corsConfigIsPresent) {
                    batchLogger.log(Level.FINER, () -> String.format(
                            "Found no @CrossOrigin annotation but using CORS configuration%n"));
                }
            };
        }

        batchLogger.log(Level.FINE, () -> resourceBuilderHasCrossOriginFromAnnotation.get() || corsConfigIsPresent
                    ? String.format("Returning new CorsSupport instance for path %s%n", resourcePath)
                    : String.format("No CorsSupport instance created for path %s%n", resourcePath));

        batchLogger.write();

        return MatchingMap.PatternMatcherValue.create(path,
                        resourceBuilderHasCrossOriginFromAnnotation.get() || corsConfigIsPresent
                                ? Optional.of(resourceBuilder.build())
                                : Optional.empty());
    }

    private static String pathForMethod(Class<?> resourceClass, Method method) {
        Path classAnnotation = resourceClass.getDeclaredAnnotation(Path.class);
        Path methodAnnotation = method.getDeclaredAnnotation(Path.class);

        return (classAnnotation == null ? "" : classAnnotation.value())
                + (methodAnnotation == null ? "" : methodAnnotation.value());
    }

    private static CrossOriginConfig annotationToConfig(CrossOrigin crossOrigin) {
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
