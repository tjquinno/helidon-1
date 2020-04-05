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
package io.helidon.cors;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import io.helidon.common.http.Http;

import static io.helidon.common.http.Http.Header.HOST;
import static io.helidon.cors.CrossOrigin.ACCESS_CONTROL_ALLOW_CREDENTIALS;
import static io.helidon.cors.CrossOrigin.ACCESS_CONTROL_ALLOW_HEADERS;
import static io.helidon.cors.CrossOrigin.ACCESS_CONTROL_ALLOW_METHODS;
import static io.helidon.cors.CrossOrigin.ACCESS_CONTROL_ALLOW_ORIGIN;
import static io.helidon.cors.CrossOrigin.ACCESS_CONTROL_EXPOSE_HEADERS;
import static io.helidon.cors.CrossOrigin.ACCESS_CONTROL_MAX_AGE;
import static io.helidon.cors.CrossOrigin.ACCESS_CONTROL_REQUEST_HEADERS;
import static io.helidon.cors.CrossOrigin.ACCESS_CONTROL_REQUEST_METHOD;
import static io.helidon.cors.CrossOrigin.ORIGIN;

/**
 * Centralizes common logic to both SE and MP CORS support.
 * <p>
 * To serve both masters, several methods here accept adapters for requests and responses. Both of these are minimal and very
 * specific to the needs of CORS support.
 * </p>
 */
public class CrossOriginHelper {

    private CrossOriginHelper() {
    }

    /**
     * Key used for retrieving CORS-related configuration.
     */
    public static final String CORS_CONFIG_KEY = "cors";

    static final String ORIGIN_DENIED = "CORS origin is denied";
    static final String ORIGIN_NOT_IN_ALLOWED_LIST = "CORS origin is not in allowed list";
    static final String METHOD_NOT_IN_ALLOWED_LIST = "CORS method is not in allowed list";
    static final String HEADERS_NOT_IN_ALLOWED_LIST = "CORS headers not in allowed list";

    /**
     * CORS-related classification of HTTP requests.
     */
    public enum RequestType {
        /**
         * A non-CORS request.
         */
        NORMAL,

        /**
         * A CORS request, either a simple one or a non-simple one already preceded by a preflight request.
         */
        CORS,

        /**
         * A CORS preflight request.
         */
        PREFLIGHT
    }

    /**
     * Minimal abstraction of an HTTP request.
     *
     * @param <T> type of the request wrapped by the adapter
     */
    public interface RequestAdapter<T> {

        /**
         *
         * @return possibly unnormalized path from the request
         */
        String path();

        /**
         * Retrieves the first value for the specified header as a String.
         *
         * @param key header name to retrieve
         * @return the first header value for the key
         */
        Optional<String> firstHeader(String key);

        /**
         * Reports whether the specified header exists.
         *
         * @param key header name to check for
         * @return whether the header exists among the request's headers
         */
        boolean headerContainsKey(String key);

        /**
         * Retrieves all header values for a given key as Strings.
         *
         * @param key header name to retrieve
         * @return header values for the header; empty list if none
         */
        List<String> allHeaders(String key);

        /**
         * Reports the method name for the request.
         *
         * @return the method name
         */
        String method();

        /**
         * Returns the request this adapter wraps.
         *
         * @return the request
         */
        T request();
    }

    /**
     * Minimal abstraction of an HTTP response.
     *
     * @param <T> the type of the response wrapped by the adapter
     */
    public interface ResponseAdapter<T> {

        /**
         * Arranges to add the specified header and value to the eventual response.
         *
         * @param key header name to add
         * @param value header value to add
         * @return the adapter
         */
        ResponseAdapter<T> header(String key, String value);

        /**
         * Arranges to add the specified header and value to the eventual response.
         *
         * @param key header name to add
         * @param value header value to add
         * @return the adapter
         */
        ResponseAdapter<T> header(String key, Object value);

        /**
         * Prepares the response type with the forbidden status and the specified error mesage.
         *
         * @param message error message to use in setting the response status
         * @return the factory
         */
        T forbidden(String message);

        /**
         * Prepares the response type with headers set and status set to OK.
         *
         * @return response instance
         */
        T ok();
    }


