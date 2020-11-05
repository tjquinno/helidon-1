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

import io.helidon.microprofile.tests.junit5.AddBean;
import io.helidon.microprofile.tests.junit5.AddExtension;
import io.helidon.microprofile.tests.junit5.HelidonTest;

import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.Map;

@HelidonTest
@AddExtension(io.helidon.microprofile.openapi.VetoCdiExtension.class)
@AddBean(VetoedResource.class)
public class TestVetoedResource {

    private static final String OPEN_API_PATH = "/openapi";

    private static Boolean originalSetting = null;

    @Inject
    private WebTarget webTarget;

    @Test
    void testNoOpenApiInfoForVetoedResource() throws IOException {
        // The OpenAPI CDI extension should ignore the vetoed resource's endpoints.
        Response response = webTarget
                .path(OPEN_API_PATH)
                .request()
                .get();

        Map<String, Object> yaml = TestUtil.yamlFromResponse(OPEN_API_PATH, response);
        Map<String, Object> paths = (Map<String, Object>) yaml.get("paths");
        assertThat("OpenAPI CDI extension incorrectly registered an endpoint on a vetoed resource",
                paths.containsKey("/vetoed"), is(false));
    }
}
