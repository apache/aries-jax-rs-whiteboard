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

package test;

import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT;

import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.jackson.TypeNameResolver;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.Test;

import org.osgi.framework.ServiceRegistration;

import test.types.TestApplicationWithClasses;
import test.types.TestHelper;
import test.types.TestOpenApiResource;

import javax.ws.rs.client.WebTarget;

import java.util.Hashtable;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OpenApiTest extends TestHelper {

    @Test
    public void testOpenApiEndpoint() {
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

            registerAddon(new TestOpenApiResource());

            String response = webTarget.request().get(String.class);

            assertTrue(response.contains("operation"));
        } finally {
            serviceRegistration.unregister();
        }
    }

    @Test
    public void testOpenApiEndpointWithModelResolver() {
        OpenAPI openAPI = new OpenAPI();

        openAPI.info(
            new Info()
                .title("My Service")
                .description("Service REST API")
                .contact(
                    new Contact()
                        .email("oschweitzer@me.com"))
        );

        Hashtable<String, Object> properties = new Hashtable<>();

        properties.put("name", "test");

        ServiceRegistration<OpenAPI> serviceRegistration =
            bundleContext.registerService(
                OpenAPI.class, openAPI, properties);

        try {
            WebTarget webTarget = createDefaultTarget().
                path("openapi.json");

            registerAddon(new TestOpenApiResource());

            String response = webTarget.request().get(String.class);

            assertFalse(response.contains("MyOwnClassName"));
        } catch (Exception e) {
            serviceRegistration.unregister();

            throw e;
        }

        ServiceRegistration<ModelConverter> serviceRegistration2 =
            bundleContext.registerService(
                ModelConverter.class, new ModelResolver(Json.mapper(), new TypeNameResolver() {
                    @Override
                    protected String nameForClass(Class<?> cls, Set<Options> options) {
                        return "MyOwnClassName";
                    }
                }), new Hashtable<>());

        try {
            WebTarget webTarget = createDefaultTarget().
                path("openapi.json");

            registerAddon(new TestOpenApiResource());

            String response = webTarget.request().get(String.class);

            assertTrue(response.contains("MyOwnClassName"));
        } catch (Exception e) {
            serviceRegistration.unregister();
        } finally {
            serviceRegistration2.unregister();
        }

        try {
            WebTarget webTarget = createDefaultTarget().
                path("openapi.json");

            registerAddon(new TestOpenApiResource());

            String response = webTarget.request().get(String.class);

            assertFalse(response.contains("MyOwnClassName"));
        } catch (Exception e) {
            serviceRegistration.unregister();

            throw e;
        }
    }

    @Test
    public void testIncludeStaticResource() {
        String applicationSelectFilter = "(" + JAX_RS_APPLICATION_BASE +
            "=/test-application-a)";

        registerAddon(
            new TestOpenApiResource(), JAX_RS_APPLICATION_SELECT,
            applicationSelectFilter);

        OpenAPI openAPI = new OpenAPI();

        openAPI.info(
            new Info()
                .title("My Service")
                .description("Service REST API")
                .contact(
                    new Contact()
                        .email("oschweitzer@me.com"))
        );

        @SuppressWarnings({"unchecked", "rawtypes", "serial"})
        ServiceRegistration<OpenAPI> serviceRegistration =
            bundleContext.registerService(
                OpenAPI.class, openAPI, new Hashtable() {{
                    put(
                        JAX_RS_APPLICATION_SELECT,
                        "(" + JAX_RS_APPLICATION_BASE + "=/test-application-*)");
                }});

        registerApplication(
            new TestApplicationWithClasses(), JAX_RS_APPLICATION_BASE,
            "/test-application-a");

        registerApplication(
            new TestApplicationWithClasses(), JAX_RS_APPLICATION_BASE,
            "/test-application-b");

        try {
            WebTarget webTarget = createDefaultTarget().
                path("test-application-a").path("openapi.json");

            String response = webTarget.request().get(String.class);

            assertTrue(response.contains("\"/operation\":"));

            webTarget = createDefaultTarget().
                path("test-application-b").path("openapi.json");

            response = webTarget.request().get(String.class);

            assertFalse(response.contains("\"/operation\":"));
        } finally {
            serviceRegistration.unregister();
        }
    }

}
