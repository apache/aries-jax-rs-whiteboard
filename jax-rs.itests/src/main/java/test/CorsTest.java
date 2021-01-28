package test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_NAME;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;

import org.apache.cxf.rs.security.cors.CrossOriginResourceSharingFilter;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceRegistration;

import test.types.TestAddon;
import test.types.TestApplication;
import test.types.TestHelper;

public class CorsTest extends TestHelper {

    @Test
    public void basicCors() throws Exception {
        AtomicBoolean calledFilterBefore = new AtomicBoolean();
        AtomicBoolean calledFilterAfter = new AtomicBoolean();

        registerExtension(
            new String[] {
                ContainerRequestFilter.class.getName(),
                ContainerResponseFilter.class.getName()},
            new PrototypeServiceFactory<Object>() {
                @Override
                public Object getService(
                    Bundle bundle, ServiceRegistration<Object> registration) {
                    return new CrossOriginResourceSharingFilter() {

                        @Override
                        public void filter(
                            ContainerRequestContext requestContext,
                            ContainerResponseContext responseContext) {

                            super.filter(requestContext, responseContext);
                            calledFilterAfter.set(true);
                        }
                        @Override
                        public void filter(ContainerRequestContext context) {
                            super.filter(context);
                            calledFilterBefore.set(true);
                        }
                    };
                }
                @Override
                public void ungetService(
                    Bundle bundle, ServiceRegistration<Object> registration,
                    Object service) {
                }
            },
            "FOO",
            JAX_RS_NAME, CrossOriginResourceSharingFilter.class.getSimpleName());

        registerAddon(
            new TestAddon(),
            JAX_RS_APPLICATION_SELECT, String.format("(%s=%s)", JAX_RS_NAME, "FOO"));

        registerApplication(
            new TestApplication(),
            JAX_RS_NAME, "FOO");

        WebTarget target = createDefaultTarget();

        try {
            System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

            Response response = target.path(
                "/test-application"
            ).path(
                "/test"
            ).request().header(
                "Origin", "http://foo.com"
            ).get();

            String result = response.readEntity(String.class);

            assertThat(result).isEqualTo("Hello test");

            assertThat(calledFilterBefore).isTrue();
            assertThat(calledFilterAfter).isTrue();

            assertThat(response.getStatus()).isEqualTo(200);

            assertThat(calledFilterBefore).isTrue();
            assertThat(calledFilterAfter).isTrue();

            assertThat(response.getStringHeaders()).contains(
                entry(
                    "Access-Control-Allow-Origin",
                    Collections.singletonList("*")
                )
            );
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            System.setProperty("sun.net.http.allowRestrictedHeaders", "flase");
        }
    }

}
