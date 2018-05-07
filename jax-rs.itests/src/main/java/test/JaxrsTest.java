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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.*;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.ext.ContextProvider;
import org.apache.cxf.message.Message;
import org.junit.Test;
import org.osgi.framework.ServiceRegistration;

import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.jaxrs.client.PromiseRxInvoker;
import org.osgi.service.jaxrs.client.SseEventSourceFactory;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;
import org.osgi.service.jaxrs.runtime.dto.ApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.DTOConstants;
import org.osgi.service.jaxrs.runtime.dto.FailedApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.ResourceDTO;
import org.osgi.service.jaxrs.runtime.dto.ResourceMethodInfoDTO;
import org.osgi.service.jaxrs.runtime.dto.RuntimeDTO;
import org.osgi.util.promise.Promise;

import test.types.ConfigurationAwareResource;
import test.types.CxfExtensionTestAddon;
import test.types.ExtensionA;
import test.types.ExtensionB;
import test.types.TestAddon;
import test.types.TestAddonConflict;
import test.types.TestAddonConflict2;
import test.types.TestApplication;
import test.types.TestApplicationConflict;
import test.types.TestApplicationWithException;
import test.types.TestAsyncResource;
import test.types.TestCxfExtension;
import test.types.TestFilter;
import test.types.TestHelper;
import test.types.TestSSEApplication;

import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.sse.SseEventSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

public class JaxrsTest extends TestHelper {

    @Test
    public void testApplication() throws InterruptedException {
        assertEquals(0, getRuntimeDTO().applicationDTOs.length);

        registerApplication(new TestApplication());

        assertEquals(1, getRuntimeDTO().applicationDTOs.length);

        WebTarget webTarget = createDefaultTarget().path("/test-application");

        Response response = webTarget.request().get();

        assertEquals("Hello application",
            response.readEntity(String.class));

        RuntimeDTO runtimeDTO = _runtime.getRuntimeDTO();

        ApplicationDTO[] applicationDTOs = runtimeDTO.applicationDTOs;

        assertEquals(1, applicationDTOs.length);

        ApplicationDTO applicationDTO = applicationDTOs[0];
        ResourceMethodInfoDTO[] resourceMethods =
            applicationDTO.resourceMethods;

        assertEquals(1, resourceMethods.length);

        ResourceMethodInfoDTO resourceMethod = resourceMethods[0];
        assertEquals(HttpMethod.GET, resourceMethod.method);
        assertEquals("/", resourceMethod.path);
        assertNull(resourceMethod.consumingMimeType);
        assertArrayEquals(
            new String[]{MediaType.TEXT_PLAIN},
            resourceMethod.producingMimeType);
        assertNull(resourceMethod.nameBindings);

    }

    @Test
    public void testApplicationChangeCount() throws Exception {
        Long changeCount = (Long)_runtimeServiceReference.getProperty(
            "service.changecount");

        ServiceRegistration<?> serviceRegistration =
            registerApplication(
                new TestApplication(), JAX_RS_APPLICATION_BASE,
                "/test-counter");

        Long newCount = (Long)_runtimeServiceReference.getProperty(
            "service.changecount");

        assertTrue(changeCount < newCount);

        changeCount = newCount;

        serviceRegistration.unregister();

        newCount = (Long)_runtimeServiceReference.getProperty(
            "service.changecount");

        assertTrue(changeCount < newCount);
    }

    @Test
    public void testApplicationConflict() {
        WebTarget webTarget = createDefaultTarget().path("test-application");

        ServiceRegistration<?> serviceRegistration2;

        registerApplication(new TestApplication());

        Response response = webTarget.request().get();

        assertEquals("Hello application", response.readEntity(String.class));

        serviceRegistration2 = registerApplication(
            new TestApplicationConflict(), "service.ranking", -1);

        response = webTarget.request().get();

        assertEquals("Hello application", response.readEntity(String.class));

        assertEquals(
            Response.Status.NOT_FOUND.getStatusCode(),
            webTarget.path("conflict").request().get().getStatus());

        serviceRegistration2.unregister();

        response = webTarget.request().get();

        assertEquals("Hello application", response.readEntity(String.class));
    }

    @Test
    public void testApplicationEndpointExtension() {
        WebTarget webTarget = createDefaultTarget().
            path("/test-application").
            path("extended");

        registerApplication(new TestApplication());

        registerAddon(
            new TestAddon(), JAX_RS_APPLICATION_SELECT,
            "(" + JAX_RS_APPLICATION_BASE + "=/test-application)");

        assertEquals(
            "Hello extended",
            webTarget.request().get().readEntity(String.class));

        RuntimeDTO runtimeDTO = _runtime.getRuntimeDTO();

        ApplicationDTO[] applicationDTOs = runtimeDTO.applicationDTOs;

        assertEquals(1, applicationDTOs.length);

        ApplicationDTO applicationDTO = applicationDTOs[0];

        ResourceMethodInfoDTO[] resourceMethods =
            applicationDTO.resourceMethods;

        assertEquals(1, resourceMethods.length);

        ResourceMethodInfoDTO resourceMethod = resourceMethods[0];
        assertEquals(HttpMethod.GET, resourceMethod.method);
        assertEquals("/", resourceMethod.path);
        assertNull(resourceMethod.consumingMimeType);
        assertArrayEquals(
            new String[]{MediaType.TEXT_PLAIN},
            resourceMethod.producingMimeType);
        assertNull(resourceMethod.nameBindings);
    }

    @Test
    public void testApplicationEndpointExtensionReadd()
        throws InterruptedException {

        WebTarget webTarget = createDefaultTarget().
            path("/test-application").
            path("extended");

        registerApplication(new TestApplication());

        Runnable testCase = () -> {
            assertEquals(webTarget.request().get().getStatus(), 404);

            ServiceRegistration<?> serviceRegistration = null;

            try {
                serviceRegistration = registerAddon(
                    new TestAddon(), JAX_RS_APPLICATION_SELECT,
                    "(" + JAX_RS_APPLICATION_BASE + "=/test-application)");

                assertEquals(
                    "Hello extended",
                    webTarget.request().get().readEntity(String.class));
            }
            finally {
                if (serviceRegistration != null) {
                    serviceRegistration.unregister();
                }
            }
        };

        testCase.run();

        testCase.run();
    }

    @Test
    public void testApplicationEndpointExtensionRuntimeDTO() {
        registerApplication(new TestApplication());

        registerAddon(
            new TestAddon(), JAX_RS_APPLICATION_SELECT,
            "(" + JAX_RS_APPLICATION_BASE + "=/test-application)");

        RuntimeDTO runtimeDTO = _runtime.getRuntimeDTO();

        assertEquals(1, runtimeDTO.applicationDTOs.length);

        ApplicationDTO applicationDTO = runtimeDTO.applicationDTOs[0];
        assertEquals(applicationDTO.base, "/test-application");
        assertEquals(1, applicationDTO.resourceDTOs.length);

        ResourceDTO resourceDTO = applicationDTO.resourceDTOs[0];
        assertEquals(1, resourceDTO.resourceMethods.length);

        ResourceMethodInfoDTO resourceMethod = resourceDTO.resourceMethods[0];
        assertEquals(HttpMethod.GET, resourceMethod.method);
        assertEquals("/{name}", resourceMethod.path);
        assertNull(resourceMethod.consumingMimeType);
        assertNull(resourceMethod.producingMimeType);
        assertNull(resourceMethod.nameBindings);
    }

