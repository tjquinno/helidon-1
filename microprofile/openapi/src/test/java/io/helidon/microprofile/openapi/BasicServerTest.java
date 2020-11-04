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
import java.net.HttpURLConnection;
import java.util.Map;

import io.helidon.microprofile.server.Server;

import io.helidon.microprofile.tests.junit5.AddBean;
import io.helidon.microprofile.tests.junit5.HelidonTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test that MP OpenAPI support works when retrieving the OpenAPI document
 * from the server's /openapi endpoint.
 */
@HelidonTest
@AddBean(TestApp.class)
@AddBean(TestApp3.class)
public class BasicServerTest {

    private static final String OPENAPI_PATH = "/openapi";

    private static Map<String, Object> yaml;

    @Inject
    private WebTarget webTarget;

    public BasicServerTest() {
    }

    /**
     * Make sure that the annotations in the test app were found and properly
     * incorporated into the OpenAPI document.
     *
     * @throws Exception in case of errors reading the HTTP response
     */
//    @SuppressWarnings("unchecked")
    @Test
    public void simpleTest() throws Exception {
        String goSummary = TestUtil.fromYaml(getYaml(), "paths./testapp/go.get.summary", String.class);
        assertEquals(TestApp.GO_SUMMARY, goSummary);
    }

    @Test
    public void testMultipleApps() throws IOException {
        String goSummary3 = TestUtil.fromYaml(getYaml(), "paths./testapp3/go3.get.summary", String.class);
        assertEquals(TestApp3.GO_SUMMARY, goSummary3);
    }

    private Map<String, Object> getYaml() throws IOException {
        if (yaml == null) {
            synchronized (BasicServerTest.class) {
                yaml = TestUtil.getYaml(webTarget, OPENAPI_PATH);
            }
        }
        return yaml;
    }
}
