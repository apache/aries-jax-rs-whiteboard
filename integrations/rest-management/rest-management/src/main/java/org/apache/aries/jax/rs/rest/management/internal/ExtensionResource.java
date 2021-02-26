/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.jax.rs.rest.management.internal;

import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_EXTENSIONS_JSON;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_EXTENSIONS_XML;

import java.util.Optional;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.aries.component.dsl.CachingServiceReference;
import org.apache.aries.jax.rs.rest.management.schema.ExtensionsSchema;
import org.osgi.framework.BundleContext;
import org.osgi.service.rest.RestApiExtension;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

public class ExtensionResource extends BaseResource {

    private final Set<CachingServiceReference<RestApiExtension>> extensions;

    public ExtensionResource(
        BundleContext bundleContext,
        Set<CachingServiceReference<RestApiExtension>> extensions) {

        super(bundleContext);
        this.extensions = extensions;
    }

    @GET
    @Path("extensions{ext: (\\.json|\\.xml)*}")
    @Produces({APPLICATION_EXTENSIONS_JSON , APPLICATION_EXTENSIONS_XML})
    @Operation(
        summary = "Retrieves a Extensions Representation ",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The framework bundle",
                content = @Content(schema = @Schema(implementation = ExtensionsSchema.class))
            ),
            @ApiResponse(
                responseCode = "406",
                description = "The REST management service does not support any of the requested representations"
            )
        }
    )
    public Response extensions(
        @Parameter(allowEmptyValue = true, schema = @Schema(allowableValues = {".json", ".xml"}))
        @PathParam("ext") String ext) {

        ResponseBuilder builder = Response.status(
            Response.Status.OK
        ).entity(
            ExtensionsSchema.build(extensions)
        );

        return Optional.ofNullable(
            ext
        ).map(
            String::trim
        ).map(
            t -> ".json".equals(t) ? APPLICATION_EXTENSIONS_JSON : APPLICATION_EXTENSIONS_XML
        ).map(t -> builder.type(t)).orElse(
            builder
        ).build();
    }

}