    /**
     * Analyzes the request to determine the type of request, from the CORS perspective.
     *
     * @param requestAdapter request adatper
     * @param <T> type of request wrapped by the adapter
     * @return RequestType the CORS request type of the request
     */
    public static <T> RequestType requestType(RequestAdapter<T> requestAdapter) {
        // If no origin header or same as host, then just normal
        Optional<String> originOpt = requestAdapter.firstHeader(ORIGIN);
        Optional<String> hostOpt = requestAdapter.firstHeader(HOST);
        if (originOpt.isEmpty() || (hostOpt.isPresent() && originOpt.get().contains("://" + hostOpt.get()))) {
            return RequestType.NORMAL;
        }

        // Is this a pre-flight request?
        if (requestAdapter.method().equalsIgnoreCase(Http.Method.OPTIONS.name())
                && requestAdapter.headerContainsKey(ACCESS_CONTROL_REQUEST_METHOD)) {
            return RequestType.PREFLIGHT;
        }

        // A CORS request that is not a pre-flight one
        return RequestType.CORS;
    }

    /**
     * Validates information about an incoming request as a CORS request.
     *
     * @param crossOriginConfigs config information for CORS
     * @param secondaryCrossOriginLookup locates {@code CrossOrigin} from other than config (e.g., annotations for MP)
     * @param requestAdapter abstraction of a request
     * @param responseAdapter factory for creating a response and managing its attributes (e.g., headers)
     * @param <T> type for the {@code Request} managed by the requestAdapter
     * @param <U> the type for the HTTP response as returned from the responseSetter
     * @return Optional of an error response if the request was an invalid CORS request; Optional.empty() if it was a
     *         valid CORS request
     */
    public static <T, U> Optional<U> processRequest(
            List<CrossOriginConfig> crossOriginConfigs,
            Supplier<Optional<CrossOrigin>> secondaryCrossOriginLookup,
            RequestAdapter<T> requestAdapter,
            ResponseAdapter<U> responseAdapter) {
        Optional<String> originOpt = requestAdapter.firstHeader(ORIGIN);
        Optional<CrossOrigin> crossOriginOpt = lookupCrossOrigin(requestAdapter.path(), crossOriginConfigs,
                secondaryCrossOriginLookup);
        if (crossOriginOpt.isEmpty()) {
            return Optional.of(responseAdapter.forbidden(ORIGIN_DENIED));
        }

        // If enabled but not whitelisted, deny request
        List<String> allowedOrigins = Arrays.asList(crossOriginOpt.get().value());
        if (!allowedOrigins.contains("*") && !contains(originOpt, allowedOrigins, String::equals)) {
            return Optional.of(responseAdapter.forbidden(ORIGIN_NOT_IN_ALLOWED_LIST));
        }

        // Successful processing of request
        return Optional.empty();
    }

