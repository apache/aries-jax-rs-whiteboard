package test;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.Assert.assertTrue;
import static org.osgi.resource.Namespace.EFFECTIVE_ACTIVE;
import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_EXTENSION_SELECT;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_MEDIA_TYPE;

import java.util.Hashtable;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.junit.Test;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JSONRequired;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import test.types.TestHelper;
import test.types.TestJaxbResource;

@Requirement(
    namespace = SERVICE_NAMESPACE, //
    filter = JSONRequired.FILTER, //
    effective = EFFECTIVE_ACTIVE
)
public class JaxbTest extends TestHelper {

    @Test
    public void testJaxbEndpoint() {
        OpenAPI openAPI = new OpenAPI();

        openAPI.info(
            new Info()
                .title("My Service")
                .description("Service REST API")
                .contact(
                    new Contact()
                        .email("oschweitzer@me.com"))
        );

        ServiceRegistration<OpenAPI> serviceRegistration =
            bundleContext.registerService(
                OpenAPI.class, openAPI, new Hashtable<>());

        try {
            WebTarget webTarget = createDefaultTarget().
                path("openapi.json");

            registerAddon(new TestJaxbResource(),
                JAX_RS_EXTENSION_SELECT,
                new String[] {
                    String.format(
                        "(&(objectClass=%s)(%s=%s))",
                        MessageBodyReader.class.getName(),
                        JAX_RS_MEDIA_TYPE, APPLICATION_JSON),
                    String.format(
                        "(&(objectClass=%s)(%s=%s))",
                        MessageBodyWriter.class.getName(),
                        JAX_RS_MEDIA_TYPE, APPLICATION_JSON),
                    }
                );

            String response = webTarget.request().get(String.class);

            System.out.println(response);

            String schema = "{\"schemas\":{\"JaxbModel\":{\"type\":\"object\",\"properties\":{\"bar\":{\"type\":\"string\"}}}}}";

            //assertTrue(response.contains(schema));
        }
        finally {
            serviceRegistration.unregister();
        }
    }

}
