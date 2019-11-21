package org.apache.aries.jax.rs.openapi;

import java.util.Optional;

import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.aries.jax.rs.whiteboard.ApplicationClasses;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.jaxrs2.integration.resources.BaseOpenApiResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;

@Path("/openapi.{type:json|yaml}")
public class OpenApiResource extends BaseOpenApiResource {

    @Context
    Application app;

    @Context
    ApplicationClasses applicationClasses;

    @Context
    ServletConfig config;

    @GET
    @Produces({MediaType.APPLICATION_JSON, "application/yaml"})
    @Operation(hidden = true)
    public Response getOpenApi(@Context HttpHeaders headers,
                               @Context UriInfo uriInfo,
                               @PathParam("type") String type) throws Exception {

        String ctxId = app.getClass().getCanonicalName()
            .concat("#").concat(String.valueOf(System.identityHashCode(app)));

        OpenApiContext ctx = new JaxrsOpenApiContextBuilder<>()
            .servletConfig(config)
            .application(app)
            .configLocation(configLocation)
            .openApiConfiguration(openApiConfiguration)
            .ctxId(ctxId)
            .buildContext(true);

        ctx.setOpenApiScanner(new JaxrsWhiteboardScanner(applicationClasses));

        OpenAPI oas = ctx.read();

        if (oas == null) {
            return Response.status(404).build();
        }

        boolean pretty = Optional.ofNullable(ctx.getOpenApiConfiguration()).map(OpenAPIConfiguration::isPrettyPrint).orElse(Boolean.FALSE);

        if (Optional.ofNullable(type).map(String::trim).map("yaml"::equalsIgnoreCase).orElse(Boolean.FALSE)) {
            return Response.status(Response.Status.OK)
                  .entity(pretty ? Yaml.pretty(oas) : Yaml.mapper().writeValueAsString(oas))
                  .type("application/yaml")
                  .build();
        } else {
            return Response.status(Response.Status.OK)
                  .entity(pretty ? Json.pretty(oas) : Json.mapper().writeValueAsString(oas))
                  .type(MediaType.APPLICATION_JSON_TYPE)
                  .build();
        }
    }

}