    /**
     * Prepares a CORS response.
     *
     * @param crossOriginConfigs config information for CORS
     * @param secondaryCrossOriginLookup locates {@code CrossOrigin} from other than config (e.g., annotations for MP)
     * @param requestAdapter request adapter
     * @param responseAdapter response adapter
     * @param <T> type for the {@code Request} managed by the requestAdapter
     * @param <U> type for the {@code Response} returned by the responseAdapter
     * @return U the response provided by the responseAdapter
     */
    public static <T, U> U prepareResponse(List<CrossOriginConfig> crossOriginConfigs,
            Supplier<Optional<CrossOrigin>> secondaryCrossOriginLookup,
            RequestAdapter<T> requestAdapter,
            ResponseAdapter<U> responseAdapter) {
        CrossOrigin crossOrigin = lookupCrossOrigin(requestAdapter.path(), crossOriginConfigs, secondaryCrossOriginLookup)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Could not locate expected CORS information while preparing response to request " + requestAdapter));

        // Add Access-Control-Allow-Origin and Access-Control-Allow-Credentials.
        //
        // Throw an exception if there is no ORIGIN because we should not even be here unless this is a CORS request, which would
        // have required the ORIGIN heading to be present when we determined the request type.
        String origin = requestAdapter.firstHeader(ORIGIN).orElseThrow(noRequiredHeaderExcFactory(ORIGIN));

        if (crossOrigin.allowCredentials()) {
            responseAdapter.header(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
                           .header(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
        } else {
            List<String> allowedOrigins = Arrays.asList(crossOrigin.value());
            responseAdapter.header(ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigins.contains("*") ? "*" : origin);
        }

        // Add Access-Control-Expose-Headers if non-empty
        formatHeader(crossOrigin.exposeHeaders()).ifPresent(
                h -> responseAdapter.header(ACCESS_CONTROL_EXPOSE_HEADERS, h));

        return responseAdapter.ok();
    }

    /**
     * Processes a pre-flight request.
     *
     * @param crossOriginConfigs config information for CORS
     * @param secondaryCrossOriginLookup locates {@code CrossOrigin} from other than config (e.g., annotations for MP)
     * @param requestAdapter the request adapter
     * @param responseAdapter the response adapter
     * @param <T> type for the {@code Request} managed by the requestAdapter
     * @param <U> the type for the returned HTTP response (as returned from the response adapter)
     * @return the response returned by the response adapter with CORS-related headers set (for a successful CORS preflight)
     */
    public static <T, U> U processPreFlight(
            List<CrossOriginConfig> crossOriginConfigs,
            Supplier<Optional<CrossOrigin>> secondaryCrossOriginLookup,
            RequestAdapter<T> requestAdapter,
            ResponseAdapter<U> responseAdapter) {

        Optional<String> originOpt = requestAdapter.firstHeader(ORIGIN);
        Optional<CrossOrigin> crossOriginOpt = lookupCrossOrigin(requestAdapter.path(), crossOriginConfigs,
                secondaryCrossOriginLookup);

        // If CORS not enabled, deny request
        if (crossOriginOpt.isEmpty()) {
            return responseAdapter.forbidden(ORIGIN_DENIED);
        }
        if (originOpt.isEmpty()) {
            return responseAdapter.forbidden(noRequiredHeader(ORIGIN));
        }

        CrossOrigin crossOrigin = crossOriginOpt.get();

        // If enabled but not whitelisted, deny request
        List<String> allowedOrigins = Arrays.asList(crossOrigin.value());
        if (!allowedOrigins.contains("*") && !contains(originOpt, allowedOrigins, String::equals)) {
            return responseAdapter.forbidden(ORIGIN_NOT_IN_ALLOWED_LIST);
        }

        // Check if method is allowed
        Optional<String> methodOpt = requestAdapter.firstHeader(ACCESS_CONTROL_REQUEST_METHOD);
        List<String> allowedMethods = Arrays.asList(crossOrigin.allowMethods());
        if (!allowedMethods.contains("*")
                && methodOpt.isPresent()
                && !contains(methodOpt.get(), allowedMethods, String::equals)) {
            return responseAdapter.forbidden(METHOD_NOT_IN_ALLOWED_LIST);
        }
        String method = methodOpt.get();

        // Check if headers are allowed
        Set<String> requestHeaders = parseHeader(requestAdapter.allHeaders(ACCESS_CONTROL_REQUEST_HEADERS));
        List<String> allowedHeaders = Arrays.asList(crossOrigin.allowHeaders());
        if (!allowedHeaders.contains("*") && !contains(requestHeaders, allowedHeaders)) {
            return responseAdapter.forbidden(HEADERS_NOT_IN_ALLOWED_LIST);
        }

        // Build successful response

        responseAdapter.header(ACCESS_CONTROL_ALLOW_ORIGIN, originOpt.get());
        if (crossOrigin.allowCredentials()) {
            responseAdapter.header(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }
        responseAdapter.header(ACCESS_CONTROL_ALLOW_METHODS, method);
        formatHeader(requestHeaders.toArray()).ifPresent(
                h -> responseAdapter.header(ACCESS_CONTROL_ALLOW_HEADERS, h));
        long maxAge = crossOrigin.maxAge();
        if (maxAge > 0) {
            responseAdapter.header(ACCESS_CONTROL_MAX_AGE, maxAge);
        }
        return responseAdapter.ok();
    }

    /**
     * Looks for a matching CORS config entry for the specified path among the provided CORS configuration information, returning
     * an {@code Optional} of the matching {@code CrossOrigin} instance for the path, if any.
     *
     * @param path the possibly unnormalized request path to check
     * @param crossOriginConfigs CORS configuration
     * @param secondaryLookup Supplier for CrossOrigin used if none found in config
     * @return Optional<CrossOrigin> for the matching config, or an empty Optional if none matched
     */
    static Optional<CrossOrigin> lookupCrossOrigin(String path, List<CrossOriginConfig> crossOriginConfigs,
            Supplier<Optional<CrossOrigin>> secondaryLookup) {
        for (CrossOriginConfig config : crossOriginConfigs) {
            String pathPrefix = normalize(config.pathPrefix());
            String uriPath = normalize(path);
            if (uriPath.startsWith(pathPrefix)) {
                return Optional.of(config);
            }
        }

        return secondaryLookup.get();
    }

    /**
     * Formats an array as a comma-separate list without brackets.
     *
     * @param array The array.
     * @param <T> Type of elements in array.
     * @return Formatted array as an {@code Optional}.
     */
    static <T> Optional<String> formatHeader(T[] array) {
        if (array == null || array.length == 0) {
            return Optional.empty();
        }
        int i = 0;
        StringBuilder builder = new StringBuilder();
        do {
            builder.append(array[i++].toString());
            if (i == array.length) {
                break;
            }
            builder.append(", ");
        } while (true);
        return Optional.of(builder.toString());
    }

    /**
     * Parse list header value as a set.
     *
     * @param header Header value as a list.
     * @return Set of header values.
     */
    static Set<String> parseHeader(String header) {
        if (header == null) {
            return Collections.emptySet();
        }
        Set<String> result = new HashSet<>();
        StringTokenizer tokenizer = new StringTokenizer(header, ",");
        while (tokenizer.hasMoreTokens()) {
            String value = tokenizer.nextToken().trim();
            if (value.length() > 0) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * Parse a list of list of headers as a set.
     *
     * @param headers Header value as a list, each a potential list.
     * @return Set of header values.
     */
    static Set<String> parseHeader(List<String> headers) {
        if (headers == null) {
            return Collections.emptySet();
        }
        return parseHeader(headers.stream().reduce("", (a, b) -> a + "," + b));
    }

    /**
     * Trim leading or trailing slashes of a path.
     *
     * @param path The path.
     * @return Normalized path.
     */
    static String normalize(String path) {
        int length = path.length();
        int beginIndex = path.charAt(0) == '/' ? 1 : 0;
        int endIndex = path.charAt(length - 1) == '/' ? length - 1 : length;
        return path.substring(beginIndex, endIndex);
    }

    /**
     * Checks containment in a {@code Collection<String>}.
     *
     * @param item Optional string, typically an Optional header value.
     * @param collection The collection.
     * @param eq Equality function.
     * @return Outcome of test.
     */
    static boolean contains(Optional<String> item, Collection<String> collection, BiFunction<String, String, Boolean> eq) {
        return item.isPresent() && contains(item.get(), collection, eq);
    }

    /**
     * Checks containment in a {@code Collection<String>}.
     *
     * @param item The string.
     * @param collection The collection.
     * @param eq Equality function.
     * @return Outcome of test.
     */
    static boolean contains(String item, Collection<String> collection, BiFunction<String, String, Boolean> eq) {
        for (String s : collection) {
            if (eq.apply(item, s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks containment in two collections, case insensitively.
     *
     * @param left First collection.
     * @param right Second collection.
     * @return Outcome of test.
     */
    static boolean contains(Collection<String> left, Collection<String> right) {
        for (String s : left) {
            if (!contains(s, right, String::equalsIgnoreCase)) {
                return false;
            }
        }
        return true;
    }

    private static Supplier<IllegalArgumentException> noRequiredHeaderExcFactory(String header) {
        return () -> new IllegalArgumentException(noRequiredHeader(header));
    }

    private static String noRequiredHeader(String header) {
        return "CORS request does not have required header " + header;
    }
}
