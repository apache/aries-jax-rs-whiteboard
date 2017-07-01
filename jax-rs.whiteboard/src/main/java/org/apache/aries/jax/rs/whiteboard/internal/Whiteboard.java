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

import static java.lang.String.format;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.cxfRegistrator;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.repeatInOrder;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.safeRegisterEndpoint;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.safeRegisterExtension;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.safeRegisterGeneric;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.service;
import static org.apache.aries.osgi.functional.OSGi.just;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;

import javax.servlet.Servlet;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Application;

import org.apache.aries.osgi.functional.OSGi;
import org.apache.aries.osgi.functional.OSGiResult;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.jaxrs.runtime.JaxRSServiceRuntime;
import org.osgi.util.tracker.ServiceTracker;

public class Whiteboard implements AutoCloseable {

    public Whiteboard(
            BundleContext bundleContext, Map<String, Object> configuration)
        throws ConfigurationException {

        BundleWiring wiring = bundleContext.getBundle().adapt(BundleWiring.class);

        configuration.put(_ID, configuration.get(Constants.SERVICE_PID));

        ExtensionManagerBus bus = new ExtensionManagerBus(null, configuration, wiring.getClassLoader());

        bus.initialize();

        _runtimeRegistration = registerJaxRSServiceRuntime(bundleContext, bus, configuration);

        OSGi<?> applications =
            repeatInOrder(
                serviceReferences(Application.class, getApplicationFilter())).
            flatMap(ref ->
            just(
                CXFJaxRsServiceRegistrator.getProperties(
                    ref, JAX_RS_APPLICATION_BASE)).
                flatMap(properties ->
            service(ref).flatMap(application ->
            cxfRegistrator(bus, application, properties)
        )));

        _applicationsResult = applications.run(bundleContext);

        OSGi<?> applicationSingletons =
            serviceReferences(format("(%s=*)", JAX_RS_APPLICATION_SELECT)).
                flatMap(ref ->
            just(ref.getProperty(JAX_RS_APPLICATION_SELECT).toString()).
                flatMap(applicationFilter ->
            services(CXFJaxRsServiceRegistrator.class, applicationFilter).
                flatMap(registrator ->
            safeRegisterGeneric(ref, registrator)
        )));

        _applicationSingletonsResult = applicationSingletons.run(bundleContext);

        Map<String, Object> properties = new HashMap<>();
        properties.put(JAX_RS_APPLICATION_BASE, "/");
        properties.put(JAX_RS_NAME, ".default");

        CXFJaxRsServiceRegistrator defaultServiceRegistrator =
            new CXFJaxRsServiceRegistrator(
                bus, new DefaultApplication(), properties);

        OSGi<?> extensions =
            serviceReferences(getExtensionFilter()).flatMap(ref ->
            waitForExtensionDependencies(ref,
                safeRegisterExtension(ref, defaultServiceRegistrator)
            )
        );

        _extensionsResult = extensions.run(bundleContext);

        OSGi<?> singletons =
            serviceReferences(getSingletonsFilter()).
                flatMap(serviceReference ->
            waitForExtensionDependencies(serviceReference,
                safeRegisterEndpoint(
                    serviceReference, defaultServiceRegistrator)
            )
        );

        _singletonsResult = singletons.run(bundleContext);

        Dictionary<String, Object> dictionary = new Hashtable<>();
        dictionary.put(JAX_RS_APPLICATION_SELECT, "(osgi.jaxrs.name=.default)");
        dictionary.put(JAX_RS_RESOURCE, "true");
        dictionary.put(Constants.SERVICE_RANKING, -1);

        _defaultWeb =  bundleContext.registerService(
            DefaultWeb.class, new DefaultWeb(), dictionary);
        _clientBuilder = bundleContext.registerService(
            ClientBuilder.class, new ClientBuilderFactory(), null);
    }

