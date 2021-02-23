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

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLEEXCEPTION_JSON;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLEEXCEPTION_JSON_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLEEXCEPTION_XML;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLEEXCEPTION_XML_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLESTATE_JSON;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLESTATE_JSON_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLESTATE_XML;
import static org.osgi.framework.Bundle.ACTIVE;
import static org.osgi.framework.Bundle.INSTALLED;
import static org.osgi.framework.Bundle.RESOLVED;

import java.util.Collections;
import java.util.Optional;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.aries.jax.rs.rest.management.schema.BundleExceptionSchema;
import org.apache.aries.jax.rs.rest.management.schema.BundleStateSchema;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.wiring.FrameworkWiring;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

public class FrameworkBundleStateResource extends BaseResource {

    public FrameworkBundleStateResource(BundleContext bundleContext) {
        super(bundleContext);
    }

    // 137.3.5.1
    @GET
    @Produces({APPLICATION_BUNDLESTATE_JSON, APPLICATION_BUNDLESTATE_XML})
    @Path("framework/bundle/{bundleid}/state{ext:(\\.json|\\.xml)*}")
    @Operation(
        operationId = "GET/bundle/state",
        summary = "Retrieves a Bundle State Representation from the REST management service",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The request has been served successfully",
                content = @Content(schema = @Schema(implementation = BundleStateSchema.class))
            ),
            @ApiResponse(
                responseCode = "404",
                description = "There is no bundle with the given bundle id"
            ),
            @ApiResponse(
                responseCode = "406",
                description = "The REST management service does not support any of the requested representations"
            )
        }
    )
    public Response bundleState(
        @PathParam("bundleid") long bundleid,
        @Parameter(allowEmptyValue = true, description = "File extension", schema = @Schema(allowableValues = {".json", ".xml"}))
        @PathParam("ext") String ext) {

        Bundle bundle = bundleContext.getBundle(bundleid);

        if (bundle == null) {
            throw new WebApplicationException(404);
        }

        ResponseBuilder builder = Response.ok(
            bundleStateSchema(bundle)
        );

        return Optional.ofNullable(
            ext
        ).map(
            String::trim
        ).map(
            e -> ".json".equals(e) ? APPLICATION_BUNDLESTATE_JSON : APPLICATION_BUNDLESTATE_XML
        ).map(
            type -> builder.type(type)
        ).orElse(
            builder
        ).build();
    }

    // 137.3.5.2
    @PUT
    @Consumes({APPLICATION_BUNDLESTATE_JSON, APPLICATION_BUNDLESTATE_XML})
    @Produces({APPLICATION_BUNDLESTATE_JSON, APPLICATION_BUNDLESTATE_XML})
    @Path("framework/bundle/{bundleid}/state{ext:(\\.json|\\.xml)*}")
    @Operation(
        operationId = "PUT/bundle/state",
        summary = "Sets the target state for the given bundle. This can, e.g., be state=32 for transitioning the bundle to started, or state=4 for stopping the bundle and transitioning it to resolved",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The request was received and valid and the framework has performed a state change",
                content = @Content(schema = @Schema(implementation = BundleStateSchema.class))
            ),
            @ApiResponse(
                responseCode = "400",
                description = "The REST management service received a BundleException when trying to perform the state transition",
                content = {
                    @Content(mediaType = APPLICATION_BUNDLEEXCEPTION_JSON, schema = @Schema(implementation = BundleExceptionSchema.class)),
                    @Content(mediaType = APPLICATION_BUNDLEEXCEPTION_XML, schema = @Schema(implementation = BundleExceptionSchema.class))
                }
            ),
            @ApiResponse(
                responseCode = "402",
                description = "The requested target state is not reachable from the current bundle state or is not a target state"
            ),
            @ApiResponse(
                responseCode = "404",
                description = "There is no bundle with the given bundle id"
            ),
            @ApiResponse(
                responseCode = "415",
                description = "The request had a media type that is not supported by the REST management service"
            )
        }
    )
    public Response bundleState(
        BundleStateSchema bundleStateDTO,
        @PathParam("bundleid") long bundleid,
        @Parameter(allowEmptyValue = true, description = "File extension", schema = @Schema(allowableValues = {".json", ".xml"}))
        @PathParam("ext") String ext,
        @HeaderParam(CONTENT_TYPE) MediaType contentType) {

        try {
            Bundle bundle = bundleContext.getBundle(bundleid);

            if (bundle == null) {
                throw new WebApplicationException("there is no bundle with the given bundle id", 404);
            }

            int currentState = bundle.getState();

            if ((currentState & INSTALLED) == INSTALLED ||
                (currentState & RESOLVED) == RESOLVED) {
                if ((bundleStateDTO.state & ACTIVE) == ACTIVE) {
                    bundle.start(bundleStateDTO.options);
                }
                else if ((bundleStateDTO.state & RESOLVED) == RESOLVED) {
                    framework.adapt(
                        FrameworkWiring.class
                    ).resolveBundles(
                        Collections.singleton(bundle)
                    );
                }
            }
            else if ((currentState & ACTIVE) == ACTIVE) {
                if ((bundleStateDTO.state & RESOLVED) == RESOLVED) {
                    bundle.stop(bundleStateDTO.options);
                }
            }
            else {
                throw new WebApplicationException(
                    String.format(
                        "the requested target state [%s] is not reachable " +
                        "from the current bundle state [%s] or is not a " +
                        "target state",
                        bundleStateDTO.state, currentState), 402);
            }

            ResponseBuilder builder = Response.ok(
                bundleStateSchema(bundle)
            );

            return Optional.ofNullable(
                ext
            ).map(
                String::trim
            ).map(
                e -> ".json".equals(e) ? APPLICATION_BUNDLESTATE_JSON : APPLICATION_BUNDLESTATE_XML
            ).map(
                type -> builder.type(type)
            ).orElse(
                builder
            ).build();
        }
        catch (BundleException exception) {
            return Response.status(400).type(
                contentType.equals(APPLICATION_BUNDLESTATE_JSON_TYPE) ?
                    APPLICATION_BUNDLEEXCEPTION_JSON_TYPE :
                    APPLICATION_BUNDLEEXCEPTION_XML_TYPE
            ).entity(
                BundleExceptionSchema.build(exception.getType(), exception.getMessage())
            ).build();
        }
        catch (Exception exception) {
            throw new WebApplicationException(exception, 400);
        }
    }

}
