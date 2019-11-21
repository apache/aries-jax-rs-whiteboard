package org.apache.aries.jax.rs.openapi;

import io.swagger.v3.jaxrs2.integration.resources.BaseOpenApiResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

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

@ObjectClassDefinition(name = "Service Root Configuration")
@interface OpenApiJaxrsConfiguration {

    String swaggerDescription() default "Service REST API";

    String swaggerTitle() default "My Service";

    String swaggerContact() default "oschweitzer@me.com";
}
@Designate(ocd = OpenApiJaxrsConfiguration.class)
@Path("/openapi.{type:json|yaml}")
@Component(service = OpenApiResource.class)
@JaxrsName("OpenApiResource")
@JaxrsResource
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + "*" + ")")
public class OpenApiResource extends BaseOpenApiResource {
    @Context
    ServletConfig config;

    @Context
    Application app;

    private OpenApiJaxrsConfiguration openApiJaxrsConfiguration;

    @Activate
    private void activate(OpenApiJaxrsConfiguration openApiJaxrsConfiguration) {
        this.openApiJaxrsConfiguration = openApiJaxrsConfiguration;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, "application/yaml"})
    @Operation(hidden = true)
    public Response getOpenApi(@Context HttpHeaders headers,
                               @Context UriInfo uriInfo,
                               @PathParam("type") String type) throws Exception {

        OpenAPI openAPI = new OpenAPI();
        openAPI.info(new Info()
                .title(openApiJaxrsConfiguration.swaggerTitle())
                .description(openApiJaxrsConfiguration.swaggerDescription())
                .contact(new Contact()
                        .email(openApiJaxrsConfiguration.swaggerContact()))
        );

        openAPI.addServersItem(new Server().url(uriInfo.getBaseUri().toString()));

        OpenAPIConfiguration openAPIConfiguration = new SwaggerConfiguration()
                .openAPI(openAPI)
                .prettyPrint(true);

        super.setOpenApiConfiguration(openAPIConfiguration);

        return super.getOpenApi(headers, config, app, uriInfo, type);
    }
}
