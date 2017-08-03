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

package org.apache.aries.jax.rs.whiteboard.internal;

import org.apache.aries.osgi.functional.OSGi;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.jaxrs.runtime.JaxRSServiceRuntime;

import javax.servlet.Servlet;
import javax.ws.rs.core.Application;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.String.format;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.cxfRegistrator;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.repeatInOrder;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.safeRegisterEndpoint;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.safeRegisterExtension;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.safeRegisterGeneric;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.service;
import static org.apache.aries.osgi.functional.OSGi.all;
import static org.apache.aries.osgi.functional.OSGi.bundleContext;
import static org.apache.aries.osgi.functional.OSGi.just;
import static org.apache.aries.osgi.functional.OSGi.register;
import static org.apache.aries.osgi.functional.OSGi.serviceReferences;
import static org.apache.aries.osgi.functional.OSGi.services;
import static org.osgi.service.http.runtime.HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET;
import static org.osgi.service.jaxrs.runtime.JaxRSServiceRuntimeConstants.JAX_RS_SERVICE_ENDPOINT;
import static org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE;
import static org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants.JAX_RS_APPLICATION_SELECT;
import static org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants.JAX_RS_EXTENSION;
import static org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants.JAX_RS_EXTENSION_SELECT;
import static org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants.JAX_RS_NAME;
import static org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants.JAX_RS_RESOURCE;

/**
 * @author Carlos Sierra Andrés
 */
public class Whiteboard {
    public static OSGi<Void> createWhiteboard(Dictionary<String, ?> configuration) {
        return
            bundleContext().flatMap(bundleContext ->
            just(createBus(bundleContext, configuration)).flatMap(bus ->
            just(createDefaultJaxRsServiceRegistrator(bus)).flatMap(defaultServiceRegistrator ->
            registerJaxRSServiceRuntime(bundleContext, bus, Maps.from(configuration)).flatMap(registratorRegistration ->
            just(new ServiceRegistrationChangeCounter(registratorRegistration)).flatMap(counter ->
                all(
                    countChanges(whiteboardApplications(bus), counter),
                    countChanges(whiteBoardApplicationSingletons(), counter),
                    countChanges(whiteboardExtensions(defaultServiceRegistrator), counter),
                    countChanges(whiteboardSingletons(defaultServiceRegistrator), counter)
            ))))));
    }

    private static OSGi<Collection<String>> bestEffortCalculationOfEnpoints(Filter filter) {
        Collection<String> endPoints = new ArrayList<>();

        return
            serviceReferences(HttpServiceRuntime.class, filter.toString()).
                foreach(
                    reference -> Strings.stringPlus(reference.getProperty(HTTP_SERVICE_ENDPOINT)).
                        ifPresent(endPoints::addAll)
                    ,
                    reference -> Strings.stringPlus(reference.getProperty(HTTP_SERVICE_ENDPOINT)).
                        ifPresent(values -> values.forEach(endPoints::remove))
                ).then(
            just(endPoints)
        );
    }

    private static String buildExtensionFilter(String filter) {
        return String.format("(&%s%s)", getExtensionFilter(), filter);
    }

    private static String[] canonicalize(Object propertyValue) {
        if (propertyValue == null) {
            return new String[0];
        }
        if (propertyValue instanceof String[]) {
            return (String[]) propertyValue;
        }
        return new String[]{propertyValue.toString()};
    }

