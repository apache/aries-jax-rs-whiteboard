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

import static javax.ws.rs.core.HttpHeaders.ACCEPT_LANGUAGE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLEHEADER_JSON;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLEHEADER_XML;

import java.util.Locale;
import java.util.Optional;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.aries.jax.rs.rest.management.internal.map.CaseInsensitiveMap;
import org.apache.aries.jax.rs.rest.management.schema.BundleHeaderSchema;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

public class FrameworkBundleHeaderResource extends BaseResource {

    public FrameworkBundleHeaderResource(BundleContext bundleContext) {
        super(bundleContext);
    }

    // 137.3.6.1
    @GET
    @Produces({APPLICATION_BUNDLEHEADER_JSON, APPLICATION_BUNDLEHEADER_XML})
    @Path("framework/bundle/{bundleid}/header{ext:(\\.json|\\.xml)*}")
    @Operation(
        operationId = "GET/bundle/header",
        summary = "Retrieves a Bundle Header Representation from the REST management service",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The request has been served successfully",
                content = @Content(schema = @Schema(implementation = BundleHeaderSchema.class))
            ),
            @ApiResponse(
                responseCode = "404",
                description = "There is no bundle with the given bundle id"
            ),
            @ApiResponse(
                responseCode = "406",
                description = "The REST management service does not support any of the requested representations"
            )
        },
        parameters = {
            @Parameter(
                name = ACCEPT_LANGUAGE,
                description = "The raw header value is used unless an Accept-Language header is set on the HTTP request. If multiple accepted languages are set only the first is used to localize the header",
                in = ParameterIn.HEADER
            )
        }
    )
    public Response bundleHeaders(
        @PathParam("bundleid") long bundleid,
        @Parameter(allowEmptyValue = true, schema = @Schema(allowableValues = {".json", ".xml"}))
        @PathParam("ext") String ext) {

        String language = headers.getAcceptableLanguages().stream().findFirst().map(Locale::toString).orElse(null);

        Bundle bundle = bundleContext.getBundle(bundleid);

        if (bundle == null) {
            throw new WebApplicationException(404);
        }

        ResponseBuilder builder = Response.ok(
            new BundleHeaderSchema(new CaseInsensitiveMap<>(bundle.getHeaders(language)))
        );

        return Optional.ofNullable(
            ext
        ).map(
            String::trim
        ).map(
            e -> ".json".equals(e) ? APPLICATION_BUNDLEHEADER_JSON : APPLICATION_BUNDLEHEADER_XML
        ).map(
            type -> builder.type(type)
        ).orElse(
            builder
        ).build();
    }

}
