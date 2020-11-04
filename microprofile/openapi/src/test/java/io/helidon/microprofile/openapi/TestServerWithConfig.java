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

import io.helidon.microprofile.server.Server;
import io.helidon.microprofile.tests.junit5.AddConfig;
import io.helidon.microprofile.tests.junit5.HelidonTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@HelidonTest
@AddConfig(key = "openapi.web-context", value="/otheropenapi")
public class TestServerWithConfig {

    private static final String ALTERNATE_OPENAPI_PATH = "/otheropenapi";

    private static Server server;

    private static HttpURLConnection cnx;

    private static Map<String, Object> yaml;

    @Inject
    private WebTarget webTarget;

    public TestServerWithConfig() {
    }

    @Test
    public void testAlternatePath() throws Exception {
        String goSummary = TestUtil.fromYaml(getYaml(), "paths./testapp/go.get.summary", String.class);
        assertEquals(TestApp.GO_SUMMARY, goSummary);
    }

    private Map<String, Object> getYaml() throws IOException {
        if (yaml == null) {
            synchronized (BasicServerTest.class) {
                yaml = TestUtil.getYaml(webTarget, ALTERNATE_OPENAPI_PATH);
            }
        }
        return yaml;
    }
}
