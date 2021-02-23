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
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLES_REPRESENTATIONS_JSON;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLES_REPRESENTATIONS_XML;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.aries.jax.rs.rest.management.schema.BundleSchemaListSchema;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

public class FrameworkBundlesRepresentationsResource extends BaseResource {

    public FrameworkBundlesRepresentationsResource(BundleContext bundleContext) {
        super(bundleContext);
    }

    // 137.3.3.1
    @GET
    @Produces({APPLICATION_BUNDLES_REPRESENTATIONS_JSON, APPLICATION_BUNDLES_REPRESENTATIONS_XML})
    @Path("framework/bundles/representations{ext:(\\.json|\\.xml)*}")
    @Operation(
        operationId = "GET/bundles/representations",
        summary = "Retrieve the bundle representation of each installed bundle",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The request has been served successfully",
                content = @Content(schema = @Schema(implementation = BundleSchemaListSchema.class))
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
    public Response bundlesRepresentations(
        @Parameter(allowEmptyValue = true, description = "File extension", schema = @Schema(allowableValues = {".json", ".xml"}))
        @PathParam("ext") String ext) {

        Predicate<Bundle> predicate = fromNamespaceQuery(uriInfo);

        ResponseBuilder builder = Response.ok(
            BundleSchemaListSchema.build(
                Stream.of(
                    bundleContext.getBundles()
                ).filter(
                    predicate
                ).map(
                    this::bundleSchema
                ).collect(toList())
            )
        );

        return Optional.ofNullable(
            ext
        ).map(
            String::trim
        ).map(
            e -> ".json".equals(e) ? APPLICATION_BUNDLES_REPRESENTATIONS_JSON : APPLICATION_BUNDLES_REPRESENTATIONS_XML
        ).map(
            type -> builder.type(type)
        ).orElse(
            builder
        ).build();
    }

}
