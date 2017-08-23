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
import static org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants.*;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceFactory;
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
import test.types.TestFilter;
import test.types.TestHelper;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

public class JaxrsTest extends TestHelper {

    private ServiceTracker<JaxRSServiceRuntime, JaxRSServiceRuntime> _runtimeTracker;

    @After
    public void tearDown() {
        if (_runtimeTracker != null) {
            _runtimeTracker.close();
        }
    }

    @Test
    public void testApplication() throws InterruptedException {
        ServiceRegistration<?> serviceRegistration = null;

        try {
            JaxRSServiceRuntime runtime = getJaxRSServiceRuntime();

            assertNotNull(runtime);

            assertEquals(1, runtime.getRuntimeDTO().applicationDTOs.length);

            serviceRegistration = registerApplication(
                new TestApplication());

            assertEquals(2, runtime.getRuntimeDTO().applicationDTOs.length);

            Client client = createClient();

            WebTarget webTarget = client.
                target("http://localhost:8080").
                path("/test-application");

            Response response = webTarget.request().get();

            assertEquals("Hello application",
                response.readEntity(String.class));
        }
        finally {
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
            }
        }
    }

    @Test
    public void testApplicationWithError() throws InterruptedException {
        JaxRSServiceRuntime runtime = getJaxRSServiceRuntime();

        assertNotNull(runtime);

        RuntimeDTO runtimeDTO = runtime.getRuntimeDTO();

        assertEquals(1, runtimeDTO.applicationDTOs.length);
        assertEquals(0, runtimeDTO.failedExtensionDTOs.length);

        ServiceRegistration<?> serviceRegistration = registerApplication(
            new TestApplication() {

                @Override
                public Set<Object> getSingletons() {
                    throw new RuntimeException();
                }

            });

        runtimeDTO = runtime.getRuntimeDTO();

        assertEquals(1, runtimeDTO.applicationDTOs.length);
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

        assertEquals(1, runtimeDTO.applicationDTOs.length);
        assertEquals(0, runtimeDTO.failedApplicationDTOs.length);
    }

    @Test
    public void testApplicationConflict() {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("test-application");

        ServiceRegistration<?> serviceRegistration = null;
        ServiceRegistration<?> serviceRegistration2;

        try {
            serviceRegistration = registerApplication(new TestApplication());

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
        finally {
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
            }
        }
    }

    @Test
    public void testApplicationEndpointExtension() {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("/test-application").
            path("extended");

        ServiceRegistration<?> applicationRegistration = null;

        ServiceRegistration<?> serviceRegistration = null;

        try {
            applicationRegistration = registerApplication(
                new TestApplication());

            serviceRegistration = registerAddon(
                new TestAddon(), JAX_RS_APPLICATION_SELECT,
                "(" + JAX_RS_APPLICATION_BASE + "=/test-application)");

            assertEquals(
                "Hello extended",
                webTarget.request().get().readEntity(String.class));
        }
        finally {
            if (applicationRegistration != null) {
                applicationRegistration.unregister();
            }
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
            }
        }
    }

    @Test
    public void testApplicationEndpointExtensionReadd() throws InterruptedException {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("/test-application").
            path("extended");

        ServiceRegistration<?> applicationRegistration = null;

        try {
            applicationRegistration = registerApplication(
                new TestApplication());

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
        finally {
            if (applicationRegistration != null) {
                applicationRegistration.unregister();
            }

        }
    }

    @Test
    public void testApplicationOverride() throws InterruptedException {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("test-application");

        JaxRSServiceRuntime runtime = getJaxRSServiceRuntime();

        ServiceRegistration<?> serviceRegistration = null;
        ServiceRegistration<?> serviceRegistration2;

        try {
            assertEquals(1, runtime.getRuntimeDTO().applicationDTOs.length);
            assertEquals(0, runtime.getRuntimeDTO().failedApplicationDTOs.length);

            serviceRegistration = registerApplication(new TestApplication());

            assertEquals(2, runtime.getRuntimeDTO().applicationDTOs.length);
            assertEquals(0, runtime.getRuntimeDTO().failedApplicationDTOs.length);

            Response response = webTarget.request().get();

            assertEquals(
                "Hello application",
                response.readEntity(String.class));

            serviceRegistration2 = registerApplication(
                new TestApplicationConflict(), "service.ranking", 1);

            assertEquals(2, runtime.getRuntimeDTO().applicationDTOs.length);
            assertEquals(1, runtime.getRuntimeDTO().failedApplicationDTOs.length);

            response = webTarget.request().get();

            assertEquals(
                "Hello application conflict",
                response.readEntity(String.class));

            assertEquals(
                "conflict",
                webTarget.path("conflict").request().get(String.class));

            serviceRegistration2.unregister();

            assertEquals(2, runtime.getRuntimeDTO().applicationDTOs.length);
            assertEquals(0, runtime.getRuntimeDTO().failedApplicationDTOs.length);

            response = webTarget.request().get();

            assertEquals(
                "Hello application", response.readEntity(String.class));
        }
        finally {
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
            }
        }
    }

    @Test
    public void testApplicationProviderExtension() {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("/test-application");

        ServiceRegistration<?> applicationRegistration = null;

        ServiceRegistration<?> filterRegistration = null;

        try {
            applicationRegistration = registerApplication(
                new TestApplication());

            filterRegistration = registerExtension(
                "filter",
                JAX_RS_APPLICATION_SELECT,
                "(" + JAX_RS_APPLICATION_BASE + "=/test-application)");

            Response response = webTarget.request().get();

            assertEquals(
                "Hello application",
                response.readEntity(String.class));

            assertEquals("true", response.getHeaders().getFirst("Filtered"));
        }
        finally {
            if (applicationRegistration != null) {
                applicationRegistration.unregister();
            }
            if (filterRegistration != null) {
                filterRegistration.unregister();
            }
        }
    }

    @Test
    public void testApplicationProviderExtensionReadd() {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("/test-application");

        ServiceRegistration<?> applicationRegistration = null;

        try {
            applicationRegistration = registerApplication(
                new TestApplication());

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
        finally {
            if (applicationRegistration != null) {
                applicationRegistration.unregister();
            }
        }
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
                serviceRegistration = registerApplication(new TestApplication());

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
    public void testEndpointsOverride() {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("conflict");

        ServiceRegistration<?> serviceRegistration = null;
        ServiceRegistration<?> serviceRegistration2 = null;

        try {
            serviceRegistration = registerAddon(new TestAddonConflict());

            Response response = webTarget.request().get();

            assertEquals(
                "This should say hello1", "hello1",
                response.readEntity(String.class));

            serviceRegistration2 = registerAddon(
                new TestAddonConflict2(), "service.ranking", 1);

            response = webTarget.request().get();

            assertEquals(
                "This should say hello2", "hello2",
                response.readEntity(String.class));

            serviceRegistration2.unregister();

            serviceRegistration2 = null;

            response = webTarget.request().get();

            assertEquals(
                "This should say hello1", "hello1",
                response.readEntity(String.class));
        }
        finally {
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
            }
            if (serviceRegistration2 != null) {
                serviceRegistration2.unregister();
            }
        }
    }

    @Test
    public void testGettableAndNotGettableApplication() throws InterruptedException {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("test-application");

        JaxRSServiceRuntime runtime = getJaxRSServiceRuntime();

        assertEquals(1, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(0, runtime.getRuntimeDTO().failedApplicationDTOs.length);

        ServiceRegistration<Application> serviceRegistration =
            registerApplication(new TestApplication());

        assertEquals(2, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(0, runtime.getRuntimeDTO().failedApplicationDTOs.length);

        assertEquals(
            "Hello application",
            webTarget.request().get().readEntity(String.class));

        ServiceRegistration<Application> ungettableServiceRegistration =
            registerUngettableApplication("service.ranking", 1);

        assertEquals(2, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(1, runtime.getRuntimeDTO().failedApplicationDTOs.length);

        assertEquals(
            DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE,
            runtime.getRuntimeDTO().failedApplicationDTOs[0].failureReason);

        assertEquals(
            "Hello application",
            webTarget.request().get().readEntity(String.class));

        serviceRegistration.unregister();

        assertEquals(1, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(1, runtime.getRuntimeDTO().failedApplicationDTOs.length);

        assertEquals(404, webTarget.request().get().getStatus());

        ungettableServiceRegistration.unregister();

        assertEquals(1, runtime.getRuntimeDTO().applicationDTOs.length);
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

        assertEquals(1, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(0, runtime.getRuntimeDTO().failedApplicationDTOs.length);

        ServiceRegistration<Application> serviceRegistration =
            registerApplication(new TestApplication());

        assertEquals(2, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(0, runtime.getRuntimeDTO().failedApplicationDTOs.length);

        assertEquals(
            "Hello application",
            webTarget.request().get().readEntity(String.class));

        ServiceRegistration<Application> ungettableServiceRegistration =
            registerUngettableApplication("service.ranking", -1);

        assertEquals(2, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(1, runtime.getRuntimeDTO().failedApplicationDTOs.length);

        assertEquals(
            DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE,
            runtime.getRuntimeDTO().failedApplicationDTOs[0].failureReason);

        assertEquals(
            "Hello application",
            webTarget.request().get().readEntity(String.class));

        serviceRegistration.unregister();

        assertEquals(1, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(1, runtime.getRuntimeDTO().failedApplicationDTOs.length);

        assertEquals(404, webTarget.request().get().getStatus());

        ungettableServiceRegistration.unregister();

        assertEquals(1, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(0, runtime.getRuntimeDTO().failedApplicationDTOs.length);
    }

    @Test
    public void testNotGettableApplication() throws InterruptedException {
        JaxRSServiceRuntime runtime = getJaxRSServiceRuntime();

        assertEquals(1, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(0, runtime.getRuntimeDTO().failedApplicationDTOs.length);

        ServiceRegistration<Application> serviceRegistration =
            registerUngettableApplication();

        assertEquals(1, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(1, runtime.getRuntimeDTO().failedApplicationDTOs.length);

        assertEquals(
            DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE,
            runtime.getRuntimeDTO().failedApplicationDTOs[0].failureReason);

        serviceRegistration.unregister();

        assertEquals(1, runtime.getRuntimeDTO().applicationDTOs.length);
        assertEquals(0, runtime.getRuntimeDTO().failedApplicationDTOs.length);
    }

    @Test
    public void testStandaloneEndPoint() throws InterruptedException {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("test");

        JaxRSServiceRuntime runtime = getJaxRSServiceRuntime();

        ServiceRegistration<?> serviceRegistration = null;

        try {
            runtime.getRuntimeDTO();

            serviceRegistration = registerAddon(new TestAddon());

            Response response = webTarget.request().get();

            assertEquals(
                "This should say hello", "Hello test",
                response.readEntity(String.class));
        }
        finally {
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
            }
        }
    }

    @Test
    public void testStandaloneEndPointPrototypeLifecycle() {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("/test-addon-lifecycle");

        ServiceRegistration<?> serviceRegistration = null;

        try {
            serviceRegistration = registerAddonLifecycle(
                false, JAX_RS_RESOURCE, "true");

            String first = webTarget.request().get().readEntity(String.class);

            String second = webTarget.request().get().readEntity(String.class);

            assertNotEquals("This should be different", first, second);
        }
        finally {
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
            }
        }
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

        ServiceRegistration<?> serviceRegistration = null;

        try {
            serviceRegistration = registerAddonLifecycle(
                true, JAX_RS_RESOURCE, "true");

            String first = webTarget.request().get().readEntity(String.class);

            String second = webTarget.request().get().readEntity(String.class);

            assertEquals("This should be equal", first, second);
        }
        finally {
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
            }
        }
    }

    @Test
    public void testStandaloneEndpointWithExtensionsDependencies() {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("test");

        ServiceRegistration<?> serviceRegistration = null;
        ServiceRegistration<?> extensionRegistration1;
        ServiceRegistration<?> extensionRegistration2;

        try {
            serviceRegistration = registerAddon(
                new TestAddon(),
                JAX_RS_EXTENSION_SELECT, new String[]{
                    "(property one=one)",
                    "(property two=two)",
                });

            assertEquals(404, webTarget.request().get().getStatus());

            extensionRegistration1 = registerExtension(
                "aExtension", "property one", "one");

            assertEquals(404, webTarget.request().get().getStatus());

            extensionRegistration2 = registerExtension(
                "anotherExtension", "property two", "two");

            Response response = webTarget.request().get();

            assertEquals(
                "This should say hello", "Hello test",
                response.readEntity(String.class));

            extensionRegistration1.unregister();

            assertEquals(404, webTarget.request().get().getStatus());

            extensionRegistration1 = registerExtension(
                "aExtension", "property one", "one");

            assertEquals(
                "This should say hello", "Hello test",
                response.readEntity(String.class));

            extensionRegistration2.unregister();

            assertEquals(404, webTarget.request().get().getStatus());

            extensionRegistration1.unregister();
        }
        finally {
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
            }
        }
    }

    @Test
    public void testStandaloneFilter() {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("test");

        ServiceRegistration<?> filterRegistration = null;

        ServiceRegistration<?> serviceRegistration = null;

        try {
            serviceRegistration = registerAddon(new TestAddon());

            filterRegistration = registerExtension(
                "Filter", JAX_RS_EXTENSION, "test-filter");

            Response response = webTarget.request().get();

            assertEquals(
                "This should say hello", "Hello test",
                response.readEntity(String.class));

            assertEquals("true", response.getHeaders().getFirst("Filtered"));
        }
        finally {
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
            }
            if (filterRegistration != null) {
                filterRegistration.unregister();
            }
        }
    }

    @Test
    public void testStandaloneFilterReadd() {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("test");

        ServiceRegistration<?> serviceRegistration = null;

        try {
            serviceRegistration = registerAddon(new TestAddon());

            assertEquals("Hello test",
                webTarget.request().get().readEntity(String.class));

            Runnable testCase = () -> {
                ServiceRegistration<?> filterRegistration = null;

                try {
                    Response response = webTarget.request().get();

                    assertNull(response.getHeaders().getFirst("Filtered"));

                    filterRegistration = registerExtension(
                        "Filter", JAX_RS_EXTENSION, "test-filter");

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
        finally {
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
            }

        }
    }

    private JaxRSServiceRuntime getJaxRSServiceRuntime() throws InterruptedException {
        _runtimeTracker = new ServiceTracker<>(
            bundleContext, JaxRSServiceRuntime.class, null);

        _runtimeTracker.open();

        return _runtimeTracker.waitForService(5000);
    }

    private ServiceRegistration<?> registerAddon(Object instance, Object... keyValues) {
        Dictionary<String, Object> properties = new Hashtable<>();

        properties.put(JAX_RS_RESOURCE, "true");

        for (int i = 0; i < keyValues.length; i = i + 2) {
            properties.put(keyValues[i].toString(), keyValues[i + 1]);
        }

        return bundleContext.registerService(
            Object.class, instance, properties);
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
                        Bundle bundle, ServiceRegistration<Object> registration) {

                        return new TestAddonLifecycle();
                    }

                    @Override
                    public void ungetService(
                        Bundle bundle, ServiceRegistration<Object> registration,
                        Object service) {

                    }
                };

            return bundleContext.registerService(
                Object.class, (ServiceFactory<?>) prototypeServiceFactory,
                properties);
        }
    }

    private ServiceRegistration<Application> registerApplication(
        Application application, Object... keyValues) {

        Dictionary<String, Object> properties = new Hashtable<>();

        properties.put(JAX_RS_APPLICATION_BASE, "/test-application");

        for (int i = 0; i < keyValues.length; i = i + 2) {
            properties.put(keyValues[i].toString(), keyValues[i + 1]);
        }

        return bundleContext.registerService(
            Application.class, application, properties);
    }

    private ServiceRegistration<Application> registerApplication(
        ServiceFactory<Application> serviceFactory, Object... keyValues) {

        Dictionary<String, Object> properties = new Hashtable<>();

        properties.put(JAX_RS_APPLICATION_BASE, "/test-application");

        for (int i = 0; i < keyValues.length; i = i + 2) {
            properties.put(keyValues[i].toString(), keyValues[i + 1]);
        }

        return bundleContext.registerService(
            Application.class, serviceFactory, properties);
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

        return bundleContext.registerService(
            Object.class, testFilter, properties);
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