    @Override
    public void close() {
        _clientBuilder.unregister();
        _defaultWeb.unregister();
        _applicationsResult.close();
        _applicationSingletonsResult.close();
        _extensionsResult.close();
        _singletonsResult.close();
        _runtimeRegistration.unregister();
    }

    private void bestEffortCalculationOfEnpoints(
            Dictionary<String, Object> properties, BundleContext bundleContext)
        throws ConfigurationException {

        String targetFilter = (String)properties.get(HTTP_WHITEBOARD_TARGET);

        Filter filter;

        try {
            filter = bundleContext.createFilter(format(
                "(&(objectClass=%s)%s)",
                HttpServiceRuntime.class.getName(),
                targetFilter));
        }
        catch (InvalidSyntaxException ise) {
            throw new ConfigurationException(
                HTTP_WHITEBOARD_TARGET, format("Invalid syntax for filter {}", targetFilter));
        }

        ServiceTracker<HttpServiceRuntime, HttpServiceRuntime> httpRuntimeTracker =
            new ServiceTracker<>(bundleContext, filter, null);

        httpRuntimeTracker.open();

        try {
            httpRuntimeTracker.waitForService(1000);
        }
        catch (InterruptedException ie) {
        }

        Optional.ofNullable(
            httpRuntimeTracker.getServiceReferences()
        ).ifPresent(
            array -> {
                Collection<String> endPoints = new ArrayList<>();

                Arrays.stream(array).forEach(
                    reference -> Strings.stringPlus(
                        reference.getProperty(HTTP_SERVICE_ENDPOINT)
                    ).ifPresent(
                        values -> values.stream().forEach(
                            value -> {
                                endPoints.add(value);
                            }
                        )
                    )
                );

                properties.put(JAX_RS_SERVICE_ENDPOINT, endPoints);
            }
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

    private static String getApplicationFilter() {
        return format("(%s=*)", JAX_RS_APPLICATION_BASE);
    }

    private static String getExtensionFilter() {
        return format("(%s=*)", JAX_RS_EXTENSION);
    }

    private static String getSingletonsFilter() {
        return format("(%s=true)", JAX_RS_RESOURCE);
    }

    private ServiceRegistration<?> registerJaxRSServiceRuntime(
            BundleContext bundleContext, Bus bus, Map<String, Object> configuration)
        throws ConfigurationException {

        Dictionary<String, Object> properties = new Hashtable<>(configuration);

        properties.put(
            HTTP_WHITEBOARD_TARGET,
            configuration.computeIfAbsent(
                HTTP_WHITEBOARD_TARGET,
                k -> "(osgi.http.endpoint=*)"));

        properties.put(
            HTTP_WHITEBOARD_CONTEXT_SELECT,
            configuration.computeIfAbsent(
                HTTP_WHITEBOARD_CONTEXT_SELECT,
                    k -> format(
                        "(%s=%s)",
                        HTTP_WHITEBOARD_CONTEXT_NAME,
                        HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME)));

        properties.put(
            HTTP_WHITEBOARD_SERVLET_PATTERN,
            configuration.computeIfAbsent(
                HTTP_WHITEBOARD_SERVLET_PATTERN,
                k -> "/*"));

        properties.put(Constants.SERVICE_RANKING, -1);

        bestEffortCalculationOfEnpoints(properties, bundleContext);

        return bundleContext.registerService(
            new String[] {JaxRSServiceRuntime.class.getName(), Servlet.class.getName()},
            new AriesJaxRSServiceRuntime(bus), properties);
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

    private static final String _ID = "org.apache.cxf.bus.id";

    private final OSGiResult<?> _applicationsResult;
    private final OSGiResult<?> _applicationSingletonsResult;
    private final ServiceRegistration<ClientBuilder> _clientBuilder;
    private final ServiceRegistration<DefaultWeb> _defaultWeb;
    private final OSGiResult<?> _extensionsResult;
    private final ServiceRegistration<?> _runtimeRegistration;
    private final OSGiResult<?> _singletonsResult;

}
