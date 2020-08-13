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

import org.eclipse.microprofile.openapi.annotations.Components;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.links.Link;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import javax.enterprise.context.ApplicationScoped;
import javax.json.JsonArrayBuilder;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ApplicationScoped
@ApplicationPath("/v1")
@OpenAPIDefinition(
        tags = {
                @Tag(name="endpoints", description="Endpoints"),
                @Tag(name="environmentInfo", description="Environment Info") },
        info = @Info(
                title="Nested Ref Example",
                version = "v1") ,
        components = @Components(
                schemas = {
                        @Schema(name = "Link", title = "Link", type = SchemaType.OBJECT, implementation = Link.class),
                        @Schema(name = "EnvironmentInfo", type = SchemaType.OBJECT, implementation = NestedRefApp.EnvironmentInfo.class),
                }
        )
)
public class NestedRefApp extends Application {

    abstract static class Link{
        protected String rel;
        protected String href;
    }


    abstract static class Entity {
        protected List<Link> links = new ArrayList<>();
        public abstract JsonArrayBuilder createLinksBuilder();
        public abstract void addLink(UriInfo uriInfo);
    }

    abstract static class EnvironmentInfo extends Entity {
        @Schema(required = true,maxLength=80)
        private String releaseVersion;

        @Schema(required = true,maxLength=80)
        private String environmentType;

        @Schema(required = true,maxLength=80)
        private String environmentSize;

        @Schema(required = true,maxLength=80)
        private boolean breakGlassEnabled;
    }

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(Resource.class);
    }

    @Path("/")
    static class Resource {

        @GET
        @Operation(operationId = "getEnvironmentInfo",
                summary = "Environment properties from the monolith like release version, environment type, break glass enabled, etc.",
                description = "Get Environment Information.")
        @APIResponse(description = "Environment Info",
                content = @Content(mediaType = "application/json",
                        /* schema = @Schema(allOf = GetEnvironmentInfoResponse.class,
                                implementation = EnvironmentInfo.class))) */
                        schema = @Schema(name = "GetEnvironmentInfoResponse", ref = "#/components/schemas/EnvironmentInfo")))
        @Produces(MediaType.APPLICATION_JSON)
        public EnvironmentInfo getEnvironmentInfo() {

            return null; // Not useful for real, but this allows compilation to succeed.

        }
    }

}
