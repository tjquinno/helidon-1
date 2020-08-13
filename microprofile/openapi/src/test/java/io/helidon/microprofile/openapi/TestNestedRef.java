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
package io.helidon.microprofile.openapi;

import io.helidon.common.http.MediaType;
import io.helidon.config.Config;
import io.helidon.microprofile.server.Server;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;

public class TestNestedRef {
    private static final String OPENAPI_PATH = "/openapi";

    private static Server server;

    private static HttpURLConnection cnx;

//    private static Map<String, Object> yaml;
    private static String yamlText;

    /**
     * Start the server to run the test app and read the response from the
     * /openapi endpoint into a map that all tests can use.
     *
     * @throws Exception in case of error reading the response as yaml
     */
    @BeforeAll
    public static void startServer() throws Exception {
        server = TestUtil.startServer(Config.create(), NestedRefApp.class);
        cnx = TestUtil.getURLConnection(
                server.port(),
                "GET",
                OPENAPI_PATH,
                MediaType.APPLICATION_OPENAPI_YAML);

        yamlText = TestUtil.stringYAMLFromResponse(cnx);
    }

    /**
     * Stop the server.
     */
    @AfterAll
    public static void stopServer() {
        TestUtil.cleanup(server, cnx);
    }

    @Test
    public void checkNestedRefForDups() throws IOException {
        Map<String, Object> content = TestUtil.fromYaml(TestUtil.toMap(yamlText),
                "components.schemas.EnvironmentInfo.properties.links.items", Map.class);
        assertThat(content.size(), is(1));
        assertThat(content.keySet(), contains("$ref"));
        assertThat(content.get("$ref"), is("#/components/schemas/Link"));
    }
}
