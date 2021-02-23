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

import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.*;

import java.util.Optional;
import java.util.stream.Stream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.aries.jax.rs.rest.management.schema.ServiceSchema;
import org.osgi.framework.BundleContext;
import org.osgi.framework.dto.ServiceReferenceDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

public class FrameworkServiceResource extends BaseResource {

    public FrameworkServiceResource(BundleContext bundleContext) {
        super(bundleContext);
    }

    // 137.3.9.1
    @GET
    @Produces({APPLICATION_SERVICE_JSON, APPLICATION_SERVICE_XML})
    @Path("framework/service/{serviceid}{ext:(\\.json|\\.xml)*}")
    @Operation(
        operationId = "GET/service",
        summary = "Retrieves a Service Representation from the REST management service",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The request has been served successfully",
                content = @Content(schema = @Schema(implementation = ServiceSchema.class))
            ),
            @ApiResponse(
                responseCode = "404",
                description = "There is no service with the given service id"
            ),
            @ApiResponse(
                responseCode = "406",
                description = "The REST management service does not support any of the requested representations"
            )
        }
    )
    public Response service(
        @PathParam("serviceid") long serviceid,
        @Parameter(allowEmptyValue = true, description = "File extension", schema = @Schema(allowableValues = {".json", ".xml"}))
        @PathParam("ext") String ext) {

        ResponseBuilder builder = Response.ok(
            Stream.of(
                bundleContext.getBundles()
            ).flatMap(
                bundle -> Optional.ofNullable(
                    bundle.adapt(ServiceReferenceDTO[].class)
                ).map(
                    Stream::of
                ).orElseGet(
                    Stream::empty
                )
            ).filter(
                sr -> sr.id == serviceid
            ).findFirst().map(
                sr -> ServiceSchema.build(uriInfo, sr)
            ).orElseThrow(
                () -> new WebApplicationException(404)
            )
        );

        return Optional.ofNullable(
            ext
        ).map(
            String::trim
        ).map(
            e -> ".json".equals(e) ? APPLICATION_SERVICE_JSON : APPLICATION_SERVICE_XML
        ).map(
            type -> builder.type(type)
        ).orElse(
            builder
        ).build();
    }

}
