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
 *
 */
package io.helidon.microprofile.openapi;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import javax.ws.rs.client.WebTarget;

import javax.ws.rs.core.Response;

import io.helidon.common.http.MediaType;

import org.yaml.snakeyaml.Yaml;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Useful utility methods during testing.
 */
public class TestUtil {

    static Map<String, Object> getYaml(WebTarget webTarget, String openApiPath) throws IOException {
        Response response = webTarget
                .path(openApiPath)
                .request()
                .get();
        return TestUtil.yamlFromResponse(openApiPath, response);
    }

    /**
     * Returns the {@code MediaType} instance conforming to the HTTP response
     * content type.
     *
     * @param response the Response from which to get the content type
     * @return the MediaType corresponding to the content type in the response
     */
    public static MediaType mediaTypeFromResponse(Response response) {
        MediaType returnedMediaType = MediaType.parse(response.getMediaType().getType());
        if (!returnedMediaType.charset().isPresent()) {
            returnedMediaType = MediaType.builder()
                    .type(returnedMediaType.type())
                    .subtype(returnedMediaType.subtype())
                    .charset(Charset.defaultCharset().name())
                    .build();
        }
        return returnedMediaType;
    }

    /**
     * Represents response payload as a String.
     *
     * @param response the response from which to get the response payload
     * @return String representation of the OpenAPI document as a String
     * @throws IOException in case of errors reading the HTTP response payload
     */
    public static String stringYAMLFromResponse(Response response) throws IOException {
        MediaType returnedMediaType = mediaTypeFromResponse(response);
        assertThat("Unexpected returned media type", MediaType.APPLICATION_OPENAPI_YAML.test(returnedMediaType), is(true));
        return stringFromResponse(response);
    }

    /**
     * Returns a {@code String} resulting from interpreting the response payload
     * in the specified connection according to the expected {@code MediaType}.
     *
     * @param response {@code Response} with the entity to convert
     * @return {@code String} of the payload
     * @throws IOException in case of errors reading the response payload
     */
    public static String stringFromResponse(Response response) throws IOException {
        return response.readEntity(String.class);
    }

    /**
     * Returns the response payload from the specified connection as a snakeyaml
     * {@code Yaml} object.
     *
     * @param openApiPath path for the endpoint from which to retrieve the OpenAPI document
     * @param response the {@code Response} containing the entity to process
     * @return the YAML {@code Map<String, Object>} (created by snakeyaml) from
     * the response payload
     * @throws IOException in case of errors reading the response
     */
    @SuppressWarnings(value = "unchecked")
    public static Map<String, Object> yamlFromResponse(String openApiPath, Response response) throws IOException {
        assertThat("Error during GET from " + openApiPath, response.getStatus(), is(Response.Status.OK.getStatusCode()));
        Yaml yaml = new Yaml();
        return (Map<String, Object>) yaml.load(stringFromResponse(response));
    }

    /**
     * Treats the provided {@code Map} as a YAML map and navigates through it
     * using the dotted-name convention as expressed in the {@code dottedPath}
     * argument, finally casting the value retrieved from the last segment of
     * the path as the specified type and returning that cast value.
     *
     * @param <T> type to which the final value will be cast
     * @param map the YAML-inspired map
     * @param dottedPath navigation path to the item of interest in the YAML
     * maps-of-maps; note that the {@code dottedPath} must not use dots except
     * as path segment separators
     * @param cl {@code Class} for the return type {@code <T>}
     * @return value from the lowest-level map retrieved using the last path
     * segment, cast to the specified type
     */
    @SuppressWarnings(value = "unchecked")
    public static <T> T fromYaml(Map<String, Object> map, String dottedPath, Class<T> cl) {
        String[] segments = dottedPath.split("\\.");
        for (int i = 0; i < segments.length - 1; i++) {
            map = (Map<String, Object>) map.get(segments[i]);
        }
        return cl.cast(map.get(segments[segments.length - 1]));
    }
}
