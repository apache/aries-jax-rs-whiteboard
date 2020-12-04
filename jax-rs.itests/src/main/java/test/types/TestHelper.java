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

package test.types;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Application;

import org.junit.After;
import org.junit.Before;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.jaxrs.client.SseEventSourceFactory;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;
import org.osgi.service.jaxrs.runtime.dto.RuntimeDTO;
import org.osgi.util.tracker.ServiceTracker;

import static org.junit.Assert.assertTrue;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_EXTENSION;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_NAME;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_RESOURCE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class TestHelper {

    public static BundleContext bundleContext =
        FrameworkUtil.
            getBundle(TestHelper.class).
            getBundleContext();

    protected Collection<ServiceRegistration<?>> _registrations =
        new ArrayList<>();
    protected ServiceTracker<JaxrsServiceRuntime, JaxrsServiceRuntime>
        _runtimeTracker;
    protected ServiceTracker<ClientBuilder, ClientBuilder>
        _clientBuilderTracker;
    protected JaxrsServiceRuntime _runtime;
    protected ServiceReference<JaxrsServiceRuntime> _runtimeServiceReference;

    @After
    public void tearDown() {
        Iterator<ServiceRegistration<?>> iterator = _registrations.iterator();

        while (iterator.hasNext()) {
            ServiceRegistration<?> registration =  iterator.next();

            try {
                registration.unregister();
            }
            catch(Exception e) {
            }
            finally {
                iterator.remove();
            }
        }

        if (_runtimeTracker != null) {
            _runtimeTracker.close();
        }

        _clientBuilderTracker.close();

        _configurationAdminTracker.close();

        _sseEventSourceFactoryTracker.close();
    }

    @Before
    public void before() {
        _clientBuilderTracker = new ServiceTracker<>(
            bundleContext, ClientBuilder.class, null);

        _clientBuilderTracker.open();

        _configurationAdminTracker = new ServiceTracker<>(
            bundleContext, ConfigurationAdmin.class, null);

        _configurationAdminTracker.open();

        _sseEventSourceFactoryTracker = new ServiceTracker<>(
            bundleContext, SseEventSourceFactory.class, null);

        _sseEventSourceFactoryTracker.open();

        _runtimeTracker = new ServiceTracker<>(
            bundleContext, JaxrsServiceRuntime.class, null);

        _runtimeTracker.open();

        try {
            _runtime = _runtimeTracker.waitForService(15000L);
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        _runtimeServiceReference = _runtimeTracker.getServiceReference();
    }

    private ServiceTracker<ConfigurationAdmin, ConfigurationAdmin>
        _configurationAdminTracker;

    private ServiceTracker<SseEventSourceFactory, SseEventSourceFactory>
        _sseEventSourceFactoryTracker;

    @SuppressWarnings("unchecked")
    private static String[] canonicalize(Object propertyValue) {
        if (propertyValue == null) {
            return new String[0];
        }
        if (propertyValue instanceof String[]) {
            return (String[]) propertyValue;
        }
        if (propertyValue instanceof Collection) {
            return ((Collection<String>) propertyValue).toArray(new String[0]);
        }

        return new String[]{propertyValue.toString()};
    }

    public ConfigurationAdmin getConfigurationAdmin() {

        try {
            return _configurationAdminTracker.waitForService(5000);
        }
        catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    protected Client createClient() {
        ClientBuilder clientBuilder;

        try {
            clientBuilder = _clientBuilderTracker.waitForService(5000);

            clientBuilder.connectTimeout(600000, TimeUnit.SECONDS);
            clientBuilder.readTimeout(600000, TimeUnit.SECONDS);

            return clientBuilder.build();
        }
        catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
    }

    protected SseEventSourceFactory createSseFactory() {
        try {
            return _sseEventSourceFactoryTracker.waitForService(5000);
        }
        catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
    }

    protected WebTarget createDefaultTarget() {
        Client client = createClient();

        String[] runtimes = canonicalize(
            _runtimeServiceReference.getProperty("osgi.jaxrs.endpoint"));

        if (runtimes.length == 0) {
            throw new IllegalStateException(
                "No runtimes could be found on \"osgi.jaxrs.endpoint\" " +
                    "runtime service property ");
        }

        String runtime = runtimes[0];

        return client.target(runtime);
    }

    protected static long getServiceId(ServiceRegistration<?> propertyHolder) {
        return (long)propertyHolder.getReference().getProperty("service.id");
    }

    protected <T> void assertThatInRuntime(
        Function<RuntimeDTO, T[]> getter, Predicate<T> predicate) {

        assertTrue(
            Arrays.stream(getter.apply(getRuntimeDTO())).anyMatch(predicate));
    }

    protected JaxrsServiceRuntime getJaxrsServiceRuntime()
        throws InterruptedException {

        _runtimeTracker = new ServiceTracker<>(
            bundleContext, JaxrsServiceRuntime.class, null);

        _runtimeTracker.open();

        return _runtimeTracker.waitForService(15000L);
    }

    protected RuntimeDTO getRuntimeDTO() {
        return _runtime.getRuntimeDTO();
    }

    protected ServiceRegistration<?> registerAddon(
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

    protected ServiceRegistration<?> registerAddonPrototype(
        Supplier<?> supplier, Object... keyValues) {

        Dictionary<String, Object> properties = new Hashtable<>();

        properties.put(JAX_RS_RESOURCE, "true");

        for (int i = 0; i < keyValues.length; i = i + 2) {
            properties.put(keyValues[i].toString(), keyValues[i + 1]);
        }

        PrototypeServiceFactory<Object> prototypeServiceFactory =
            new PrototypeServiceFactory<Object>() {
                @Override
                public Object getService(
                    Bundle bundle,
                    ServiceRegistration<Object> registration) {

                    return supplier.get();
                }

                @Override
                public void ungetService(
                    Bundle bundle, ServiceRegistration<Object> registration,
                    Object service) {

                }
            };

        ServiceRegistration<?> serviceRegistration =
            bundleContext.registerService(
                Object.class, (ServiceFactory<?>) prototypeServiceFactory,
                properties);

        _registrations.add(serviceRegistration);

        return serviceRegistration;
    }

    protected ServiceRegistration<?> registerAddonLifecycle(
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

    protected ServiceRegistration<Application> registerApplication(
        Application application, Object... keyValues) {

        Dictionary<String, Object> properties = new Hashtable<>();

        for (int i = 0; i < keyValues.length; i = i + 2) {
            properties.put(keyValues[i].toString(), keyValues[i + 1]);
        }

        if (properties.get(JAX_RS_APPLICATION_BASE) == null) {
            properties.put(JAX_RS_APPLICATION_BASE, "/test-application");
        }

        ServiceRegistration<Application> serviceRegistration =
            bundleContext.registerService(
                Application.class, application, properties);

        _registrations.add(serviceRegistration);

        return serviceRegistration;
    }

    protected ServiceRegistration<Application> registerApplication(
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

    protected ServiceRegistration<?> registerExtension(
        String name, Object... keyValues) {

        TestFilter testFilter = new TestFilter();

        Hashtable<String, Object> properties = new Hashtable<>();

        properties.put(JAX_RS_EXTENSION, true);
        properties.put(JAX_RS_NAME, name);
        properties.putIfAbsent(
            JAX_RS_APPLICATION_SELECT, "(osgi.jaxrs.name=*)");

        for (int i = 0; i < keyValues.length; i = i + 2) {
            properties.put(keyValues[i].toString(), keyValues[i + 1]);
        }

        ServiceRegistration<ContainerResponseFilter> serviceRegistration =
            bundleContext.registerService(
                ContainerResponseFilter.class, testFilter, properties);

        _registrations.add(serviceRegistration);

        return serviceRegistration;
    }

    protected <T> ServiceRegistration<T> registerExtension(
        Class<T> clazz, T extension, String name, Object... keyValues) {

        Hashtable<String, Object> properties = new Hashtable<>();

        properties.put(JAX_RS_EXTENSION, true);
        properties.put(JAX_RS_NAME, name);
        properties.putIfAbsent(
            JAX_RS_APPLICATION_SELECT, "(osgi.jaxrs.name=*)");

        for (int i = 0; i < keyValues.length; i = i + 2) {
            properties.put(keyValues[i].toString(), keyValues[i + 1]);
        }

        ServiceRegistration<T> serviceRegistration =
            bundleContext.registerService(clazz, extension, properties);

        _registrations.add(serviceRegistration);

        return serviceRegistration;
    }

    protected ServiceRegistration<?> registerInvalidExtension(
        String name, Object... keyValues) {

        TestFilter testFilter = new TestFilter();

        Hashtable<String, Object> properties = new Hashtable<>();

        properties.put(JAX_RS_EXTENSION, true);
        properties.put(JAX_RS_NAME, name);
        properties.putIfAbsent(
            JAX_RS_APPLICATION_SELECT, "(osgi.jaxrs.name=*)");

        for (int i = 0; i < keyValues.length; i = i + 2) {
            properties.put(keyValues[i].toString(), keyValues[i + 1]);
        }

        ServiceRegistration<Object> serviceRegistration =
            bundleContext.registerService(
                Object.class, testFilter, properties);

        _registrations.add(serviceRegistration);

        return serviceRegistration;
    }

    protected ServiceRegistration<?> registerMultiExtension(
        String name, String... classes) {

        Hashtable<String, Object> properties = new Hashtable<>();

        properties.put(JAX_RS_EXTENSION, true);
        properties.put(JAX_RS_NAME, name);
        properties.putIfAbsent(
            JAX_RS_APPLICATION_SELECT, "(osgi.jaxrs.name=*)");

        ServiceRegistration<?> serviceRegistration =
            bundleContext.registerService(
                classes, new TestFilterAndExceptionMapper(), properties);

        _registrations.add(serviceRegistration);

        return serviceRegistration;
    }

    protected ServiceRegistration<Application> registerUngettableApplication(
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

    protected ServiceRegistration<?> registerUngettableExtension(
        String name, Object... keyValues) {

        Hashtable<String, Object> properties = new Hashtable<>();

        properties.put(JAX_RS_EXTENSION, true);
        properties.put(JAX_RS_NAME, name);
        properties.putIfAbsent(
            JAX_RS_APPLICATION_SELECT, "(osgi.jaxrs.name=*)");

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

}
