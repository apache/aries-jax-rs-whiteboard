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

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

import org.osgi.service.jaxrs.runtime.dto.FailedApplicationDTO;
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

    @Test
    public void testApplication() {
        ServiceRegistration<?> serviceRegistration = null;

        try {
            serviceRegistration = registerApplication(
                new TestApplication());

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
    public void testApplicationConflict() {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("test-application");

        @SuppressWarnings("rawtypes")
        ServiceTracker serviceTracker = new ServiceTracker<>(
            bundleContext, FailedApplicationDTO.class, null);

        serviceTracker.open();

        ServiceRegistration<?> serviceRegistration = null;
        ServiceRegistration<?> serviceRegistration2;

        try {
            serviceRegistration = registerApplication(new TestApplication());

            Response response = webTarget.request().get();

            assertEquals(
                "Hello application",
                response.readEntity(String.class));

            assertNull(serviceTracker.getService());

            serviceRegistration2 = registerApplication(
                new TestApplicationConflict(), "service.ranking", -1);

            response = webTarget.request().get();

            assertEquals(
                "Hello application", response.readEntity(String.class));

            assertNotNull(serviceTracker.getService());

            assertEquals(
                Response.Status.NOT_FOUND.getStatusCode(),
                webTarget.path("conflict").request().get().getStatus());

            serviceRegistration2.unregister();

            assertNull(serviceTracker.getService());

            response = webTarget.request().get();

            assertEquals(
                "Hello application", response.readEntity(String.class));
        }
        finally {
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
            }

            serviceTracker.close();
        }
    }

    @Test
    public void testApplicationOverride() {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("test-application");

        @SuppressWarnings("rawtypes")
        ServiceTracker serviceTracker = new ServiceTracker<>(
            bundleContext, FailedApplicationDTO.class, null);

        serviceTracker.open();

        ServiceRegistration<?> serviceRegistration = null;
        ServiceRegistration<?> serviceRegistration2;

        try {
            serviceRegistration = registerApplication(new TestApplication());

            Response response = webTarget.request().get();

            assertEquals(
                "Hello application",
                response.readEntity(String.class));

            assertNull(serviceTracker.getService());

            serviceRegistration2 = registerApplication(
                new TestApplicationConflict(), "service.ranking", 1);

            response = webTarget.request().get();

            assertEquals(
                "Hello application conflict",
                response.readEntity(String.class));

            assertNotNull(serviceTracker.getService());

            assertEquals(
                "conflict",
                webTarget.path("conflict").request().get(String.class));

            serviceRegistration2.unregister();

            assertNull(serviceTracker.getService());

            response = webTarget.request().get();

            assertEquals(
                "Hello application", response.readEntity(String.class));
        }
        finally {
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
            }

            serviceTracker.close();
        }
    }

    @Test
    public void testApplicationReadd() {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("/test-application");

        Runnable testCase = () -> {
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
    public void testApplicationEndpointExtensionReadd() {
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

            filterRegistration = registerFilter(
                JAX_RS_APPLICATION_SELECT,
                "(" + JAX_RS_APPLICATION_BASE + "=/test-application)");

            Response response = webTarget.request().get();

            assertEquals(
                "Hello application",
                response.readEntity(String.class));

            assertEquals(
                response.getHeaders().getFirst("Filtered"),
                "true");
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

            Runnable testCase = () ->  {
                Response response = webTarget.request().get();

                assertNull(response.getHeaders().getFirst("Filtered"));

                ServiceRegistration<?> filterRegistration = null;

                try {
                    filterRegistration = registerFilter(
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
    public void testStandaloneEndPoint() {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("test");

        ServiceRegistration<?> serviceRegistration = null;

        try {
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
    public void testStandaloneFilter() {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("test");

        ServiceRegistration<?> filterRegistration = null;

        ServiceRegistration<?> serviceRegistration = null;

        try {
            serviceRegistration = registerAddon(new TestAddon());

            filterRegistration = registerFilter(
                JAX_RS_EXTENSION, "test-filter");

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

                    filterRegistration = registerFilter(
                        JAX_RS_EXTENSION, "test-filter");

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

    private ServiceRegistration<?> registerAddon(Object instance, Object ... keyValues) {
        Dictionary<String, Object> properties = new Hashtable<>();

        properties.put(JAX_RS_RESOURCE, "true");

        for (int i = 0; i < keyValues.length; i = i + 2) {
            properties.put(keyValues[i].toString(), keyValues[i + 1]);
        }

        return bundleContext.registerService(
            Object.class, instance, properties);
    }

    private ServiceRegistration<?> registerAddonLifecycle(
        boolean singleton, Object ... keyValues) {

        Dictionary<String, Object> properties = new Hashtable<>();

        for (int i = 0; i < keyValues.length; i = i + 2) {
            properties.put(keyValues[i].toString(), keyValues[i + 1]);
        }

        if (singleton) {
            return bundleContext.registerService(
                Object.class, new TestAddonLifecycle(), properties);
        }
        else {
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
                Object.class, (ServiceFactory<?>)prototypeServiceFactory,
                properties);
        }
    }

    private ServiceRegistration<Application> registerApplication(
        Application application, Object ... keyValues) {

        Dictionary<String, Object> properties = new Hashtable<>();

        properties.put(JAX_RS_APPLICATION_BASE, "/test-application");

        for (int i = 0; i < keyValues.length; i = i + 2) {
            properties.put(keyValues[i].toString(), keyValues[i + 1]);
        }

        return bundleContext.registerService(
            Application.class, application, properties);
    }

    private ServiceRegistration<?> registerFilter(Object ... keyValues) {

        TestFilter testFilter = new TestFilter();

        Dictionary<String, Object> properties = new Hashtable<>();

        for (int i = 0; i < keyValues.length; i = i + 2) {
            properties.put(keyValues[i].toString(), keyValues[i + 1]);
        }

        return bundleContext.registerService(
            Object.class, testFilter, properties);
    }

    private ServiceRegistration<?> registerExtension(
        String name, Object ... keyValues) {

        TestFilter testFilter = new TestFilter();

        Dictionary<String, Object> properties = new Hashtable<>();

        properties.put(JAX_RS_EXTENSION, name);

        for (int i = 0; i < keyValues.length; i = i + 2) {
            properties.put(keyValues[i].toString(), keyValues[i + 1]);
        }

        return bundleContext.registerService(
            Object.class, testFilter, properties);
    }

}
