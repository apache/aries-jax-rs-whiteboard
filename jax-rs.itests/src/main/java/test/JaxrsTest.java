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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import org.osgi.service.jaxrs.runtime.JaxRSServiceRuntime;
import org.osgi.service.jaxrs.runtime.dto.DTOConstants;
import org.osgi.service.jaxrs.runtime.dto.RuntimeDTO;
import org.osgi.util.tracker.ServiceTracker;
import test.types.TestAddon;
import test.types.TestAddonConflict;
import test.types.TestAddonConflict2;
import test.types.TestAddonLifecycle;
import test.types.TestApplication;
import test.types.TestApplicationConflict;
import test.types.TestApplicationWithException;
import test.types.TestFilter;
import test.types.TestFilterAndExceptionMapper;
import test.types.TestHelper;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

public class JaxrsTest extends TestHelper {

    public static final long SERVICE_TIMEOUT = 15000L;
    private ServiceTracker<JaxRSServiceRuntime, JaxRSServiceRuntime>
        _runtimeTracker;

    private Collection<ServiceRegistration<?>> _registrations =
        new ArrayList<>();

    @After
    public void tearDown() {
        Iterator<ServiceRegistration<?>> iterator = _registrations.iterator();

        while (iterator.hasNext()) {
            ServiceRegistration<?> registration =  iterator.next();

            try {
                registration.unregister();

                iterator.remove();
            }
            catch(Exception e) {
            }
        }

        if (_runtimeTracker != null) {
            _runtimeTracker.close();
        }
    }

    @Test
    public void testApplication() throws InterruptedException {

        JaxRSServiceRuntime runtime = getJaxRSServiceRuntime();

        assertNotNull(runtime);

        assertEquals(0, runtime.getRuntimeDTO().applicationDTOs.length);

        registerApplication(new TestApplication());

        assertEquals(1, runtime.getRuntimeDTO().applicationDTOs.length);

        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("/test-application");

        Response response = webTarget.request().get();

        assertEquals("Hello application",
            response.readEntity(String.class));
    }

    @Test
    public void testApplicationWithError() throws InterruptedException {
        JaxRSServiceRuntime runtime = getJaxRSServiceRuntime();

        assertNotNull(runtime);

        RuntimeDTO runtimeDTO = runtime.getRuntimeDTO();

        assertEquals(0, runtimeDTO.applicationDTOs.length);
        assertEquals(0, runtimeDTO.failedExtensionDTOs.length);

        ServiceRegistration<?> serviceRegistration = registerApplication(
            new TestApplication() {

                @Override
                public Set<Object> getSingletons() {
                    throw new RuntimeException();
                }

            });

        runtimeDTO = runtime.getRuntimeDTO();

        assertEquals(0, runtimeDTO.applicationDTOs.length);
        assertEquals(1, runtimeDTO.failedApplicationDTOs.length);
        assertEquals(
            DTOConstants.FAILURE_REASON_UNKNOWN,
            runtimeDTO.failedApplicationDTOs[0].failureReason);

        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("/test-application");

        assertEquals(404, webTarget.request().get().getStatus());

        serviceRegistration.unregister();

        runtimeDTO = runtime.getRuntimeDTO();

        assertEquals(0, runtimeDTO.applicationDTOs.length);
        assertEquals(0, runtimeDTO.failedApplicationDTOs.length);
    }

    @Test
    public void testApplicationConflict() {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("test-application");

        ServiceRegistration<?> serviceRegistration2;

        registerApplication(new TestApplication());

        Response response = webTarget.request().get();

        assertEquals(
            "Hello application",
            response.readEntity(String.class));

        serviceRegistration2 = registerApplication(
            new TestApplicationConflict(), "service.ranking", -1);

        response = webTarget.request().get();

        assertEquals(
            "Hello application", response.readEntity(String.class));

        assertEquals(
            Response.Status.NOT_FOUND.getStatusCode(),
            webTarget.path("conflict").request().get().getStatus());

        serviceRegistration2.unregister();

        response = webTarget.request().get();

        assertEquals(
            "Hello application", response.readEntity(String.class));
    }

    @Test
    public void testApplicationEndpointExtension() {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("/test-application").
            path("extended");

        registerApplication(new TestApplication());

        registerAddon(
            new TestAddon(), JAX_RS_APPLICATION_SELECT,
            "(" + JAX_RS_APPLICATION_BASE + "=/test-application)");

        assertEquals(
            "Hello extended",
            webTarget.request().get().readEntity(String.class));
    }

