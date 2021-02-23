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
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLESTARTLEVEL_JSON;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLESTARTLEVEL_JSON_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLESTARTLEVEL_XML;

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
import org.apache.aries.jax.rs.rest.management.schema.BundleStartLevelSchema;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.dto.BundleStartLevelDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

public class FrameworkBundleStartLevelResource extends BaseResource {

    public FrameworkBundleStartLevelResource(BundleContext bundleContext) {
        super(bundleContext);
    }

    // 137.3.7.1
    @GET
    @Produces({APPLICATION_BUNDLESTARTLEVEL_JSON, APPLICATION_BUNDLESTARTLEVEL_XML})
    @Path("framework/bundle/{bundleid}/startlevel{ext:(\\.json|\\.xml)*}")
    @Operation(
        operationId = "GET/bundle/startlevel",
        summary = "Retrieves a Bundle Startlevel Representation from the REST management service",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The request has been served successfully",
                content = @Content(schema = @Schema(implementation = BundleStartLevelSchema.class))
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
    public Response bundleStartlevel(
        @PathParam("bundleid") long bundleid,
        @Parameter(allowEmptyValue = true, description = "File extension", schema = @Schema(allowableValues = {".json", ".xml"}))
        @PathParam("ext") String ext) {

        Bundle bundle = bundleContext.getBundle(bundleid);

        if (bundle == null) {
            throw new WebApplicationException(404);
        }

        ResponseBuilder builder = Response.ok(
            BundleStartLevelSchema.build(bundle.adapt(BundleStartLevelDTO.class))
        );

        return Optional.ofNullable(
            ext
        ).map(
            String::trim
        ).map(
            e -> ".json".equals(e) ? APPLICATION_BUNDLESTARTLEVEL_JSON : APPLICATION_BUNDLESTARTLEVEL_XML
        ).map(
            type -> builder.type(type)
        ).orElse(
            builder
        ).build();
    }

    // 137.3.7.2
    @PUT
    @Consumes({APPLICATION_BUNDLESTARTLEVEL_JSON, APPLICATION_BUNDLESTARTLEVEL_XML})
    @Produces({APPLICATION_BUNDLESTARTLEVEL_JSON, APPLICATION_BUNDLESTARTLEVEL_XML})
    @Path("framework/bundle/{bundleid}/startlevel{ext:(\\.json|\\.xml)*}")
    @Operation(
        operationId = "PUT/bundle/startlevel",
        summary = "Sets the target bundle startlevel",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The request was received and valid. The REST management service has changed the bundle startlevel according to the target value",
                content = @Content(schema = @Schema(implementation = BundleStartLevelSchema.class))
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Either the target startlevel state involved invalid values, e.g., a startlevel smaller or equal to zero and the REST management service got an IllegalArgumentException, or the REST management service received a BundleException when trying to perform the startlevel change",
                content = {
                    @Content(mediaType = APPLICATION_BUNDLEEXCEPTION_JSON, schema = @Schema(implementation = BundleExceptionSchema.class)),
                    @Content(mediaType = APPLICATION_BUNDLEEXCEPTION_XML, schema = @Schema(implementation = BundleExceptionSchema.class))
                }
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
    public Response bundleStartlevel(
        BundleStartLevelSchema update,
        @PathParam("bundleid") long bundleid,
        @Parameter(allowEmptyValue = true, description = "File extension", schema = @Schema(allowableValues = {".json", ".xml"}))
        @PathParam("ext") String ext,
        @HeaderParam(CONTENT_TYPE) MediaType contentType) {

        Bundle bundle = bundleContext.getBundle(bundleid);

        if (bundle == null) {
            throw new WebApplicationException(404);
        }

        try {
            BundleStartLevel current = bundle.adapt(BundleStartLevel.class);

            if (current.getStartLevel() != update.startLevel) {
                current.setStartLevel(update.startLevel);
            }

            ResponseBuilder builder = Response.ok(
                BundleStartLevelSchema.build(bundle.adapt(BundleStartLevelDTO.class))
            );

            return Optional.ofNullable(
                ext
            ).map(
                String::trim
            ).map(
                e -> ".json".equals(e) ? APPLICATION_BUNDLESTARTLEVEL_JSON : APPLICATION_BUNDLESTARTLEVEL_XML
            ).map(
                type -> builder.type(type)
            ).orElse(
                builder
            ).build();
        }
        catch (Exception exception) {
            if (exception instanceof BundleException) {
                return Response.status(400).type(
                    contentType.equals(APPLICATION_BUNDLESTARTLEVEL_JSON_TYPE) ?
                        APPLICATION_BUNDLEEXCEPTION_JSON_TYPE :
                        APPLICATION_BUNDLEEXCEPTION_XML_TYPE
                ).entity(
                    BundleExceptionSchema.build(
                        ((BundleException)exception).getType(), exception.getMessage())
                ).build();
            }
            else {
                throw new WebApplicationException(exception, 400);
            }
        }
    }

}
