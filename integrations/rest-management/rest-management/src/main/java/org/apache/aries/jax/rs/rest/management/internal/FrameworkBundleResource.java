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
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLEEXCEPTION_JSON;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLEEXCEPTION_JSON_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLEEXCEPTION_XML;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLEEXCEPTION_XML_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLE_JSON;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLE_JSON_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLE_XML;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_VNDOSGIBUNDLE;

import java.io.InputStream;
import java.net.URL;
import java.util.Optional;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import org.apache.aries.jax.rs.rest.management.schema.BundleSchema;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

public class FrameworkBundleResource extends BaseResource {

    public FrameworkBundleResource(BundleContext bundleContext) {
        super(bundleContext);
    }

    // 137.3.4.1
    @GET
    @Produces({APPLICATION_BUNDLE_JSON, APPLICATION_BUNDLE_XML})
    @Path("framework/bundle/{bundleid}{ext:(\\.json|\\.xml)*}")
    @Operation(
        operationId = "GET/bundle",
        summary = "Retrieves a Bundle Representation from the REST management service",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The request has been served successfully",
                content = @Content(schema = @Schema(implementation = BundleSchema.class))
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
    public Response bundle(
        @PathParam("bundleid") long bundleid,
        @Parameter(allowEmptyValue = true, description = "File extension", schema = @Schema(allowableValues = {".json", ".xml"}))
        @PathParam("ext") String ext) {

        Bundle bundle = bundleContext.getBundle(bundleid);

        if (bundle == null) {
            throw new WebApplicationException("There is no bundle with the given bundle id", 404);
        }

        ResponseBuilder builder = Response.ok(
            bundleSchema(bundle)
        );

        return Optional.ofNullable(
            ext
        ).map(
            String::trim
        ).map(
            e -> ".json".equals(e) ? APPLICATION_BUNDLE_JSON : APPLICATION_BUNDLE_XML
        ).map(
            type -> builder.type(type)
        ).orElse(
            builder
        ).build();
    }

    // 137.3.4.2
    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces({APPLICATION_BUNDLE_JSON, APPLICATION_BUNDLE_XML})
    @Path("framework/bundle/{bundleid}{ext:(\\.json|\\.xml)*}")
    @Operation(
        operationId = "PUT/bundle",
        summary = "Updates the bundle with a new version",
        requestBody = @RequestBody(
            description = "One of the following contents is allowed; a location string with content-type type 'text/plain' OR a bundle jar with content-type 'application/vnd.osgi.bundle'",
            required = true,
            content = {
                @Content(mediaType = TEXT_PLAIN, schema = @Schema(description = "a location string", type = "string")),
                @Content(mediaType = APPLICATION_VNDOSGIBUNDLE, schema = @Schema(description = "a bundle jar", type = "string", format = "binary"))
            }
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The request was received and valid and the framework has issued the update",
                content = @Content(schema = @Schema(implementation = BundleSchema.class))
            ),
            @ApiResponse(
                responseCode = "400",
                description = "the REST management service received a BundleException when trying to update",
                content = {
                    @Content(mediaType = APPLICATION_BUNDLEEXCEPTION_JSON, schema = @Schema(implementation = BundleExceptionSchema.class)),
                    @Content(mediaType = APPLICATION_BUNDLEEXCEPTION_XML, schema = @Schema(implementation = BundleExceptionSchema.class))
                }
            ),
            @ApiResponse(
                responseCode = "404",
                description = "The REST management service does not support any of the requested representations"
            )
        }
    )
    public Response bundle(
        String location,
        @PathParam("bundleid") long bundleid,
        @Parameter(allowEmptyValue = true, description = "File extension", schema = @Schema(allowableValues = {".json", ".xml"}))
        @PathParam("ext") String ext,
        @HeaderParam(CONTENT_TYPE) MediaType contentType) {

        try {
            Bundle bundle = bundleContext.getBundle(bundleid);

            if (bundle == null) {
                throw new WebApplicationException(String.valueOf(bundleid), 404);
            }

            if (location != null && !location.isEmpty()) {
                bundle.update(new URL(location).openStream());
            }
            else {
                bundle.update();
            }

            ResponseBuilder builder = Response.ok(
                bundleSchema(bundle)
            );

            return Optional.ofNullable(
                ext
            ).map(
                String::trim
            ).map(
                e -> ".json".equals(e) ? APPLICATION_BUNDLE_JSON : APPLICATION_BUNDLE_XML
            ).map(
                type -> builder.type(type)
            ).orElse(
                builder
            ).build();
        }
        catch (BundleException exception) {
            return Response.status(400).type(
                contentType.equals(APPLICATION_BUNDLE_JSON_TYPE) ?
                    APPLICATION_BUNDLEEXCEPTION_JSON_TYPE :
                    APPLICATION_BUNDLEEXCEPTION_XML_TYPE
            ).entity(
                BundleExceptionSchema.build(exception.getType(), exception.getMessage())
            ).build();
        }
        catch (Exception e) {
            throw new WebApplicationException(e, 400);
        }
    }