    @Test
    public void testApplicationEndpointExtensionReadd()
        throws InterruptedException {

        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
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
    public void testApplicationOverride() throws InterruptedException {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("test-application");

        JaxRSServiceRuntime runtime = getJaxRSServiceRuntime();

        ServiceRegistration<?> serviceRegistration2;

        assertEquals(0, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(0, runtime.getRuntimeDTO().failedApplicationDTOs.length);

        registerApplication(new TestApplication());

        assertEquals(1, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(0, runtime.getRuntimeDTO().failedApplicationDTOs.length);

        Response response = webTarget.request().get();

        assertEquals(
            "Hello application",
            response.readEntity(String.class));

        serviceRegistration2 = registerApplication(
            new TestApplicationConflict(), "service.ranking", 1);

        assertEquals(1, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(1, runtime.getRuntimeDTO().failedApplicationDTOs.length);

        response = webTarget.request().get();

        assertEquals(
            "Hello application conflict",
            response.readEntity(String.class));

        assertEquals(
            "conflict",
            webTarget.path("conflict").request().get(String.class));

        serviceRegistration2.unregister();

        assertEquals(1, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(0, runtime.getRuntimeDTO().failedApplicationDTOs.length);

        response = webTarget.request().get();

        assertEquals(
            "Hello application", response.readEntity(String.class));
    }

    @Test
    public void testApplicationProviderExtension() {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("/test-application");

        registerApplication(new TestApplication());

        registerExtension(
            "filter", JAX_RS_APPLICATION_SELECT,
            "(" + JAX_RS_APPLICATION_BASE + "=/test-application)");

        Response response = webTarget.request().get();

        assertEquals(
            "Hello application",
            response.readEntity(String.class));

        assertEquals("true", response.getHeaders().getFirst("Filtered"));
    }

    @Test
    public void testApplicationProviderExtensionReadd() {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("/test-application");

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
                    response.getHeaders().getFirst("Filtered"),
                    "true");
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
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("/test-application");

        JaxRSServiceRuntime runtime = getJaxRSServiceRuntime();

        Runnable testCase = () -> {
            int applications = runtime.getRuntimeDTO().applicationDTOs.length;

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
                    runtime.getRuntimeDTO().applicationDTOs.length);
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
    public void testApplicationChangeCount() throws Exception {
        ServiceTracker<JaxRSServiceRuntime, JaxRSServiceRuntime>
            runtimeTracker = new ServiceTracker<>(
                bundleContext, JaxRSServiceRuntime.class, null);

        try {
            runtimeTracker.open();

            JaxRSServiceRuntime runtime = runtimeTracker.waitForService(
                SERVICE_TIMEOUT);

            assertNotNull(runtime);

            ServiceReference<JaxRSServiceRuntime> serviceReference =
                runtimeTracker.getServiceReference();

            Long changeCount = (Long)serviceReference.getProperty(
                "service.changecount");

            Dictionary<String, Object> properties = new Hashtable<>();

            properties.put(JAX_RS_APPLICATION_BASE, "/test-counter");

            ServiceRegistration<?> serviceRegistration =
                bundleContext.registerService(
                    Application.class, new TestApplication(), properties);

            Long newCount = (Long)serviceReference.getProperty(
                "service.changecount");

            assertTrue(changeCount < newCount);

            changeCount = newCount;

            serviceRegistration.unregister();

            newCount = (Long)serviceReference.getProperty(
                "service.changecount");

            assertTrue(changeCount < newCount);
        }
        finally {
            runtimeTracker.close();
        }
    }

    @Test
    public void testResourcesChangeCount() throws Exception {
        ServiceTracker<JaxRSServiceRuntime, JaxRSServiceRuntime>
            runtimeTracker = new ServiceTracker<>(
                bundleContext, JaxRSServiceRuntime.class, null);

        try {
            runtimeTracker.open();

            JaxRSServiceRuntime runtime = runtimeTracker.waitForService(15000L);

            assertNotNull(runtime);

            ServiceReference<JaxRSServiceRuntime> serviceReference =
                runtimeTracker.getServiceReference();

            Long changeCount = (Long)serviceReference.getProperty(
                "service.changecount");

            ServiceRegistration<?> serviceRegistration =
                registerAddon(new TestAddon());

            Long newCount = (Long)serviceReference.getProperty(
                "service.changecount");

            assertTrue(changeCount < newCount);

            changeCount = newCount;

            ServiceRegistration<?> serviceRegistration2 =
                registerAddon(new TestAddon());

            newCount = (Long)serviceReference.getProperty(
                "service.changecount");

            assertTrue(changeCount < newCount);

            changeCount = newCount;

            serviceRegistration.unregister();

            newCount = (Long)serviceReference.getProperty(
                "service.changecount");

            assertTrue(changeCount < newCount);

            changeCount = newCount;

            serviceRegistration2.unregister();

            newCount = (Long)serviceReference.getProperty(
                "service.changecount");

            assertTrue(changeCount < newCount);
        }
        finally {
            runtimeTracker.close();
        }
    }

    @Test
    public void testEndpointsOverride() {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("conflict");

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
    public void testGettableAndNotGettableApplication()
        throws InterruptedException {

        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("test-application");

        JaxRSServiceRuntime runtime = getJaxRSServiceRuntime();

        assertEquals(0, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(0, runtime.getRuntimeDTO().failedApplicationDTOs.length);

        ServiceRegistration<Application> serviceRegistration =
            registerApplication(new TestApplication());

        assertEquals(1, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(0, runtime.getRuntimeDTO().failedApplicationDTOs.length);

        assertEquals(
            "Hello application",
            webTarget.request().get().readEntity(String.class));

        ServiceRegistration<Application> ungettableServiceRegistration =
            registerUngettableApplication("service.ranking", 1);

        assertEquals(1, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(1, runtime.getRuntimeDTO().failedApplicationDTOs.length);

        assertEquals(
            DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE,
            runtime.getRuntimeDTO().failedApplicationDTOs[0].failureReason);

        assertEquals(
            "Hello application",
            webTarget.request().get().readEntity(String.class));

        serviceRegistration.unregister();

        assertEquals(0, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(1, runtime.getRuntimeDTO().failedApplicationDTOs.length);

        assertEquals(404, webTarget.request().get().getStatus());

        ungettableServiceRegistration.unregister();

        assertEquals(0, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(0, runtime.getRuntimeDTO().failedApplicationDTOs.length);
    }

    @Test
    public void testGettableAndShadowedNotGettableApplication()
        throws InterruptedException {

        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("test-application");

        JaxRSServiceRuntime runtime = getJaxRSServiceRuntime();

        assertEquals(0, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(0, runtime.getRuntimeDTO().failedApplicationDTOs.length);

        ServiceRegistration<Application> serviceRegistration =
            registerApplication(new TestApplication());

        assertEquals(1, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(0, runtime.getRuntimeDTO().failedApplicationDTOs.length);

        assertEquals(
            "Hello application",
            webTarget.request().get().readEntity(String.class));

        ServiceRegistration<Application> ungettableServiceRegistration =
            registerUngettableApplication("service.ranking", -1);

        assertEquals(1, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(1, runtime.getRuntimeDTO().failedApplicationDTOs.length);

        assertEquals(
            DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE,
            runtime.getRuntimeDTO().failedApplicationDTOs[0].failureReason);

        assertEquals(
            "Hello application",
            webTarget.request().get().readEntity(String.class));

        serviceRegistration.unregister();

        assertEquals(0, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(1, runtime.getRuntimeDTO().failedApplicationDTOs.length);

        assertEquals(404, webTarget.request().get().getStatus());

        ungettableServiceRegistration.unregister();

        assertEquals(0, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(0, runtime.getRuntimeDTO().failedApplicationDTOs.length);
    }

    @Test
    public void testNotGettableApplication() throws InterruptedException {
        JaxRSServiceRuntime runtime = getJaxRSServiceRuntime();

        assertEquals(0, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(0, runtime.getRuntimeDTO().failedApplicationDTOs.length);

        ServiceRegistration<Application> serviceRegistration =
            registerUngettableApplication();

        assertEquals(0, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(1, runtime.getRuntimeDTO().failedApplicationDTOs.length);

        assertEquals(
            DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE,
            runtime.getRuntimeDTO().failedApplicationDTOs[0].failureReason);

        serviceRegistration.unregister();

        assertEquals(0, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(0, runtime.getRuntimeDTO().failedApplicationDTOs.length);
    }

    @Test
    public void testStandaloneEndPoint() throws InterruptedException {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("test");

        JaxRSServiceRuntime runtime = getJaxRSServiceRuntime();

        runtime.getRuntimeDTO();

        registerAddon(new TestAddon());

        Response response = webTarget.request().get();

        assertEquals(
            "This should say hello", "Hello test",
            response.readEntity(String.class));
    }

    @Test
    public void testStandaloneEndPointPrototypeLifecycle() {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("/test-addon-lifecycle");

        registerAddonLifecycle(false, JAX_RS_RESOURCE, "true");

        String first = webTarget.request().get().readEntity(String.class);

        String second = webTarget.request().get().readEntity(String.class);

        assertNotEquals("This should be different", first, second);
    }

    @Test
    public void testStandaloneEndPointReadd() {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("test");

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
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("/test-addon-lifecycle");

        registerAddonLifecycle(true, JAX_RS_RESOURCE, "true");

        String first = webTarget.request().get().readEntity(String.class);

        String second = webTarget.request().get().readEntity(String.class);

        assertEquals("This should be equal", first, second);
    }

    @Test
    public void testStandaloneEndpointWithExtensionsDependencies()
        throws InterruptedException {

        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("test");

        JaxRSServiceRuntime runtime = getJaxRSServiceRuntime();

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
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("test");

        JaxRSServiceRuntime runtime = getJaxRSServiceRuntime();

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
    public void testInvalidExtension() throws InterruptedException {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("test");

        JaxRSServiceRuntime runtime = getJaxRSServiceRuntime();

        RuntimeDTO runtimeDTO = runtime.getRuntimeDTO();

        assertEquals(
            0, runtimeDTO.defaultApplication.extensionDTOs.length);

        registerAddon(new TestAddon());

        ServiceRegistration<?> filterRegistration =
            registerInvalidExtension("Filter");

        runtimeDTO = runtime.getRuntimeDTO();

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

        runtimeDTO = runtime.getRuntimeDTO();

        assertEquals(0, runtimeDTO.defaultApplication.extensionDTOs.length);

        assertEquals(0, runtimeDTO.failedExtensionDTOs.length);
    }

    @Test
    public void testExtensionRegisterOnlySignalledInterfaces()
        throws InterruptedException {

        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("test-application");

        registerApplication(new TestApplicationWithException());

        ServiceRegistration<?> filterRegistration =
            registerMultiExtension("Filter", ExceptionMapper.class.getName());

        Response response = webTarget.request().get();

        assertEquals(200, response.getStatus());

        assertNull(response.getHeaders().getFirst("Filtered"));

        filterRegistration.unregister();
    }

    @Test
    public void testUngettableExtension() throws InterruptedException {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("test");

        JaxRSServiceRuntime runtime = getJaxRSServiceRuntime();

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
    public void testStandaloneFilterReadd() {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("test");

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

    private JaxRSServiceRuntime getJaxRSServiceRuntime()
        throws InterruptedException {

        _runtimeTracker = new ServiceTracker<>(
            bundleContext, JaxRSServiceRuntime.class, null);

        _runtimeTracker.open();

        return _runtimeTracker.waitForService(15000L);
    }

    private ServiceRegistration<?> registerAddon(
        Object instance, Object... keyValues) {

        Dictionary<String, Object> properties = new Hashtable<>();

        properties.put(JAX_RS_RESOURCE, "true");

        for (int i = 0; i < keyValues.length; i = i + 2) {
            properties.put(keyValues[i].toString(), keyValues[i + 1]);
        }

        ServiceRegistration<Object> serviceRegistration =
            bundleContext.registerService(Object.class, instance, properties);

        _registrations.add(serviceRegistration);

        return serviceRegistration;
    }

    private ServiceRegistration<?> registerAddonLifecycle(
        boolean singleton, Object... keyValues) {

        Dictionary<String, Object> properties = new Hashtable<>();

        for (int i = 0; i < keyValues.length; i = i + 2) {
            properties.put(keyValues[i].toString(), keyValues[i + 1]);
        }

        if (singleton) {
            return bundleContext.registerService(
                Object.class, new TestAddonLifecycle(), properties);
        } else {
            PrototypeServiceFactory<Object> prototypeServiceFactory =
                new PrototypeServiceFactory<Object>() {
                    @Override
                    public Object getService(
                        Bundle bundle,
                        ServiceRegistration<Object> registration) {

                        return new TestAddonLifecycle();
                    }

                    @Override
                    public void ungetService(
                        Bundle bundle, ServiceRegistration<Object> registration,
                        Object service) {

                    }
                };

            ServiceRegistration<Object> serviceRegistration =
                bundleContext.registerService(
                    Object.class, (ServiceFactory<?>) prototypeServiceFactory,
                    properties);

            _registrations.add(serviceRegistration);

            return serviceRegistration;
        }
    }

    private ServiceRegistration<Application> registerApplication(
        Application application, Object... keyValues) {

        Dictionary<String, Object> properties = new Hashtable<>();

        properties.put(JAX_RS_APPLICATION_BASE, "/test-application");

        for (int i = 0; i < keyValues.length; i = i + 2) {
            properties.put(keyValues[i].toString(), keyValues[i + 1]);
        }

        ServiceRegistration<Application> serviceRegistration =
            bundleContext.registerService(
                Application.class, application, properties);

        _registrations.add(serviceRegistration);

        return serviceRegistration;
    }

    private ServiceRegistration<Application> registerApplication(
        ServiceFactory<Application> serviceFactory, Object... keyValues) {

        Dictionary<String, Object> properties = new Hashtable<>();

        properties.put(JAX_RS_APPLICATION_BASE, "/test-application");

        for (int i = 0; i < keyValues.length; i = i + 2) {
            properties.put(keyValues[i].toString(), keyValues[i + 1]);
        }

        ServiceRegistration<Application> serviceRegistration =
            bundleContext.registerService(
                Application.class, serviceFactory, properties);

        _registrations.add(serviceRegistration);

        return serviceRegistration;
    }

    private ServiceRegistration<?> registerExtension(
        String name, Object... keyValues) {

        TestFilter testFilter = new TestFilter();

        Dictionary<String, Object> properties = new Hashtable<>();

        properties.put(JAX_RS_EXTENSION, true);
        properties.put(JAX_RS_NAME, name);

        for (int i = 0; i < keyValues.length; i = i + 2) {
            properties.put(keyValues[i].toString(), keyValues[i + 1]);
        }

        ServiceRegistration<ContainerResponseFilter> serviceRegistration =
            bundleContext.registerService(
                ContainerResponseFilter.class, testFilter, properties);

        _registrations.add(serviceRegistration);

        return serviceRegistration;
    }

    private ServiceRegistration<?> registerMultiExtension(
        String name, String... classes) {

        Dictionary<String, Object> properties = new Hashtable<>();

        properties.put(JAX_RS_EXTENSION, true);
        properties.put(JAX_RS_NAME, name);

        ServiceRegistration<?> serviceRegistration =
            bundleContext.registerService(
                classes, new TestFilterAndExceptionMapper(), properties);

        _registrations.add(serviceRegistration);

        return serviceRegistration;
    }

    private ServiceRegistration<?> registerInvalidExtension(
        String name, Object... keyValues) {

        TestFilter testFilter = new TestFilter();

        Dictionary<String, Object> properties = new Hashtable<>();

        properties.put(JAX_RS_EXTENSION, true);
        properties.put(JAX_RS_NAME, name);

        for (int i = 0; i < keyValues.length; i = i + 2) {
            properties.put(keyValues[i].toString(), keyValues[i + 1]);
        }

        ServiceRegistration<Object> serviceRegistration =
            bundleContext.registerService(
                Object.class, testFilter, properties);

        _registrations.add(serviceRegistration);

        return serviceRegistration;
    }

    private ServiceRegistration<?> registerUngettableExtension(
        String name, Object... keyValues) {

        Dictionary<String, Object> properties = new Hashtable<>();

        properties.put(JAX_RS_EXTENSION, true);
        properties.put(JAX_RS_NAME, name);

        for (int i = 0; i < keyValues.length; i = i + 2) {
            properties.put(keyValues[i].toString(), keyValues[i + 1]);
        }

        ServiceRegistration<ContainerResponseFilter> serviceRegistration =
            bundleContext.registerService(
                ContainerResponseFilter.class,
                new ServiceFactory<ContainerResponseFilter>() {
                    @Override
                    public ContainerResponseFilter getService(
                        Bundle bundle,
                        ServiceRegistration<ContainerResponseFilter>
                            serviceRegistration) {

                        return null;
                    }

                    @Override
                    public void ungetService(
                        Bundle bundle,
                        ServiceRegistration<ContainerResponseFilter>
                            serviceRegistration,
                        ContainerResponseFilter containerResponseFilter) {

                    }
                }, properties);

        _registrations.add(serviceRegistration);

        return serviceRegistration;
    }

    private ServiceRegistration<Application> registerUngettableApplication(
        Object... keyValues) {

        return registerApplication(
            new ServiceFactory<Application>() {
                @Override
                public Application getService(
                    Bundle bundle,
                    ServiceRegistration<Application> serviceRegistration) {

                    return null;
                }

                @Override
                public void ungetService(
                    Bundle bundle,
                    ServiceRegistration<Application> serviceRegistration,
                    Application application) {

                }
            }, keyValues);
    }

}
