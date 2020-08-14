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
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.links.Link;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
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
                        @Schema(name = "EnvironmentInfo", type = SchemaType.OBJECT, implementation = EnvironmentInfo.class),
                }
        )
)
public class NestedRefApp extends Application {


    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(Resource.class);
    }

}