    @Test
    public void testApplicationOverride() throws InterruptedException {
        WebTarget webTarget = createDefaultTarget().path("test-application");

        JaxrsServiceRuntime runtime = getJaxrsServiceRuntime();

        ServiceRegistration<?> serviceRegistration2;

        assertEquals(0, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(0, runtime.getRuntimeDTO().failedApplicationDTOs.length);

        registerApplication(new TestApplication());

        assertEquals(1, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(0, runtime.getRuntimeDTO().failedApplicationDTOs.length);

        Response response = webTarget.request().get();

        assertEquals("Hello application", response.readEntity(String.class));

        serviceRegistration2 = registerApplication(
            new TestApplicationConflict(), "service.ranking", 1);

        assertEquals(1, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(1, runtime.getRuntimeDTO().failedApplicationDTOs.length);

        response = webTarget.request().get();

        assertEquals(
            "Hello application conflict", response.readEntity(String.class));

        assertEquals(
            "conflict",
            webTarget.path("conflict").request().get(String.class));

        serviceRegistration2.unregister();

        assertEquals(1, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(0, runtime.getRuntimeDTO().failedApplicationDTOs.length);

        response = webTarget.request().get();

        assertEquals("Hello application", response.readEntity(String.class));
    }

    @Test
    public void testApplicationProviderExtension() {
        WebTarget webTarget = createDefaultTarget().path("/test-application");

        registerApplication(new TestApplication());

        registerExtension(
            "filter", JAX_RS_APPLICATION_SELECT,
            "(" + JAX_RS_APPLICATION_BASE + "=/test-application)");

        Response response = webTarget.request().get();

        assertEquals("Hello application", response.readEntity(String.class));

        assertEquals("true", response.getHeaders().getFirst("Filtered"));
    }

    @Test
    public void testApplicationProviderExtensionReadd() {
        WebTarget webTarget = createDefaultTarget().path("/test-application");

        registerApplication(new TestApplication());

        assertEquals(
            "Hello application",
            webTarget.request().get().readEntity(String.class));

        Runnable testCase = () -> {
            Response response = webTarget.request().get();

            assertNull(response.getHeaders().getFirst("Filtered"));

            ServiceRegistration<?> filterRegistration = null;

            try {
                filterRegistration = registerExtension(
                    "Filter",
                    JAX_RS_APPLICATION_SELECT,
                    "(" + JAX_RS_APPLICATION_BASE + "=/test-application)");

                response = webTarget.request().get();

                assertEquals(
                    response.getHeaders().getFirst("Filtered"), "true");
            }
            finally {
                if (filterRegistration != null) {
                    filterRegistration.unregister();
                }
            }
        };

        testCase.run();

        testCase.run();
    }

    @Test
    public void testApplicationReadd() throws InterruptedException {
        WebTarget webTarget = createDefaultTarget().path("/test-application");

        Runnable testCase = () -> {
            int applications = getRuntimeDTO().applicationDTOs.length;

            assertEquals(404, webTarget.request().get().getStatus());

            ServiceRegistration<?> serviceRegistration = null;

            try {
                serviceRegistration = registerApplication(
                    new TestApplication());

                assertEquals(
                    "Hello application",
                    webTarget.
                        request().
                        get().
                        readEntity(String.class));

                assertEquals(
                    applications + 1,
                    getRuntimeDTO().applicationDTOs.length);
            }
            finally {
                if (serviceRegistration != null) {
                    serviceRegistration.unregister();
                }
            }
        };

        testCase.run();

        testCase.run();
    }

    @SuppressWarnings("serial")
    @Test
    public void testApplicationRebase() {
        assertEquals(0, getRuntimeDTO().applicationDTOs.length);

        ServiceRegistration<Application> serviceRegistration =
            registerApplication(new TestApplication());

        assertEquals(1, getRuntimeDTO().applicationDTOs.length);

        WebTarget webTarget = createDefaultTarget().path("/test-application");

        Response response = webTarget.request().get();

        assertEquals("Hello application", response.readEntity(String.class));

        serviceRegistration.setProperties(
            new Hashtable<String, Object>() {{
                put(JAX_RS_APPLICATION_BASE, "/test-application-rebased");
            }});

        webTarget = createDefaultTarget().path("/test-application-rebased");

        response = webTarget.request().get();

        assertEquals("Hello application", response.readEntity(String.class));

        serviceRegistration.setProperties(
            new Hashtable<String, Object>() {{
                put(JAX_RS_APPLICATION_BASE, "/test-application-rebased-again");
            }});

        webTarget = createDefaultTarget().path("/test-application-rebased-again");

        response = webTarget.request().get();

        assertEquals("Hello application", response.readEntity(String.class));
    }

    @Test
    public void testApplicationReplaceDefault() {
        assertEquals(0, getRuntimeDTO().applicationDTOs.length);
        assertEquals(0, getRuntimeDTO().failedApplicationDTOs.length);

        registerAddon(new TestAddon());

        assertEquals(
            "Hello test",
            createDefaultTarget().path("/test").request().get(String.class));

        ServiceRegistration<Application> serviceRegistration =
            registerApplication(new TestApplication(), JAX_RS_NAME, ".default");

        assertEquals(0, getRuntimeDTO().applicationDTOs.length);
        assertEquals(1, getRuntimeDTO().failedApplicationDTOs.length);
        assertEquals(
            serviceRegistration.getReference().getProperty("service.id"),
            getRuntimeDTO().defaultApplication.serviceId);
        assertEquals(
            DTOConstants.FAILURE_REASON_DUPLICATE_NAME,
            getRuntimeDTO().failedApplicationDTOs[0].failureReason);

        assertEquals(
            "Hello application",
            createDefaultTarget().
                path("/test-application").
                request().
                get().
                readEntity(String.class));

        assertEquals(
            404,
            createDefaultTarget().path("/test").request().get().getStatus());

        assertEquals(
            "Hello test",
            createDefaultTarget().
                path("/test-application").
                path("/test").
                request().
                get(String.class));

        ServiceRegistration<Application> serviceRegistration2 =
            registerApplication(
                new TestApplicationConflict(), JAX_RS_NAME, ".default",
                "service.ranking", 1);

        assertEquals(0, getRuntimeDTO().applicationDTOs.length);
        assertEquals(2, getRuntimeDTO().failedApplicationDTOs.length);
        assertEquals(
            serviceRegistration2.getReference().getProperty("service.id"),
            getRuntimeDTO().defaultApplication.serviceId);
        assertEquals(
            DTOConstants.FAILURE_REASON_DUPLICATE_NAME,
            getRuntimeDTO().failedApplicationDTOs[0].failureReason);
        assertEquals(
            DTOConstants.FAILURE_REASON_DUPLICATE_NAME,
            getRuntimeDTO().failedApplicationDTOs[1].failureReason);

        assertEquals(
            "Hello application conflict",
            createDefaultTarget().
                path("/test-application").
                request().
                get().
                readEntity(String.class));

        assertEquals(
            404,
            createDefaultTarget().path("/test").request().get().getStatus());

        assertEquals(
            "Hello test",
            createDefaultTarget().
                path("/test-application").
                path("/test").
                request().
                get(String.class));

        serviceRegistration2.unregister();

        assertEquals(0, getRuntimeDTO().applicationDTOs.length);
        assertEquals(1, getRuntimeDTO().failedApplicationDTOs.length);
        assertEquals(
            serviceRegistration.getReference().getProperty("service.id"),
            getRuntimeDTO().defaultApplication.serviceId);
        assertEquals(
            DTOConstants.FAILURE_REASON_DUPLICATE_NAME,
            getRuntimeDTO().failedApplicationDTOs[0].failureReason);

        assertEquals(
            "Hello application",
            createDefaultTarget().
                path("/test-application").
                request().
                get().
                readEntity(String.class));

        assertEquals(
            404,
            createDefaultTarget().path("/test").request().get().getStatus());

        assertEquals(
            "Hello test",
            createDefaultTarget().
                path("/test-application").
                path("/test").
                request().
                get(String.class));

        serviceRegistration.unregister();

        assertEquals(0, getRuntimeDTO().applicationDTOs.length);
        assertEquals(0, getRuntimeDTO().failedApplicationDTOs.length);

        assertEquals(
            "Hello test",
            createDefaultTarget().path("/test").request().get(String.class));
    }

    @Test
    public void testApplicationShadowsDefault() {
        assertEquals(0, getRuntimeDTO().applicationDTOs.length);
        assertEquals(0, getRuntimeDTO().failedApplicationDTOs.length);

        registerAddon(new TestAddon());

        assertEquals(
            "Hello test",
            createDefaultTarget().path("/test").request().get(String.class));

        registerApplication(
            new TestApplication(), JAX_RS_APPLICATION_BASE, "/");

        assertEquals(1, getRuntimeDTO().applicationDTOs.length);
        assertEquals(1, getRuntimeDTO().failedApplicationDTOs.length);

        assertEquals(
            "Hello application",
            createDefaultTarget().request().get(String.class));
    }

    @Test
    public void testApplicationWithDedicatedExtension()
        throws InterruptedException {

        assertEquals(0, getRuntimeDTO().applicationDTOs.length);

        registerApplication(
            new TestApplication(),
            JAX_RS_EXTENSION_SELECT,
            String.format("(%s=%s)", JAX_RS_NAME, "Filter"), "propertyKey",
            "propertyValue");

        assertEquals(0, getRuntimeDTO().applicationDTOs.length);

        WebTarget webTarget = createDefaultTarget().path("/test-application");

        Response response = webTarget.request().get();

        assertEquals(404, response.getStatus());

        ServiceRegistration<?> filterRegistration = registerExtension(
            "Filter", JAX_RS_APPLICATION_SELECT, "(propertyKey=propertyValue)");

        assertEquals(1, getRuntimeDTO().applicationDTOs.length);

        response = webTarget.request().get();

        assertEquals("Hello application", response.readEntity(String.class));

        filterRegistration.unregister();

        assertEquals(0, getRuntimeDTO().applicationDTOs.length);

        response = webTarget.request().get();

        assertEquals(404, response.getStatus());
    }

    @Test
    public void testApplicationWithError() throws InterruptedException {
        RuntimeDTO runtimeDTO = getRuntimeDTO();

        assertEquals(0, runtimeDTO.applicationDTOs.length);
        assertEquals(0, runtimeDTO.failedApplicationDTOs.length);

        ServiceRegistration<?> serviceRegistration = registerApplication(
            new TestApplication() {

                @Override
                public Set<Object> getSingletons() {
                    throw new RuntimeException();
                }

            });

        runtimeDTO = getRuntimeDTO();

        assertEquals(0, runtimeDTO.applicationDTOs.length);
        assertEquals(1, runtimeDTO.failedApplicationDTOs.length);
        assertEquals(
            DTOConstants.FAILURE_REASON_UNKNOWN,
            runtimeDTO.failedApplicationDTOs[0].failureReason);

        WebTarget webTarget = createDefaultTarget().path("/test-application");

        assertEquals(404, webTarget.request().get().getStatus());

        serviceRegistration.unregister();

        runtimeDTO = getRuntimeDTO();

        assertEquals(0, runtimeDTO.applicationDTOs.length);
        assertEquals(0, runtimeDTO.failedApplicationDTOs.length);
    }

    @Test
    public void testApplicationWithErrorAndHigherRanking() {
        RuntimeDTO runtimeDTO = getRuntimeDTO();

        assertEquals(0, runtimeDTO.applicationDTOs.length);
        assertEquals(0, runtimeDTO.failedApplicationDTOs.length);

        ServiceRegistration<Application> applicationRegistration =
            registerApplication(new TestApplication());

        try {
	        runtimeDTO = getRuntimeDTO();
	
	        assertEquals(1, runtimeDTO.applicationDTOs.length);
	        assertEquals(0, runtimeDTO.failedApplicationDTOs.length);
	
	        ServiceRegistration<?> erroredRegistration = registerApplication(
	            new TestApplication() {

	                @Override
	                public Set<Object> getSingletons() {
	                    throw new RuntimeException();
	                }

	            }, "service.ranking", 10);
	
	        runtimeDTO = getRuntimeDTO();
	
	        assertEquals(0, runtimeDTO.applicationDTOs.length);
	        assertEquals(2, runtimeDTO.failedApplicationDTOs.length);
	
	        erroredRegistration.unregister();
	
	        runtimeDTO = getRuntimeDTO();
	
	        assertEquals(1, runtimeDTO.applicationDTOs.length);
	        assertEquals(0, runtimeDTO.failedApplicationDTOs.length);
	
	        WebTarget webTarget = createDefaultTarget().path("/test-application");
	
	        assertEquals(200, webTarget.request().get().getStatus());
	        assertEquals("Hello application", webTarget.request().get(String.class));
        } finally {
        	applicationRegistration.unregister();
        }
    }

    @Test
    public void testApplicationWithExtensionDryRun()
        throws InterruptedException {

        assertEquals(0, getRuntimeDTO().applicationDTOs.length);

        registerApplication(
            new TestApplication(), JAX_RS_EXTENSION_SELECT,
            String.format("(%s=%s)", JAX_RS_NAME, "Filter"));

        assertEquals(0, getRuntimeDTO().applicationDTOs.length);

        WebTarget webTarget = createDefaultTarget().path("/test-application");

        Response response = webTarget.request().get();

        assertEquals(404, response.getStatus());

        ServiceRegistration<?> filterRegistration = registerExtension(
            "Filter", JAX_RS_APPLICATION_SELECT, "(unexistent=application)");

        assertEquals(0, getRuntimeDTO().applicationDTOs.length);

        response = webTarget.request().get();

        assertEquals(404, response.getStatus());

        filterRegistration.unregister();

        assertEquals(0, getRuntimeDTO().applicationDTOs.length);

        response = webTarget.request().get();

        assertEquals(404, response.getStatus());
    }

    @Test
    public void testApplicationWithGenericExtension()
        throws InterruptedException {

        assertEquals(0, getRuntimeDTO().applicationDTOs.length);

        registerApplication(
            new TestApplication(),
            JAX_RS_EXTENSION_SELECT,
            String.format("(%s=%s)", JAX_RS_NAME, "Filter"));

        assertEquals(0, getRuntimeDTO().applicationDTOs.length);

        WebTarget webTarget = createDefaultTarget().path("/test-application");

        Response response = webTarget.request().get();

        assertEquals(404, response.getStatus());

        ServiceRegistration<?> filterRegistration = registerExtension("Filter");

        assertEquals(1, getRuntimeDTO().applicationDTOs.length);

        response = webTarget.request().get();

        assertEquals("Hello application", response.readEntity(String.class));

        filterRegistration.unregister();

        assertEquals(0, getRuntimeDTO().applicationDTOs.length);

        response = webTarget.request().get();

        assertEquals(404, response.getStatus());
    }

    @Test
    public void testApplicationWithGenericExtensionAndApplicationSelect()
        throws InterruptedException {

        assertEquals(0, getRuntimeDTO().applicationDTOs.length);

        registerApplication(
            new TestApplication(),
            JAX_RS_EXTENSION_SELECT,
            String.format("(%s=%s)", JAX_RS_NAME, "Filter"));

        registerApplication(
            new TestApplication(),
            JAX_RS_APPLICATION_BASE, "/test-application-2");

        assertEquals(1, getRuntimeDTO().applicationDTOs.length);

        WebTarget webTarget = createDefaultTarget().path("/test-application");

        Response response = webTarget.request().get();

        assertEquals(404, response.getStatus());

        ServiceRegistration<?> filterRegistration = registerExtension(
            "Filter", JAX_RS_APPLICATION_SELECT,
            "(" + JAX_RS_EXTENSION_SELECT + "=" +
                String.format("\\(%s\\=%s\\)", JAX_RS_NAME, "Filter") + ")");

        assertEquals(2, getRuntimeDTO().applicationDTOs.length);

        response = webTarget.request().get();

        assertTrue(Boolean.parseBoolean(response.getHeaderString("Filtered")));
        assertEquals("Hello application", response.readEntity(String.class));

        response = createDefaultTarget().path("/test-application-2").request().get();

        assertNull(response.getHeaderString("Filtered"));
        assertEquals("Hello application", response.readEntity(String.class));

        filterRegistration.unregister();

        assertEquals(1, getRuntimeDTO().applicationDTOs.length);

        response = webTarget.request().get();

        assertEquals(404, response.getStatus());
    }

    @Test
    /**
     * Propietary extension... not in the spec
     * breaks application isolation if several applications are forced to
     * use the same ServletContext
     */
    public void testApplicationWithProvidedServletContext() {
        ServiceRegistration<Application> applicationRegistration =
            registerApplication(
                new TestApplication(), HTTP_WHITEBOARD_CONTEXT_SELECT,
                "(" + HTTP_WHITEBOARD_CONTEXT_PATH + "=/context)");

        WebTarget webTarget = createDefaultTarget().path(
            "/test-application"
        );

        Response response = webTarget.request().get();

        assertEquals(404, response.getStatus());

        RuntimeDTO runtimeDTO = getRuntimeDTO();

        FailedApplicationDTO[] failedApplicationDTOs =
            runtimeDTO.failedApplicationDTOs;

        assertEquals(1, failedApplicationDTOs.length);
        assertEquals(
            applicationRegistration.getReference().getProperty("service.id"),
            failedApplicationDTOs[0].serviceId);
        assertEquals(
            101, failedApplicationDTOs[0].failureReason);

        ServiceRegistration<ServletContextHelper> context =
            bundleContext.registerService(
                ServletContextHelper.class, new ServletContextHelper() {},
                new Hashtable<String, Object>() {{
                    put(HTTP_WHITEBOARD_CONTEXT_PATH, "/context");
                    put(HTTP_WHITEBOARD_CONTEXT_NAME, "contextName");
                }});

        runtimeDTO = getRuntimeDTO();

        failedApplicationDTOs = runtimeDTO.failedApplicationDTOs;

        assertEquals(0, failedApplicationDTOs.length);

        try {
            webTarget = createDefaultTarget().
                path(
                    "/context"
                ).path(
                    "/test-application"
            );

            String responseString = webTarget.request().get(String.class);

            assertEquals("Hello application", responseString);
        }
        finally {
            context.unregister();
        }

    }

    @Test
    /**
     * Proprietary extension... not in the spec
     * breaks application isolation if several applications are forced to
     * use the same ServletContext
     */
    public void testApplicationWithProvidedServletContextClashes() {
        ServiceRegistration<Application> applicationRegistration =
            registerApplication(
                new TestApplication(), JAX_RS_APPLICATION_BASE,
                "/context/test-application", "service.ranking", 10);

        ServiceRegistration<ServletContextHelper> context =
            bundleContext.registerService(
                ServletContextHelper.class, new ServletContextHelper() {},
                new Hashtable<String, Object>() {{
                    put(HTTP_WHITEBOARD_CONTEXT_PATH, "/context");
                    put(HTTP_WHITEBOARD_CONTEXT_NAME, "contextName");
                }});
        try {
            ServiceRegistration<Application> applicationRegistration2 =
                registerApplication(
                    new TestApplication(), HTTP_WHITEBOARD_CONTEXT_SELECT,
                    "(" + HTTP_WHITEBOARD_CONTEXT_PATH + "=/context)");

            WebTarget webTarget = createDefaultTarget().
                path(
                    "/context"
                ).
                path(
                    "test-application"
                );

            Response response = webTarget.request().get();

            assertEquals(
                "Hello application", response.readEntity(String.class));

            RuntimeDTO runtimeDTO = getRuntimeDTO();

            FailedApplicationDTO[] failedApplicationDTOs =
                runtimeDTO.failedApplicationDTOs;

            assertEquals(1, failedApplicationDTOs.length);
            assertEquals(
                applicationRegistration2.getReference().getProperty(
                    "service.id"),
                failedApplicationDTOs[0].serviceId);
            assertEquals(
                DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE,
                failedApplicationDTOs[0].failureReason);

            Hashtable<String, Object> properties = new Hashtable<>();

            properties.put(
                HTTP_WHITEBOARD_CONTEXT_SELECT,
                "(" + HTTP_WHITEBOARD_CONTEXT_PATH + "=/context)");
            properties.put(JAX_RS_APPLICATION_BASE, "/test-application");
            properties.put("service.ranking", 20);

            applicationRegistration2.setProperties(properties);

            runtimeDTO = getRuntimeDTO();

            failedApplicationDTOs = runtimeDTO.failedApplicationDTOs;

            assertEquals(1, failedApplicationDTOs.length);
            assertEquals(
                applicationRegistration.getReference().getProperty(
                    "service.id"),
                failedApplicationDTOs[0].serviceId);
            assertEquals(
                DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE,
                failedApplicationDTOs[0].failureReason);

            webTarget = createDefaultTarget().
                path(
                    "/context"
                ).path(
                "/test-application"
            );

            String responseString = webTarget.request().get(String.class);

            assertEquals("Hello application", responseString);

            context.unregister();

            runtimeDTO = getRuntimeDTO();

            failedApplicationDTOs = runtimeDTO.failedApplicationDTOs;

            assertEquals(1, failedApplicationDTOs.length);
            assertEquals(
                applicationRegistration2.getReference().getProperty(
                    "service.id"),
                failedApplicationDTOs[0].serviceId);
            assertEquals(
                101, failedApplicationDTOs[0].failureReason);

            webTarget = createDefaultTarget().
                path(
                    "/context"
                ).path(
                    "/test-application"
                );

            responseString = webTarget.request().get(String.class);

            assertEquals("Hello application", responseString);
        }
        finally {
            try {
                context.unregister();
            }
            catch (Exception e) {

            }
        }

    }

    @Test
    public void testApplicationWithoutStartingSlash()
        throws InterruptedException {

        assertEquals(0, getRuntimeDTO().applicationDTOs.length);

        registerApplication(
            new TestApplication(), JAX_RS_APPLICATION_BASE, "test-application");

        assertEquals(1, getRuntimeDTO().applicationDTOs.length);

        WebTarget webTarget = createDefaultTarget().path("/test-application");

        Response response = webTarget.request().get();

        assertEquals("Hello application",
            response.readEntity(String.class));
    }

    @Test
    public void testAsyncResource()
        throws ExecutionException, InterruptedException {

        WebTarget webTarget =
            createDefaultTarget().path("whiteboard").path("async").
                path("HelloAsync");

        AtomicBoolean pre = new AtomicBoolean();
        AtomicBoolean post = new AtomicBoolean();

        CountDownLatch countDownLatch = new CountDownLatch(1);

        registerAddon(
            new TestAsyncResource(
                () -> pre.set(true),
                () -> {
                    post.set(true);

                    countDownLatch.countDown();
                }));

        Future<String> future = webTarget.request().async().get(
            new InvocationCallback<String>() {
                @Override
                public void completed(String s) {
                    assertTrue(pre.get());
                }

                @Override
                public void failed(Throwable throwable) {

                }
            });

        String result = future.get();

        countDownLatch.await(1, TimeUnit.MINUTES);

        assertTrue(post.get());

        assertEquals("This should say HelloAsync", "HelloAsync", result);
    }

    @Test
    public void testAsyncResourceClientWithPromises()
        throws ExecutionException, InterruptedException,
        InvocationTargetException {

        WebTarget webTarget =
            createDefaultTarget().path("whiteboard").path("async").
                path("HelloAsync");

        AtomicBoolean pre = new AtomicBoolean();
        AtomicBoolean post = new AtomicBoolean();

        CountDownLatch countDownLatch = new CountDownLatch(1);

        registerAddon(
            new TestAsyncResource(
                () -> pre.set(true),
                () -> {
                    post.set(true);

                    countDownLatch.countDown();
                }));

        Promise<String> promise =
            webTarget.
                request().
                rx(PromiseRxInvoker.class).
                get(String.class);

        String result = promise.getValue();

        countDownLatch.await(1, TimeUnit.MINUTES);

        assertTrue(post.get());

        assertEquals("This should say HelloAsync", "HelloAsync", result);
    }

    @Test
    public void testDefaultServiceReferencePropertiesAreAvailableInFeatures() {
        AtomicBoolean executed = new AtomicBoolean();
        AtomicReference<Object> propertyvalue = new AtomicReference<>();

        registerExtension(
            Feature.class, featureContext -> {
                executed.set(true);

                Map<String, Object> properties =
                    (Map<String, Object>)
                        featureContext.getConfiguration().getProperty(
                            "osgi.jaxrs.application.serviceProperties");
                propertyvalue.set(properties.get(JAX_RS_NAME));

                return false;
            }, "Feature", JAX_RS_APPLICATION_SELECT,
            "("+ JAX_RS_NAME + "=" + JAX_RS_DEFAULT_APPLICATION + ")");

        assertTrue(executed.get());
        assertEquals(JAX_RS_DEFAULT_APPLICATION, propertyvalue.get());
    }

    @Test
    public void testEndpointsOverride() {
        WebTarget webTarget = createDefaultTarget().path("conflict");

        registerAddon(new TestAddonConflict());

        Response response = webTarget.request().get();

        assertEquals(
            "This should say hello1", "hello1",
            response.readEntity(String.class));

        ServiceRegistration<?> serviceRegistration = registerAddon(
            new TestAddonConflict2(), "service.ranking", 1);

        response = webTarget.request().get();

        assertEquals(
            "This should say hello2", "hello2",
            response.readEntity(String.class));

        serviceRegistration.unregister();

        response = webTarget.request().get();

        assertEquals(
            "This should say hello1", "hello1",
            response.readEntity(String.class));
    }

    @Test
    public void testErroredExtension() {
        registerApplication(new TestApplication());

        ServiceRegistration<Feature> serviceRegistration = registerExtension(
            Feature.class,
            context -> {
                throw new RuntimeException();
            },
            "ErrorFeature",
            JAX_RS_APPLICATION_SELECT,
            "(" + JAX_RS_APPLICATION_BASE + "=/test-application)");

        RuntimeDTO runtimeDTO = _runtime.getRuntimeDTO();

        assertEquals(1, runtimeDTO.failedExtensionDTOs.length);
        assertEquals(
            serviceRegistration.getReference().getProperty("service.id"),
            runtimeDTO.failedExtensionDTOs[0].serviceId);
        assertEquals(
            DTOConstants.FAILURE_REASON_UNKNOWN,
            runtimeDTO.failedExtensionDTOs[0].failureReason);
    }

    @Test
    public void testExtensionRegisterOnlySignalledInterfaces()
        throws InterruptedException {

        WebTarget webTarget = createDefaultTarget().path("test-application");

        registerApplication(new TestApplicationWithException());

        ServiceRegistration<?> filterRegistration =
            registerMultiExtension("Filter", ExceptionMapper.class.getName());

        Response response = webTarget.request().get();

        assertEquals(200, response.getStatus());

        assertNull(response.getHeaders().getFirst("Filtered"));

        filterRegistration.unregister();
    }

    @Test
    public void testExtensionWithoutAName() {
        Dictionary<String, Object> properties = new Hashtable<>();

        properties.put(JAX_RS_EXTENSION, true);

        ServiceRegistration<ContainerResponseFilter> registration =
            bundleContext.registerService(
                ContainerResponseFilter.class, new TestFilter(), properties);

        try {
            RuntimeDTO runtimeDTO = _runtime.getRuntimeDTO();

            assertEquals(
                (long)registration.getReference().getProperty("service.id"),
                runtimeDTO.defaultApplication.extensionDTOs[0].serviceId);
        }
        finally {
            registration.unregister();
        }

    }

    @Test
    public void testFeatureExtension() {
        WebTarget webTarget = createDefaultTarget().path("/test-application");

        registerApplication(new TestApplication());

        registerExtension(
            Feature.class,
            context -> {
                context.register(new TestFilter());

                return true;
            },
            "Feature",
            JAX_RS_APPLICATION_SELECT,
            "(" + JAX_RS_APPLICATION_BASE + "=/test-application)");

        Response response = webTarget.request().get();

        assertEquals("Hello application", response.readEntity(String.class));

        assertEquals("true", response.getHeaders().getFirst("Filtered"));
    }

    @Test
    public void testGettableAndNotGettableApplication()
        throws InterruptedException {

        WebTarget webTarget = createDefaultTarget().path("test-application");

        assertEquals(0, getRuntimeDTO().applicationDTOs.length);
        assertEquals(0, getRuntimeDTO().failedApplicationDTOs.length);

        ServiceRegistration<Application> serviceRegistration =
            registerApplication(new TestApplication());

        assertEquals(1, getRuntimeDTO().applicationDTOs.length);
        assertEquals(0, getRuntimeDTO().failedApplicationDTOs.length);

        assertEquals(
            "Hello application",
            webTarget.request().get().readEntity(String.class));

        ServiceRegistration<Application> ungettableServiceRegistration =
            registerUngettableApplication("service.ranking", 1);

        assertEquals(0, getRuntimeDTO().applicationDTOs.length);
        assertEquals(2, getRuntimeDTO().failedApplicationDTOs.length);

        assertThatInRuntime(
            FAILED_APPLICATIONS,
            fa -> fa.serviceId == getServiceId(ungettableServiceRegistration) &&
                DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE ==
                fa.failureReason);

        assertEquals(404, webTarget.request().get().getStatus());

        serviceRegistration.unregister();

        assertEquals(0, getRuntimeDTO().applicationDTOs.length);
        assertEquals(1, getRuntimeDTO().failedApplicationDTOs.length);

        assertEquals(404, webTarget.request().get().getStatus());

        ungettableServiceRegistration.unregister();

        assertEquals(0, getRuntimeDTO().applicationDTOs.length);
        assertEquals(0, getRuntimeDTO().failedApplicationDTOs.length);
    }

    @Test
    public void testGettableAndShadowedNotGettableApplication()
        throws InterruptedException {

        WebTarget webTarget = createDefaultTarget().path("test-application");

        assertEquals(0, getRuntimeDTO().applicationDTOs.length);
        assertEquals(0, getRuntimeDTO().failedApplicationDTOs.length);

        ServiceRegistration<Application> serviceRegistration =
            registerApplication(new TestApplication());

        assertEquals(1, getRuntimeDTO().applicationDTOs.length);
        assertEquals(0, getRuntimeDTO().failedApplicationDTOs.length);

        assertEquals(
            "Hello application",
            webTarget.request().get().readEntity(String.class));

        ServiceRegistration<Application> ungettableServiceRegistration =
            registerUngettableApplication("service.ranking", -1);

        assertEquals(1, getRuntimeDTO().applicationDTOs.length);
        assertEquals(1, getRuntimeDTO().failedApplicationDTOs.length);

        assertEquals(
            DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE,
            getRuntimeDTO().failedApplicationDTOs[0].failureReason);

        assertEquals(
            "Hello application",
            webTarget.request().get().readEntity(String.class));

        serviceRegistration.unregister();

        assertEquals(0, getRuntimeDTO().applicationDTOs.length);
        assertEquals(1, getRuntimeDTO().failedApplicationDTOs.length);

        assertEquals(404, webTarget.request().get().getStatus());

        ungettableServiceRegistration.unregister();

        assertEquals(0, getRuntimeDTO().applicationDTOs.length);
        assertEquals(0, getRuntimeDTO().failedApplicationDTOs.length);
    }

    @Test
    public void testInvalidExtension() throws InterruptedException {
        WebTarget webTarget = createDefaultTarget().path("test");

        RuntimeDTO runtimeDTO = getRuntimeDTO();

        assertEquals(0, runtimeDTO.defaultApplication.extensionDTOs.length);

        registerAddon(new TestAddon());

        ServiceRegistration<?> filterRegistration = registerInvalidExtension(
            "Filter");

        runtimeDTO = getRuntimeDTO();

        assertEquals(0, runtimeDTO.defaultApplication.extensionDTOs.length);

        assertEquals(1, runtimeDTO.failedExtensionDTOs.length);
        assertEquals(
            (long)filterRegistration.getReference().getProperty(
                "service.id"),
            runtimeDTO.failedExtensionDTOs[0].serviceId);
        assertEquals(
            DTOConstants.FAILURE_REASON_NOT_AN_EXTENSION_TYPE,
            runtimeDTO.failedExtensionDTOs[0].failureReason);

        Response response = webTarget.request().get();

        assertEquals(
            "This should say hello", "Hello test",
            response.readEntity(String.class));

        assertNull(response.getHeaders().getFirst("Filtered"));

        filterRegistration.unregister();

        runtimeDTO = getRuntimeDTO();

        assertEquals(0, runtimeDTO.defaultApplication.extensionDTOs.length);

        assertEquals(0, runtimeDTO.failedExtensionDTOs.length);
    }

    @Test
    public void testNotGettableApplication() throws InterruptedException {
        assertEquals(0, getRuntimeDTO().applicationDTOs.length);
        assertEquals(0, getRuntimeDTO().failedApplicationDTOs.length);

        ServiceRegistration<Application> serviceRegistration =
            registerUngettableApplication();

        assertEquals(0, getRuntimeDTO().applicationDTOs.length);
        assertEquals(1, getRuntimeDTO().failedApplicationDTOs.length);

        assertEquals(
            DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE,
            getRuntimeDTO().failedApplicationDTOs[0].failureReason);

        serviceRegistration.unregister();

        assertEquals(0, getRuntimeDTO().applicationDTOs.length);
        assertEquals(0, getRuntimeDTO().failedApplicationDTOs.length);
    }

    @Test
    public void testRegisterApplicationWithOnlyExtensions() {
        ServiceRegistration<Application> serviceRegistration =
            registerApplication(new Application() {
                @Override
                public Set<Class<?>> getClasses() {
                    return Collections.singleton(TestFilter.class);
                }
            }, JAX_RS_NAME, JAX_RS_DEFAULT_APPLICATION);

        assertEquals(getServiceId(serviceRegistration),
            getRuntimeDTO().defaultApplication.serviceId);
    }

    @Test
    public void testResourcesChangeCount() throws Exception {
        Long changeCount = (Long)_runtimeServiceReference.getProperty(
            "service.changecount");

        ServiceRegistration<?> serviceRegistration = registerAddon(
            new TestAddon());

        Long newCount = (Long)_runtimeServiceReference.getProperty(
            "service.changecount");

        assertTrue(changeCount < newCount);

        changeCount = newCount;

        ServiceRegistration<?> serviceRegistration2 = registerAddon(
            new TestAddon());

        newCount = (Long)_runtimeServiceReference.getProperty(
            "service.changecount");

        assertTrue(changeCount < newCount);

        changeCount = newCount;

        serviceRegistration.unregister();

        newCount = (Long)_runtimeServiceReference.getProperty(
            "service.changecount");

        assertTrue(changeCount < newCount);

        changeCount = newCount;

        serviceRegistration2.unregister();

        newCount = (Long)_runtimeServiceReference.getProperty(
            "service.changecount");

        assertTrue(changeCount < newCount);
    }

    @Test
    public void testServiceReferencePropertiesAreAvailableInConfigurationInjection() {
        registerApplication(
            new Application() {
                @Override
                public Set<Object> getSingletons() {
                    return new HashSet<>(Collections.singleton(
                        new ConfigurationAwareResource()
                    ));
                }
            }, JAX_RS_NAME, "test", "property", "aValue");

        WebTarget webTarget = createDefaultTarget().path("test-application");

        assertEquals("aValue", webTarget.request().get(String.class));
    }

    @Test
    public void testServiceReferencePropertiesAreAvailableInFeatures() {
        AtomicBoolean executed = new AtomicBoolean();
        AtomicReference<Object> propertyvalue = new AtomicReference<>();

        registerExtension(
            Feature.class, featureContext -> {
                executed.set(true);

                Map<String, Object> properties =
                    (Map<String, Object>)
                        featureContext.getConfiguration().getProperty(
                            "osgi.jaxrs.application.serviceProperties");
                propertyvalue.set(properties.get("property"));

                return false;
            }, "Feature", JAX_RS_APPLICATION_SELECT, "(property=true)");

        registerApplication(
            new Application() {
            @Override
            public Set<Object> getSingletons() {
                return Collections.singleton(
                    new Object() {

                        @GET
                        public String hello() {
                            return "hello";
                        }
                    });
            }
        }, JAX_RS_NAME, "test", "property", true);

        assertTrue(executed.get());
        assertEquals(true, propertyvalue.get());
    }

    @Test
    public void testServiceReferencePropertiesAreAvailableInStaticFeatures() {
        AtomicBoolean executed = new AtomicBoolean();
        AtomicReference<Object> propertyvalue = new AtomicReference<>();

        registerApplication(
            new Application() {
                @Override
                public Set<Object> getSingletons() {
                    return new HashSet<>(Arrays.asList(
                        (Feature)featureContext -> {
                            executed.set(true);

                            Map<String, Object> properties =
                                (Map<String, Object>)
                                    featureContext.getConfiguration().getProperty(
                                        "osgi.jaxrs.application.serviceProperties");
                            propertyvalue.set(properties.get("property"));

                            return false;
                        },
                        new Object() {

                            @GET
                            public String hello() {
                                return "hello";
                            }
                        }
                        ));
                }
            }, JAX_RS_NAME, "test", "property", true);

        assertTrue(executed.get());
        assertEquals(true, propertyvalue.get());
    }

    @Test
    public void testStandaloneEndPoint() throws InterruptedException {
        WebTarget webTarget = createDefaultTarget().path("test");

        getRuntimeDTO();

        registerAddon(new TestAddon());

        Response response = webTarget.request().get();

        assertEquals(
            "This should say hello", "Hello test",
            response.readEntity(String.class));
    }

    @Test
    public void testStandaloneEndPointPrototypeLifecycle() {
        WebTarget webTarget =
            createDefaultTarget().
            path("/test-addon-lifecycle");

        registerAddonLifecycle(false, JAX_RS_RESOURCE, "true");

        String first = webTarget.request().get().readEntity(String.class);

        String second = webTarget.request().get().readEntity(String.class);

        assertNotEquals("This should be different", first, second);
    }

    @Test
    public void testStandaloneEndPointReadd() {
        WebTarget webTarget = createDefaultTarget().path("test");

        Runnable testCase = () -> {
            assertEquals(404, webTarget.request().get().getStatus());

            ServiceRegistration<?> serviceRegistration = null;

            try {
                serviceRegistration = registerAddon(new TestAddon());

                assertEquals(
                    "Hello test",
                    webTarget.request().get().readEntity(String.class));
            }
            finally {
                if (serviceRegistration != null) {
                    serviceRegistration.unregister();
                }
            }
        };

        testCase.run();

        testCase.run();
    }

    @Test
    public void testStandaloneEndPointSingletonLifecycle() {
        WebTarget webTarget =
            createDefaultTarget().
                path("/test-addon-lifecycle");

        registerAddonLifecycle(true, JAX_RS_RESOURCE, "true");

        String first = webTarget.request().get().readEntity(String.class);

        String second = webTarget.request().get().readEntity(String.class);

        assertEquals("This should be equal", first, second);
    }

    @Test
    public void testStandaloneEndpointWithExtensionsDependencies()
        throws InterruptedException {

        WebTarget webTarget = createDefaultTarget().path("test");

        JaxrsServiceRuntime runtime = getJaxrsServiceRuntime();

        ServiceRegistration<?> serviceRegistration;
        ServiceRegistration<?> extensionRegistration1;
        ServiceRegistration<?> extensionRegistration2;

        serviceRegistration = registerAddon(
            new TestAddon(),
            JAX_RS_EXTENSION_SELECT, new String[]{
                "(property one=one)",
                "(property two=two)",
            });

        RuntimeDTO runtimeDTO = runtime.getRuntimeDTO();

        assertEquals(1, runtimeDTO.failedResourceDTOs.length);
        assertEquals(
            (long)serviceRegistration.getReference().getProperty(
                "service.id"),
            runtimeDTO.failedResourceDTOs[0].serviceId);

        assertEquals(404, webTarget.request().get().getStatus());

        extensionRegistration1 = registerExtension(
            "aExtension", "property one", "one");

        runtimeDTO = runtime.getRuntimeDTO();

        assertEquals(1, runtimeDTO.failedResourceDTOs.length);
        assertEquals(
            (long)serviceRegistration.getReference().getProperty(
                "service.id"),
            runtimeDTO.failedResourceDTOs[0].serviceId);

        assertEquals(404, webTarget.request().get().getStatus());

        extensionRegistration2 = registerExtension(
            "anotherExtension", "property two", "two");

        runtimeDTO = runtime.getRuntimeDTO();

        assertEquals(0, runtimeDTO.failedResourceDTOs.length);

        Response response = webTarget.request().get();

        assertEquals(
            "This should say hello", "Hello test",
            response.readEntity(String.class));

        extensionRegistration1.unregister();

        runtimeDTO = runtime.getRuntimeDTO();
        assertEquals(1, runtimeDTO.failedResourceDTOs.length);
        assertEquals(
            (long)serviceRegistration.getReference().getProperty(
                "service.id"),
            runtimeDTO.failedResourceDTOs[0].serviceId);

        assertEquals(404, webTarget.request().get().getStatus());

        extensionRegistration1 = registerExtension(
            "aExtension", "property one", "one");

        runtimeDTO = runtime.getRuntimeDTO();
        assertEquals(0, runtimeDTO.failedResourceDTOs.length);

        assertEquals(
            "This should say hello", "Hello test",
            response.readEntity(String.class));

        extensionRegistration2.unregister();

        runtimeDTO = runtime.getRuntimeDTO();
        assertEquals(1, runtimeDTO.failedResourceDTOs.length);
        assertEquals(
            (long)serviceRegistration.getReference().getProperty(
                "service.id"),
            runtimeDTO.failedResourceDTOs[0].serviceId);

        assertEquals(404, webTarget.request().get().getStatus());

        extensionRegistration1.unregister();

        runtimeDTO = runtime.getRuntimeDTO();
        assertEquals(1, runtimeDTO.failedResourceDTOs.length);
        assertEquals(
            (long)serviceRegistration.getReference().getProperty(
                "service.id"),
            runtimeDTO.failedResourceDTOs[0].serviceId);
    }

    @Test
    public void testStandaloneFilter() throws InterruptedException {
        WebTarget webTarget = createDefaultTarget().path("test");

        JaxrsServiceRuntime runtime = getJaxrsServiceRuntime();

        RuntimeDTO runtimeDTO = runtime.getRuntimeDTO();

        assertEquals(0, runtimeDTO.defaultApplication.extensionDTOs.length);

        registerAddon(new TestAddon());

        ServiceRegistration<?> filterRegistration = registerExtension("Filter");

        runtimeDTO = runtime.getRuntimeDTO();

        assertEquals(1, runtimeDTO.defaultApplication.extensionDTOs.length);

        Response response = webTarget.request().get();

        assertEquals(
            "This should say hello", "Hello test",
            response.readEntity(String.class));

        assertEquals("true", response.getHeaders().getFirst("Filtered"));

        filterRegistration.unregister();

        runtimeDTO = runtime.getRuntimeDTO();

        assertEquals(0, runtimeDTO.defaultApplication.extensionDTOs.length);
    }

    @Test
    public void testStandaloneFilterReadd() {
        WebTarget webTarget = createDefaultTarget().path("test");

        registerAddon(new TestAddon());

        assertEquals("Hello test",
            webTarget.request().get().readEntity(String.class));

        Runnable testCase = () -> {
            ServiceRegistration<?> filterRegistration = null;

            try {
                Response response = webTarget.request().get();

                assertNull(response.getHeaders().getFirst("Filtered"));

                filterRegistration = registerExtension("Filter");

                response = webTarget.request().get();

                assertEquals(
                    "Hello test", response.readEntity(String.class));

                assertEquals(
                    "true", response.getHeaders().getFirst("Filtered"));
            }
            finally {
                if (filterRegistration != null) {
                    filterRegistration.unregister();
                }
            }
        };

        testCase.run();

        testCase.run();
    }

    @Test
    public void testUngettableExtension() throws InterruptedException {
        WebTarget webTarget = createDefaultTarget().path("test");

        JaxrsServiceRuntime runtime = getJaxrsServiceRuntime();

        RuntimeDTO runtimeDTO = runtime.getRuntimeDTO();

        assertEquals(0, runtimeDTO.defaultApplication.extensionDTOs.length);

        registerAddon(new TestAddon());

        ServiceRegistration<?> filterRegistration =
            registerUngettableExtension("Filter");

        runtimeDTO = runtime.getRuntimeDTO();

        assertEquals(0, runtimeDTO.defaultApplication.extensionDTOs.length);

        assertEquals(1, runtimeDTO.failedExtensionDTOs.length);
        assertEquals(
            (long)filterRegistration.getReference().getProperty(
                "service.id"),
            runtimeDTO.failedExtensionDTOs[0].serviceId);
        assertEquals(
            DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE,
            runtimeDTO.failedExtensionDTOs[0].failureReason);

        Response response = webTarget.request().get();

        assertEquals(
            "This should say hello", "Hello test",
            response.readEntity(String.class));

        assertNull(response.getHeaders().getFirst("Filtered"));

        filterRegistration.unregister();

        runtimeDTO = runtime.getRuntimeDTO();

        assertEquals(0, runtimeDTO.defaultApplication.extensionDTOs.length);

        assertEquals(0, runtimeDTO.failedExtensionDTOs.length);
    }

    @Test
    public void testSSEApplication() throws 
        InterruptedException, MalformedURLException {
        
        registerApplication(
            new TestSSEApplication(), JAX_RS_APPLICATION_BASE, "/sse");

        SseEventSourceFactory sseFactory = createSseFactory();

        SseEventSource source1 = sseFactory.newSource(
            createDefaultTarget().path("/sse").path("/subscribe"));

        SseEventSource source2 = sseFactory.newSource(
                createDefaultTarget().path("/sse").path("/subscribe"));

        ArrayList<String> source1Events = new ArrayList<>();
        ArrayList<String> source2Events = new ArrayList<>();

        source1.register(event -> source1Events.add(event.readData(String.class)));
        source2.register(event -> source2Events.add(event.readData(String.class)));

        source1.open();
        source2.open();

        WebTarget broadcast = createDefaultTarget().path("/sse").path(
            "/broadcast");

        broadcast.request().post(
            Entity.entity("message", MediaType.TEXT_PLAIN_TYPE));

        source2.close();

        assertEquals(Arrays.asList("welcome", "message"), source1Events);
        assertEquals(Arrays.asList("welcome", "message"), source2Events);

        broadcast.request().post(
            Entity.entity("another message", MediaType.TEXT_PLAIN_TYPE));

        assertEquals(
            Arrays.asList("welcome", "message", "another message"),
            source1Events);
        assertEquals(Arrays.asList("welcome", "message"), source2Events);

        source1.close();
    }

    @Test
    public void testCxfExtension() {
        registerApplication(new TestApplication());

        registerExtension(
            ContextProvider.class,
            new BusContextProvider(),
            "Bus provider");

        registerAddon(
            new CxfExtensionTestAddon(), JAX_RS_APPLICATION_SELECT,
            String.format(
                "(%s=%s)", JAX_RS_APPLICATION_BASE, "/test-application"));

        ServiceRegistration<?> extensionRegistration =
            bundleContext.registerService(
                new String[]{
                    ExtensionA.class.getName(),
                    ExtensionB.class.getName()
                },
                new TestCxfExtension(),
                new Hashtable<String, Object>() {{
                    put("cxf.extension", Boolean.TRUE);
                }});

        try {
            WebTarget path = createDefaultTarget().
                path("/test-application").
                path("/extensions").
                path(ExtensionA.class.getName());

            String result = path.request().get(String.class);

            assertTrue(Boolean.parseBoolean(result));

            path = createDefaultTarget().
                path("/test-application").
                path("/extensions").
                path(ExtensionB.class.getName());

            result = path.request().get(String.class);

            assertTrue(Boolean.parseBoolean(result));

            extensionRegistration.unregister();

            path = createDefaultTarget().
                path("/test-application").
                path("/extensions").
                path(ExtensionA.class.getName());

            result = path.request().get(String.class);

            assertFalse(Boolean.parseBoolean(result));

            path = createDefaultTarget().
                path("/test-application").
                path("/extensions").
                path(ExtensionB.class.getName());

            result = path.request().get(String.class);

            assertFalse(Boolean.parseBoolean(result));
        }
        finally {
            try {
                extensionRegistration.unregister();
            }
            catch (Exception e) {
            }
        }

    }

    private static Function<RuntimeDTO, FailedApplicationDTO[]>
        FAILED_APPLICATIONS = r -> r.failedApplicationDTOs;

    @Provider
    private static class BusContextProvider implements ContextProvider<Bus> {
        @Override
        public Bus createContext(Message message) {
            return message.getExchange().getBus();
        }
    }

}
