/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
 */
package io.helidon.security.jersey;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;

import io.helidon.security.Security;
import io.helidon.security.SecurityContext;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;

/**
 * TODO javadoc.
 */
class SecureClientTest {
    private static Client client;

    @BeforeAll
    static void initClass() {
        client = ClientBuilder.newBuilder()
                .register(new ClientSecurityFeature())
                .build();
    }

    @AfterAll
    static void destroyClass() {
        client.close();
    }

    @Test
    void testSecureClientOverride() throws IOException {
        Security security = Security.builder().build();
        SecurityContext context = security.createContext(getClass().getName() + ".testSecureClientOverride()");

        ClientRequestContext requestContext = Mockito.mock(ClientRequestContext.class);

        when(requestContext.getProperty(ClientSecurityFeature.PROPERTY_CONTEXT)).thenReturn(context);
        when(requestContext.getProperty(ClientSecurityFeature.PROPERTY_PROVIDER)).thenReturn("http-basic-auth");
        when(requestContext.getProperty("io.helidon.security.outbound.user")).thenReturn("jack");
        when(requestContext.getProperty("io.helidon.security.outbound.password")).thenReturn("password");
        when(requestContext.getUri()).thenReturn(URI.create("http://localhost:7070/test"));
        when(requestContext.getStringHeaders()).thenReturn(new MultivaluedHashMap<>());

        ClientSecurityFilter csf = new ClientSecurityFilter();
        csf.filter(requestContext);

        //        String response = client.target("http://localhost:7777/testIt")
        //                .request()
        //                .property(ClientSecurityFeature.PROPERTY_CONTEXT, context)
        //                .property(ClientSecurityFeature.PROPERTY_PROVIDER, "http-basic-auth")
        //                .property("io.helidon.security.outbound.user", "jack")
        //                .property("io.helidon.security.outbound.password", "password")
        //                .get(String.class);

        //assertThat(response, containsString("user: jack"));

        if (1 == 1) {
            return;
        }
        String response = client.target("http://localhost:7777/testIt")
                .request()
                .property(ClientSecurityFeature.PROPERTY_CONTEXT, context)
                .property(ClientSecurityFeature.PROPERTY_PROVIDER, "jwt")
                // override security context
                .property("io.helidon.security.outbound.jwt-subject", "user@company.com")
                .property("io.helidon.security.outbound.jwt-roles", new String[] {"role1", "role2"})
                // override configuration of provider
                .property("io.helidon.security.outbound.jwk-kid", "used-to-sign")
                .property("io.helidon.security.outbound.jwt-kid", "used-in-jwt")
                .property("io.helidon.security.outbound.jwt-audience", "auidence-uri")
                .get(String.class);

    }
}
