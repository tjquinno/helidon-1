/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import java.util.Set;

import io.helidon.common.CollectionsHelper;
import io.helidon.microprofile.server.Server;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.helidon.microprofile.cors.CrossOrigin.ACCESS_CONTROL_REQUEST_HEADERS;
import static io.helidon.microprofile.cors.CrossOrigin.ACCESS_CONTROL_REQUEST_METHOD;
import static io.helidon.microprofile.cors.CrossOrigin.ORIGIN;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Class CrossOriginTest.
 */
public class CrossOriginTest {

    private static Client client;
    private static Server server;
    private static WebTarget target;

    static {
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
    }

    @BeforeAll
    static void initClass() {
        server = Server.builder()
                .addApplication("/app", new CorsApplication())
                .build();
        server.start();
        client = ClientBuilder.newClient();
        target = client.target("http://localhost:" + server.port());
    }

    @AfterAll
    static void destroyClass() throws Exception {
        server.stop();
        client.close();
    }

    @ApplicationScoped
    static public class CorsApplication extends Application {

        @Override
        public Set<Class<?>> getClasses() {
            return CollectionsHelper.setOf(CorsResource1.class, CorsResource2.class);
        }
    }

    @RequestScoped
    @Path("/cors1")
    static public class CorsResource1 {

        @OPTIONS
        @CrossOrigin
        public String options() {
            return "options";
        }

        @GET
        public String getCors() {
            return "getCors";
        }

        @PUT
        public String putCors() {
            return "putCors";
        }
    }

    @RequestScoped
    @Path("/cors2")
    static public class CorsResource2 {

        @OPTIONS
        @CrossOrigin(value = {"http://foo.bar", "http://bar.foo"},
                allowHeaders = {"X-foo", "X-bar"},
                allowMethods = {HttpMethod.GET, HttpMethod.PUT},
                allowCredentials = true,
                maxAge = -1)
        public String options() {
            return "options";
        }

        @GET
        public String getCors() {
            return "getCors";
        }

        @PUT
        public String putCors() {
            return "putCors";
        }
    }

    @Test
    void test1AllowedOrigin() {
        Response response = target.path("/app/cors1")
                .request()
                .header(ORIGIN, "http://foo.bar")
                .header(ACCESS_CONTROL_REQUEST_METHOD, "PUT")
                .options();
        assertThat(response.getStatusInfo(), is(Response.Status.OK));
    }

    @Test
    void test1AllowedMethod() {
        Response response = target.path("/app/cors1")
                .request()
                .header(ORIGIN, "http://foo.bar")
                .header(ACCESS_CONTROL_REQUEST_METHOD, "PUT")
                .options();
        assertThat(response.getStatusInfo(), is(Response.Status.OK));
    }

    @Test
    void test1AllowedHeaders1() {
        Response response = target.path("/app/cors1")
                .request()
                .header(ORIGIN, "http://foo.bar")
                .header(ACCESS_CONTROL_REQUEST_METHOD, "PUT")
                .header(ACCESS_CONTROL_REQUEST_HEADERS, "X-foo")
                .options();
        assertThat(response.getStatusInfo(), is(Response.Status.OK));
    }

    @Test
    void test1AllowedHeaders2() {
        Response response = target.path("/app/cors1")
                .request()
                .header(ORIGIN, "http://foo.bar")
                .header(ACCESS_CONTROL_REQUEST_METHOD, "PUT")
                .header(ACCESS_CONTROL_REQUEST_HEADERS, "X-foo, X-bar")
                .options();
        assertThat(response.getStatusInfo(), is(Response.Status.OK));
    }

    @Test
    void test2ForbiddenOrigin() {
        Response response = target.path("/app/cors2")
                .request()
                .header(ORIGIN, "http://not.allowed")
                .header(ACCESS_CONTROL_REQUEST_METHOD, "PUT")
                .options();
        assertThat(response.getStatusInfo(), is(Response.Status.FORBIDDEN));
    }

    @Test
    void testAllowedOrigin() {
        Response response = target.path("/app/cors2")
                .request()
                .header(ORIGIN, "http://foo.bar")
                .header(ACCESS_CONTROL_REQUEST_METHOD, "PUT")
                .options();
        assertThat(response.getStatusInfo(), is(Response.Status.OK));
    }

    @Test
    void test2ForbiddenMethod() {
        Response response = target.path("/app/cors2")
                .request()
                .header(ORIGIN, "http://foo.bar")
                .header(ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .options();
        assertThat(response.getStatusInfo(), is(Response.Status.FORBIDDEN));
    }

    @Test
    void test2AllowedMethod() {
        Response response = target.path("/app/cors2")
                .request()
                .header(ORIGIN, "http://foo.bar")
                .header(ACCESS_CONTROL_REQUEST_METHOD, "PUT")
                .options();
        assertThat(response.getStatusInfo(), is(Response.Status.OK));
    }

    @Test
    void test2ForbiddenHeader() {
        Response response = target.path("/app/cors2")
                .request()
                .header(ORIGIN, "http://foo.bar")
                .header(ACCESS_CONTROL_REQUEST_METHOD, "PUT")
                .header(ACCESS_CONTROL_REQUEST_HEADERS, "X-foo, X-bar, X-oops")
                .options();
        assertThat(response.getStatusInfo(), is(Response.Status.FORBIDDEN));
    }

    @Test
    void test2AllowedHeaders1() {
        Response response = target.path("/app/cors2")
                .request()
                .header(ORIGIN, "http://foo.bar")
                .header(ACCESS_CONTROL_REQUEST_METHOD, "PUT")
                .header(ACCESS_CONTROL_REQUEST_HEADERS, "X-foo")
                .options();
        assertThat(response.getStatusInfo(), is(Response.Status.OK));
    }

    @Test
    void test2AllowedHeaders2() {
        Response response = target.path("/app/cors2")
                .request()
                .header(ORIGIN, "http://foo.bar")
                .header(ACCESS_CONTROL_REQUEST_METHOD, "PUT")
                .header(ACCESS_CONTROL_REQUEST_HEADERS, "X-foo, X-bar")
                .options();
        assertThat(response.getStatusInfo(), is(Response.Status.OK));
    }

    @Test
    void test2AllowedHeaders3() {
        Response response = target.path("/app/cors2")
                .request()
                .header(ORIGIN, "http://foo.bar")
                .header(ACCESS_CONTROL_REQUEST_METHOD, "PUT")
                .header(ACCESS_CONTROL_REQUEST_HEADERS, "X-foo, X-bar")
                .header(ACCESS_CONTROL_REQUEST_HEADERS, "X-foo, X-bar")
                .options();
        assertThat(response.getStatusInfo(), is(Response.Status.OK));
    }
}
