/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
 */
package io.helidon.rest.client.example.jersey;

import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;

import io.helidon.common.CollectionsHelper;
import io.helidon.microprofile.server.Server;

/**
 * TODO javadoc.
 */
public class JerseyClientExample {
    public static void main(String[] args) {
        Server server = Server.create(MyApplication.class);
        server.start();
    }

    @Path("/hello")
    public static class MyResource {
        @GET
        public String getIt() {

        }
    }

    public static class MyApplication extends Application {
        @Override
        public Set<Class<?>> getClasses() {
            return CollectionsHelper.setOf(MyResource.class);
        }
    }
}
