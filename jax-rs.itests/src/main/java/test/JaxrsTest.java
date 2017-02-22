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

import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import test.types.TestAddon;
import test.types.TestApplication;
import test.types.TestFilter;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class JaxrsTest {

    static BundleContext bundleContext = FrameworkUtil.getBundle(
        JaxrsTest.class).getBundleContext();

    @Test
    public void testApplication() {
        ServiceRegistration<?> serviceRegistration = null;

        try {
            serviceRegistration = registerApplication();

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
    public void testApplicationReadd() {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("/test-application");

        Runnable testCase = () -> {
            assertEquals(404, webTarget.request().get().getStatus());

            ServiceRegistration<?> serviceRegistration = null;

            try {
                serviceRegistration = registerApplication();

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
            applicationRegistration = registerApplication();

            serviceRegistration = registerAddon(
                "osgi.jaxrs.application.select",
                "(osgi.jaxrs.application.base=/test-application)");

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
            applicationRegistration = registerApplication();

            Runnable testCase = () -> {
                assertEquals(webTarget.request().get().getStatus(), 404);

                ServiceRegistration<?> serviceRegistration = null;

                try {
                    serviceRegistration = registerAddon(
                        "osgi.jaxrs.application.select",
                        "(osgi.jaxrs.application.base=/test-application)");

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
            applicationRegistration = registerApplication();

            filterRegistration = registerFilter(
                "osgi.jaxrs.application.select",
                "(osgi.jaxrs.application.base=/test-application)");

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
            applicationRegistration = registerApplication();

            assertEquals(
                "Hello application",
                webTarget.request().get().readEntity(String.class));

            Runnable testCase = () ->  {
                Response response = webTarget.request().get();

                assertNull(response.getHeaders().getFirst("Filtered"));

                ServiceRegistration<?> filterRegistration = null;

                try {
                    filterRegistration = registerFilter(
                        "osgi.jaxrs.application.select",
                        "(osgi.jaxrs.application.base=/test-application)");

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
    public void testStandaloneEndPoint() {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("/test-addon").
            path("test");

        ServiceRegistration<?> serviceRegistration = null;

        try {
            serviceRegistration = registerAddon(
                "osgi.jaxrs.resource.base", "/test-addon");

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
            path("/test-addon").
            path("test");

        Runnable testCase = () -> {
            assertEquals(404, webTarget.request().get().getStatus());

            ServiceRegistration<?> serviceRegistration = null;

            try {
                serviceRegistration = registerAddon(
                    "osgi.jaxrs.resource.base", "/test-addon");

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
    public void testStandaloneFilter() {
        Client client = createClient();

        WebTarget webTarget = client.
            target("http://localhost:8080").
            path("/test-addon").
            path("test");

        ServiceRegistration<?> filterRegistration = null;

        ServiceRegistration<?> serviceRegistration = null;

        try {
            serviceRegistration = registerAddon(
                "osgi.jaxrs.resource.base", "/test-addon");

            filterRegistration = registerFilter(
                "osgi.jaxrs.extension.name", "test-filter");

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
            path("/test-addon").
            path("test");

        ServiceRegistration<?> serviceRegistration = null;

        try {
            serviceRegistration = registerAddon(
                "osgi.jaxrs.resource.base", "/test-addon");

            assertEquals("Hello test",
                webTarget.request().get().readEntity(String.class));

            Runnable testCase = () -> {
                ServiceRegistration<?> filterRegistration = null;

                try {
                    Response response = webTarget.request().get();

                    assertNull(response.getHeaders().getFirst("Filtered"));

                    filterRegistration = registerFilter(
                        "osgi.jaxrs.extension.name", "test-filter");

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
            path("/test-addon").
            path("test");

        ServiceRegistration<?> serviceRegistration = null;
        ServiceRegistration<?> extensionRegistration1;
        ServiceRegistration<?> extensionRegistration2;

        try {
            serviceRegistration = registerAddon(
                "osgi.jaxrs.resource.base", "/test-addon",
                "osgi.jaxrs.extension.select", new String[]{
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

    private Client createClient() {
        Thread thread = Thread.currentThread();

        ClassLoader contextClassLoader = thread.getContextClassLoader();

        try {
            thread.setContextClassLoader(
                org.apache.cxf.jaxrs.client.Client.class.getClassLoader());

            return ClientBuilder.newClient();
        }
        finally {
            thread.setContextClassLoader(contextClassLoader);
        }
    }

    private ServiceRegistration<?> registerAddon(Object ... keyValues) {

        TestAddon testAddon = new TestAddon();

        Dictionary<String, Object> properties = new Hashtable<>();

        for (int i = 0; i < keyValues.length; i = i + 2) {
            properties.put(keyValues[i].toString(), keyValues[i + 1]);
        }

        return bundleContext.registerService(
            Object.class, testAddon, properties);
    }


    private ServiceRegistration<?> registerApplication() {
        TestApplication testApplication = new TestApplication();

        Dictionary<String, Object> properties = new Hashtable<>();

        properties.put(
            "osgi.jaxrs.application.base", "/test-application");

        return bundleContext.registerService(
            Application.class, testApplication, properties);
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

        properties.put("osgi.jaxrs.extension.name", name);

        for (int i = 0; i < keyValues.length; i = i + 2) {
            properties.put(keyValues[i].toString(), keyValues[i + 1]);
        }

        return bundleContext.registerService(
            Object.class, testFilter, properties);
    }

}
