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
    public void testApplicationEndpointExtension() {
        ServiceRegistration<?> applicationRegistration = null;

        ServiceRegistration<?> serviceRegistration = null;

        try {
            applicationRegistration = registerApplication();


            TestAddon testAddon = new TestAddon();

            Dictionary<String, Object> properties = new Hashtable<>();

            properties.put(
                "jaxrs.application.select",
                "(osgi.jaxrs.application.base=/test-application)");

            serviceRegistration = bundleContext.registerService(
                Object.class, testAddon, properties);

            Client client = createClient();

            WebTarget webTarget = client.
                target("http://localhost:8080").
                path("/test-application").
                path("extended");

            Response response = webTarget.request().get();

            assertEquals("Hello extended",
                response.readEntity(String.class));
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
    public void testApplicationProviderExtension() {
        ServiceRegistration<?> applicationRegistration = null;

        ServiceRegistration<?> filterRegistration = null;

        try {
            applicationRegistration = registerApplication();

            TestFilter testFilter = new TestFilter();

            Dictionary<String, Object> properties = new Hashtable<>();

            properties.put(
                "jaxrs.application.select",
                "(osgi.jaxrs.application.base=/test-application)");

            filterRegistration = bundleContext.registerService(
                Object.class, testFilter, properties);

            Client client = createClient();

            WebTarget webTarget = client.
                target("http://localhost:8080").
                path("/test-application");
            Response response = webTarget.request().get();

            assertEquals(
                "Hello application", response.readEntity(String.class));

            assertEquals(response.getHeaders().getFirst("Filtered"), "true");
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
    public void testStandaloneEndPoint() {
        ServiceRegistration<?> serviceRegistration = null;

        try {
            TestAddon testAddon = new TestAddon();

            Dictionary<String, Object> properties = new Hashtable<>();

            properties.put("osgi.jaxrs.resource.base", "/test-addon");

            serviceRegistration = bundleContext.registerService(
                Object.class, testAddon, properties);

            Client client = createClient();

            WebTarget webTarget = client.
                target("http://localhost:8080").
                path("/test-addon").
                path("test");

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
    public void testStandaloneFilter() {
        ServiceRegistration<?> filterRegistration = null;

        ServiceRegistration<?> serviceRegistration = null;

        try {
            TestAddon testAddon = new TestAddon();

            Dictionary<String, Object> properties = new Hashtable<>();

            properties.put("osgi.jaxrs.resource.base", "/test-addon");

            serviceRegistration = bundleContext.registerService(
                Object.class, testAddon, properties);

            TestFilter testFilter = new TestFilter();

            properties = new Hashtable<>();

            properties.put("osgi.jaxrs.filter.base", "/test-addon");

            filterRegistration = bundleContext.registerService(
                Object.class, testFilter, properties);

            Client client = createClient();

            WebTarget webTarget = client.
                target("http://localhost:8080").
                path("/test-addon").
                path("test");

            Response response = webTarget.request().get();

            assertEquals(
                "This should say hello", "Hello test",
                response.readEntity(String.class));

            assertEquals(response.getHeaders().getFirst("Filtered"), "true");
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


    private ServiceRegistration<?> registerApplication() {
        TestApplication testApplication = new TestApplication();

        Dictionary<String, Object> properties = new Hashtable<>();

        properties.put(
            "osgi.jaxrs.application.base", "/test-application");

        return bundleContext.registerService(
            Application.class, testApplication, properties);
    }

}
