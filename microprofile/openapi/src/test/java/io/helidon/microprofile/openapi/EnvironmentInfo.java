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

import org.eclipse.microprofile.openapi.annotations.media.Schema;

abstract class EnvironmentInfo extends Entity {
    @Schema(required = true, maxLength = 80)
    private String releaseVersion;

    @Schema(required = true, maxLength = 80)
    private String environmentType;

    @Schema(required = true, maxLength = 80)
    private String environmentSize;

    @Schema(required = true, maxLength = 80)
    private boolean breakGlassEnabled;
}