    // 137.3.4.3
    @PUT
    @Consumes(APPLICATION_VNDOSGIBUNDLE)
    @Produces({APPLICATION_BUNDLE_JSON, APPLICATION_BUNDLE_XML})
    @Path("framework/bundle/{bundleid}{ext:(\\.json|\\.xml)*}")
    @Operation(hidden = true)
    public Response putBundle(
        InputStream inputStream,
        @PathParam("bundleid") long bundleid,
        @Parameter(allowEmptyValue = true, description = "File extension", schema = @Schema(allowableValues = {".json", ".xml"}))
        @PathParam("ext") String ext,
        @HeaderParam(CONTENT_TYPE) MediaType contentType) {

        try {
            Bundle bundle = bundleContext.getBundle(bundleid);

            if (bundle == null) {
                throw new WebApplicationException(String.valueOf(bundleid), 404);
            }

            bundle.update(inputStream);

            ResponseBuilder builder = Response.ok(
                bundleSchema(bundle)
            );

            return Optional.ofNullable(
                ext
            ).map(
                String::trim
            ).map(
                e -> ".json".equals(e) ? APPLICATION_BUNDLE_JSON : APPLICATION_BUNDLE_XML
            ).map(
                type -> builder.type(type)
            ).orElse(
                builder
            ).build();
        }
        catch (BundleException exception) {
            return Response.status(400).type(
                contentType.equals(APPLICATION_BUNDLE_JSON_TYPE) ?
                    APPLICATION_BUNDLEEXCEPTION_JSON_TYPE :
                    APPLICATION_BUNDLEEXCEPTION_XML_TYPE
            ).entity(
                BundleExceptionSchema.build(exception.getType(), exception.getMessage())
            ).build();
        }
    }

    // 137.3.4.4
    @DELETE
    @Produces({APPLICATION_BUNDLE_JSON, APPLICATION_BUNDLE_XML})
    @Path("framework/bundle/{bundleid}{ext:(\\.json|\\.xml)*}")
    @Operation(
        operationId = "DELETE/bundle",
        summary = "Uninstalls the bundle from the framework",
        responses = {
            @ApiResponse(
                responseCode = "204",
                description = "The request was received and valid and the framework has uninstalled the bundle",
                content = @Content(schema = @Schema(implementation = BundleSchema.class))
            ),
            @ApiResponse(
                responseCode = "400",
                description = "The REST management service received a BundleException when trying to uninstall",
                content = {
                    @Content(mediaType = APPLICATION_BUNDLEEXCEPTION_JSON, schema = @Schema(implementation = BundleExceptionSchema.class)),
                    @Content(mediaType = APPLICATION_BUNDLEEXCEPTION_XML, schema = @Schema(implementation = BundleExceptionSchema.class))
                }
            ),
            @ApiResponse(
                responseCode = "404",
                description = "There is no bundle with the given bundle id"
            )
        }
    )
    public Response deleteBundle(
        @PathParam("bundleid") long bundleid,
        @Parameter(allowEmptyValue = true, description = "File extension", schema = @Schema(allowableValues = {".json", ".xml"}))
        @PathParam("ext") String ext,
        @HeaderParam(CONTENT_TYPE) MediaType contentType) {

        try {
            Bundle bundle = bundleContext.getBundle(bundleid);

            if (bundle == null) {
                throw new WebApplicationException(String.valueOf(bundleid), 404);
            }

            bundle.uninstall();

            ResponseBuilder builder = Response.ok(
                bundleSchema(bundle)
            );

            return Optional.ofNullable(
                ext
            ).map(
                String::trim
            ).map(
                e -> ".json".equals(e) ? APPLICATION_BUNDLE_JSON : APPLICATION_BUNDLE_XML
            ).map(
                type -> builder.type(type)
            ).orElse(
                builder
            ).build();
        }
        catch (BundleException exception) {
            return Response.status(400).type(
                contentType.equals(APPLICATION_BUNDLE_JSON_TYPE) ?
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