    private static ExtensionManagerBus createBus(BundleContext bundleContext, Dictionary<String, ?> configuration) {
        BundleWiring wiring = bundleContext.getBundle().adapt(BundleWiring.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = Maps.from((Dictionary<String, Object>)configuration);

        properties.put("org.apache.cxf.bus.id", configuration.get(Constants.SERVICE_PID));

        ExtensionManagerBus bus = new ExtensionManagerBus(null, properties, wiring.getClassLoader());

        bus.initialize();

        return bus;
    }

    private static CXFJaxRsServiceRegistrator createDefaultJaxRsServiceRegistrator(
        ExtensionManagerBus bus) {

        Map<String, Object> properties = new HashMap<>();
        properties.put(JAX_RS_APPLICATION_BASE, "/");
        properties.put(JAX_RS_NAME, ".default");

        return new CXFJaxRsServiceRegistrator(
            bus, new DefaultApplication(), properties);
    }

    private static String getApplicationFilter() {
        return format("(%s=*)", JAX_RS_APPLICATION_BASE);
    }

    private static String getExtensionFilter() {
        return format("(%s=*)", JAX_RS_EXTENSION);
    }

    private static String getSingletonsFilter() {
        return format("(%s=true)", JAX_RS_RESOURCE);
    }

    private static OSGi<ServiceRegistration<?>>
        registerJaxRSServiceRuntime(
            BundleContext bundleContext, Bus bus, Map<String, ?> configuration) {

        Map<String, Object> properties = new HashMap<>(configuration);

        properties.putIfAbsent(
            HTTP_WHITEBOARD_TARGET, "(osgi.http.endpoint=*)");

        properties.putIfAbsent(
            HTTP_WHITEBOARD_CONTEXT_SELECT,
            format(
                "(%s=%s)",
                HTTP_WHITEBOARD_CONTEXT_NAME,
                HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME));

        properties.putIfAbsent(HTTP_WHITEBOARD_SERVLET_PATTERN, "/*");

        properties.put(Constants.SERVICE_RANKING, -1);

        String targetFilter = (String)properties.get(HTTP_WHITEBOARD_TARGET);

        Filter filter;

        try {
            filter = bundleContext.createFilter(
                format(
                    "(&(objectClass=%s)%s)", HttpServiceRuntime.class.getName(),
                    targetFilter));
        }
        catch (InvalidSyntaxException ise) {
            throw new IllegalArgumentException(
                format("Invalid syntax for filter %s", targetFilter));
        }

        return
            bestEffortCalculationOfEnpoints(filter).flatMap(endpoints -> {
                properties.put(JAX_RS_SERVICE_ENDPOINT, endpoints);

                return register(
                    new String[]{JaxRSServiceRuntime.class.getName(), Servlet.class.getName()},
                    new AriesJaxRSServiceRuntime(bus), properties);
            }
        );
    }

    private static OSGi<?> waitForExtensionDependencies(
        ServiceReference<?> serviceReference, OSGi<?> program) {

        String[] extensionDependencies = canonicalize(
            serviceReference.getProperty(JAX_RS_EXTENSION_SELECT));

        for (String extensionDependency : extensionDependencies) {
            program =
                serviceReferences(buildExtensionFilter(extensionDependency)).
                    then(program);
        }

        return program;
    }

    private static OSGi<?> whiteBoardApplicationSingletons() {
        return
            serviceReferences(format("(%s=*)", JAX_RS_APPLICATION_SELECT)).
                flatMap(ref ->
            just(ref.getProperty(JAX_RS_APPLICATION_SELECT).toString()).
                flatMap(applicationFilter ->
            services(CXFJaxRsServiceRegistrator.class, applicationFilter).
                flatMap(registrator ->
            safeRegisterGeneric(ref, registrator)
        )));
    }

    private static OSGi<?> whiteboardApplications(ExtensionManagerBus bus) {
        return
            repeatInOrder(
                serviceReferences(Application.class, getApplicationFilter())).
                flatMap(ref ->
            just(CXFJaxRsServiceRegistrator.getProperties(ref, JAX_RS_APPLICATION_BASE)).
                flatMap(properties ->
            service(ref).flatMap(application ->
            cxfRegistrator(bus, application, properties)
        )));
    }

    private static OSGi<?> whiteboardExtensions(
        CXFJaxRsServiceRegistrator defaultServiceRegistrator) {

        return
            serviceReferences(getExtensionFilter()).flatMap(ref ->
            waitForExtensionDependencies(ref,
                safeRegisterExtension(ref, defaultServiceRegistrator)
            )
        );
    }

    private static OSGi<?> whiteboardSingletons(
        CXFJaxRsServiceRegistrator defaultServiceRegistrator) {

        return
            serviceReferences(getSingletonsFilter()).
                flatMap(serviceReference ->
            waitForExtensionDependencies(serviceReference,
                safeRegisterEndpoint(
                    serviceReference, defaultServiceRegistrator)
            )
        );
    }

    private static <T> OSGi<T> countChanges(
        OSGi<T> program, ChangeCounter counter) {

        return program.map(t -> {counter.inc(); return t;});
    }

    private static interface ChangeCounter {

        public void inc();

    }

    private static class ServiceRegistrationChangeCounter
        implements ChangeCounter{

        private static final String changecount = "service.changecount";
        private final AtomicLong _atomicLong = new AtomicLong();
        private ServiceRegistration<?> _serviceRegistration;
        private final Hashtable<String, Object> _properties;

        public ServiceRegistrationChangeCounter(
            ServiceRegistration<?> serviceRegistration) {

            _serviceRegistration = serviceRegistration;

            ServiceReference<?> serviceReference =
                _serviceRegistration.getReference();

            _properties = new Hashtable<>();

            for (String propertyKey : serviceReference.getPropertyKeys()) {
                _properties.put(
                    propertyKey, serviceReference.getProperty(propertyKey));
            }
        }

        @Override
        public void inc() {
            long l = _atomicLong.incrementAndGet();

            @SuppressWarnings("unchecked")
            Hashtable<String, Object> properties =
                (Hashtable<String, Object>)_properties.clone();

            properties.put(changecount, l);

            _serviceRegistration.setProperties(properties);
        }
    }

}
