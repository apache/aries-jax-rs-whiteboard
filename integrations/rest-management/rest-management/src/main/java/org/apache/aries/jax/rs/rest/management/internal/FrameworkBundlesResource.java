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

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.HttpHeaders.CONTENT_LOCATION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLEEXCEPTION_JSON;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLEEXCEPTION_JSON_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLEEXCEPTION_XML;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLEEXCEPTION_XML_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLESTATE_JSON;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLESTATE_XML;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLES_JSON;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLES_XML;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLE_JSON;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLE_JSON_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLE_XML;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_VNDOSGIBUNDLE;

import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.aries.jax.rs.rest.management.schema.BundleExceptionSchema;
import org.apache.aries.jax.rs.rest.management.schema.BundleSchema;
import org.apache.aries.jax.rs.rest.management.schema.BundleListSchema;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

public class FrameworkBundlesResource extends BaseResource {

    public FrameworkBundlesResource(BundleContext bundleContext) {
        super(bundleContext);
    }

    // 137.3.2.1
    @GET
    @Produces({APPLICATION_BUNDLES_JSON, APPLICATION_BUNDLES_XML})
    @Path("framework/bundles{ext:(\\.json|\\.xml)*}")
    @Operation(
        operationId = "GET/bundles",
        summary = "Retrieves a Bundle List Representation from the REST management service",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The request has been served successfully",
                content = @Content(schema = @Schema(implementation = BundleListSchema.class))
            ),
            @ApiResponse(
                responseCode = "406",
                description = "The REST management service does not support any of the requested representations"
            )
        },
        parameters = {
            @Parameter(
                name = "<capability_namespace>",
                in = ParameterIn.QUERY,
                description = "A query parameter whose name is an osgi capability namespace and whose value is a filter expression that follows the Core Specifications Framework Filter Syntax; see OSGi Core, Chapter 3.2.7 Filter Syntax. Filters are matched against the attributes of capabilities in the respective namespaces. If multiple capabilities for a given namespace are present, then a filter succeeds when one of these capabilities matches. When multiple filter expressions across namespaces are given, these are combined with the and operator.",
                examples = @ExampleObject(
                    summary = "format: <capability_namespace>=<filter_string>",
                    value = "osgi.identity=(osgi.identity=foo)&osgi.package.wiring=(osgi.package.wiring=com.foo)"
                )
            )
        }
    )
    public Response bundles(
        @Parameter(allowEmptyValue = true, description = "File extension", schema = @Schema(allowableValues = {".json", ".xml"}))
        @PathParam("ext") String ext) {

        Predicate<Bundle> predicate = fromNamespaceQuery(uriInfo);

        ResponseBuilder builder = Response.status(
            Response.Status.OK
        ).entity(
            BundleListSchema.build(
                Stream.of(
                    bundleContext.getBundles()
                ).filter(
                    predicate
                ).map(
                    Bundle::getBundleId
                ).map(
                    String::valueOf
                ).map(
                    id -> uriInfo.getBaseUriBuilder().path("framework").path("bundle").path("{id}").build(id).toASCIIString()
                ).collect(toList())
            )
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

    // 137.3.2.2
    @POST
    @Consumes(TEXT_PLAIN)
    @Produces({APPLICATION_BUNDLE_JSON, APPLICATION_BUNDLE_XML})
    @Path("framework/bundles{ext:(\\.json|\\.xml)*}")
    @Operation(
        operationId = "POST/bundles",
        summary = "Installs a new bundle to the managed framework and thereby logically appends it to the bundles resource",
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
                content = @Content(schema = @Schema(implementation = BundleSchema.class))
            ),
            @ApiResponse(
                responseCode = "400",
                description = "the REST management service received a BundleException when trying to install",
                content = {
                    @Content(mediaType = APPLICATION_BUNDLEEXCEPTION_JSON, schema = @Schema(implementation = BundleExceptionSchema.class)),
                    @Content(mediaType = APPLICATION_BUNDLEEXCEPTION_XML, schema = @Schema(implementation = BundleExceptionSchema.class))
                }
            ),
            @ApiResponse(
                responseCode = "406",
                description = "The REST management service does not support any of the requested representations"
            )
        },
        parameters = {
            @Parameter(
                name = CONTENT_LOCATION,
                in = ParameterIn.HEADER,
                description = "When posting a bundle jar a location string can be specified using the 'Content-Location' header",
                required = false
            )
        }
    )
    public Response bundles(
        String location,
        @Parameter(allowEmptyValue = true, description = "File extension", schema = @Schema(allowableValues = {".json", ".xml"}))
        @PathParam("ext") String ext,
        @HeaderParam(CONTENT_TYPE) MediaType contentType) {

        try {
            Instant now = Instant.now().minusMillis(2);
            Bundle bundle = bundleContext.installBundle(location);

            if (!now.isBefore(Instant.ofEpochMilli(bundle.getLastModified()))) {
                throw new WebApplicationException(location, 409);
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
    }

    // 137.3.2.3
    @POST
    @Consumes(APPLICATION_VNDOSGIBUNDLE)
    @Produces({APPLICATION_BUNDLE_JSON, APPLICATION_BUNDLE_XML})
    @Path("framework/bundles{ext:(\\.json|\\.xml)*}")
    @Operation(hidden = true)
    public Response bundles(
        InputStream inputStream,
        @Parameter(allowEmptyValue = true, description = "File extension", schema = @Schema(allowableValues = {".json", ".xml"}))
        @PathParam("ext") String ext,
        @HeaderParam(CONTENT_LOCATION) String location,
        @HeaderParam(CONTENT_TYPE) MediaType contentType) {

        try {
            location = Optional.ofNullable(location).orElseGet(
                () -> "org.apache.aries.jax.rs.whiteboard:".concat(
                    UUID.randomUUID().toString()));

            Instant now = Instant.now().minusMillis(2);
            Bundle bundle = bundleContext.installBundle(location, inputStream);

            if (!now.isBefore(Instant.ofEpochMilli(bundle.getLastModified()))) {
                throw new WebApplicationException(location, 409);
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
    }

}
