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
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_SERVICES_REPRESENTATIONS_JSON;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_SERVICES_REPRESENTATIONS_XML;

import java.util.Optional;
import java.util.function.Predicate;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.aries.jax.rs.rest.management.internal.map.CaseInsensitiveMap;
import org.apache.aries.jax.rs.rest.management.schema.ServiceSchema;
import org.apache.aries.jax.rs.rest.management.schema.ServiceSchemaListSchema;
import org.osgi.framework.BundleContext;
import org.osgi.framework.dto.FrameworkDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

public class FrameworkServicesRepresentationsResource extends BaseResource {

    public FrameworkServicesRepresentationsResource(BundleContext bundleContext) {
        super(bundleContext);
    }

    // 137.3.9.1
    @GET
    @Produces({APPLICATION_SERVICES_REPRESENTATIONS_JSON, APPLICATION_SERVICES_REPRESENTATIONS_XML})
    @Path("framework/services/representations{ext:(\\.json|\\.xml)*}")
    @Operation(
        operationId = "GET/services/representations",
        summary = "Retrieves the service representation of each available service",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The request has been served successfully",
                content = @Content(schema = @Schema(implementation = ServiceSchemaListSchema.class))
            ),
            @ApiResponse(
                responseCode = "400",
                description = "The provided filter expression was not valid"
            ),
            @ApiResponse(
                responseCode = "406",
                description = "The REST management service does not support any of the requested representations"
            )
        }
    )
    public Response serviceDTOs(
        @Parameter(
            name = "filter",
            in = ParameterIn.QUERY,
            description = "A filter parameter whose value is a filter expression that follows the Core Specifications Framework Filter Syntax; see OSGi Core, Chapter 3.2.7 Filter Syntax. The filter is matched against the service attributes and only services that match are returned",
            examples = @ExampleObject(
                summary = "format: filter=<filter_string>",
                value = "filter=(&(osgi.vendor=Apache Aries)(objectClass=com.acme.Widget))"
            )
        )
        @QueryParam("filter") String filter,
        @Parameter(allowEmptyValue = true, description = "File extension", schema = @Schema(allowableValues = {".json", ".xml"}))
        @PathParam("ext") String ext) {

        Predicate<ServiceReferenceDTO> predicate = fromFilterQuery(filter);

        ResponseBuilder builder = Response.ok(
            ServiceSchemaListSchema.build(
                framework.adapt(
                    FrameworkDTO.class
                ).services.stream().map(
                    sr -> {
                        sr.properties = new CaseInsensitiveMap<Object>(sr.properties);
                        return sr;
                    }
                ).filter(
                    predicate
                ).map(
                    sr -> ServiceSchema.build(uriInfo, sr)
                ).collect(toList())
            )
        );

        return Optional.ofNullable(
            ext
        ).map(
            String::trim
        ).map(
            e -> ".json".equals(e) ? APPLICATION_SERVICES_REPRESENTATIONS_JSON : APPLICATION_SERVICES_REPRESENTATIONS_XML
        ).map(
            type -> builder.type(type)
        ).orElse(
            builder
        ).build();
    }

}
