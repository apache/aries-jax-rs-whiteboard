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

import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_FRAMEWORKSTARTLEVEL_JSON;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_FRAMEWORKSTARTLEVEL_XML;

import java.util.Optional;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.aries.jax.rs.rest.management.schema.FrameworkStartLevelSchema;
import org.osgi.framework.BundleContext;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.startlevel.dto.FrameworkStartLevelDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

public class FrameworkStartLevelResource extends BaseResource {

    public FrameworkStartLevelResource(BundleContext bundleContext) {
        super(bundleContext);
    }

    // 137.3.1.1
    @GET
    @Produces({APPLICATION_FRAMEWORKSTARTLEVEL_JSON, APPLICATION_FRAMEWORKSTARTLEVEL_XML})
    @Path("framework/startlevel{ext:(\\.json|\\.xml)*}")
    @Operation(
        operationId = "GET/startlevel",
        summary = "Retrieves a Framework Startlevel Representation from the REST management service",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The framework start level",
                content = @Content(schema = @Schema(implementation = FrameworkStartLevelSchema.class))
            ),
            @ApiResponse(
                responseCode = "406",
                description = "The REST management service does not support any of the requested representations"
            )
        }
    )
    public Response startlevel(
        @Parameter(allowEmptyValue = true, schema = @Schema(allowableValues = {".json", ".xml"}))
        @PathParam("ext") String ext) {

        ResponseBuilder builder = Response.status(
            Response.Status.OK
        ).entity(
            FrameworkStartLevelSchema.build(framework.adapt(FrameworkStartLevelDTO.class))
        );

        return Optional.ofNullable(
            ext
        ).map(
            String::trim
        ).map(
            t -> ".json".equals(t) ? APPLICATION_FRAMEWORKSTARTLEVEL_JSON : APPLICATION_FRAMEWORKSTARTLEVEL_XML
        ).map(t -> builder.type(t)).orElse(
            builder
        ).build();
    }

    // 137.3.1.2
    @PUT
    @Consumes({APPLICATION_FRAMEWORKSTARTLEVEL_JSON, APPLICATION_FRAMEWORKSTARTLEVEL_XML})
    @Path("framework/startlevel{ext:(\\.json|\\.xml)*}")
    @Operation(
        operationId = "PUT/startlevel",
        summary = "Set the target framework startlevel",
        responses = {
            @ApiResponse(
                responseCode = "204",
                description = "The request was received and valid. The framework will asynchronously start to adjust the framework startlevel until the target startlevel has been reached"
            ),
            @ApiResponse(
                responseCode = "400",
                description = "The REST management service received an IllegalArgumentException when trying to adjust the framework startlevel"
            ),
            @ApiResponse(
                responseCode = "415",
                description = "The request had a media type that is not supported by the REST management service"
            )
        }
    )
    public Response startlevel(
        FrameworkStartLevelSchema update,
        @Parameter(allowEmptyValue = true, schema = @Schema(allowableValues = {".json", ".xml"}))
        @PathParam("ext") String ext) {

        try {
            FrameworkStartLevel current = framework.adapt(FrameworkStartLevel.class);

            if (current.getStartLevel() != update.startLevel) {
                current.setStartLevel(update.startLevel);
            }
            if (current.getInitialBundleStartLevel() != update.initialBundleStartLevel) {
                current.setInitialBundleStartLevel(update.initialBundleStartLevel);
            }

            return Response.noContent().build();
        }
        catch (IllegalArgumentException exception) {
            throw new WebApplicationException(exception, 400);
        }
    }

}
