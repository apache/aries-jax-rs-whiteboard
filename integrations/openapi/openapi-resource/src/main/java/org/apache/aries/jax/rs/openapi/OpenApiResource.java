package org.apache.aries.jax.rs.openapi;

import java.util.Optional;

import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.Providers;

import org.apache.aries.jax.rs.whiteboard.ApplicationClasses;
import org.osgi.annotation.bundle.Capability;
import org.osgi.namespace.implementation.ImplementationNamespace;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.jaxrs2.integration.resources.BaseOpenApiResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;

@Capability(
    namespace = ImplementationNamespace.IMPLEMENTATION_NAMESPACE,
    name = "org.apache.aries.jax.rs.openapi"
)
@Path("/openapi.{type:json|yaml}")
public class OpenApiResource extends BaseOpenApiResource {

    @Context
    Application app;

    @Context
    ApplicationClasses applicationClasses;

    @Context
    ServletConfig config;
    private long serviceId;

    @Context
    Providers providers;

    @Context
    Configuration configuration;

    public OpenApiResource(long serviceId) {
        this.serviceId = serviceId;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, "application/yaml"})
    @Operation(hidden = true)
    public Response getOpenApi(@Context HttpHeaders headers,
                               @Context UriInfo uriInfo,
                               @PathParam("type") String type) throws Exception {

        String ctxId = app.getClass().getCanonicalName()
            .concat("#").
                concat(String.valueOf(System.identityHashCode(app))).
                concat(String.valueOf(this.serviceId));

        OpenApiContext ctx = new JaxrsOpenApiContextBuilder<>()
            .servletConfig(config)
            .application(app)
            .configLocation(configLocation)
            .openApiConfiguration(openApiConfiguration)
            .ctxId(ctxId)
            .buildContext(false);

        ctx.setOpenApiScanner(new JaxrsWhiteboardScanner(app, applicationClasses));

        ctx.init();

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
