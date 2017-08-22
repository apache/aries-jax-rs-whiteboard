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

import org.apache.aries.jax.rs.whiteboard.internal.Utils.ServiceTuple;
import org.apache.aries.osgi.functional.OSGi;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
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
import java.util.function.Function;

import static java.lang.String.format;
import static org.apache.aries.jax.rs.whiteboard.internal.AriesJaxRSServiceRuntime.getApplicationName;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.deployRegistrator;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.getProperties;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.highestPer;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.onlyGettables;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.safeRegisterEndpoint;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.safeRegisterExtension;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.safeRegisterGeneric;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.service;
import static org.apache.aries.osgi.functional.OSGi.all;
import static org.apache.aries.osgi.functional.OSGi.bundleContext;
import static org.apache.aries.osgi.functional.OSGi.just;
import static org.apache.aries.osgi.functional.OSGi.register;
import static org.apache.aries.osgi.functional.OSGi.serviceReferences;
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

    public static final Function<ServiceTuple<Application>, String> APPLICATION_BASE = ((Function<ServiceTuple<Application>, ServiceReference<Application>>) ServiceTuple::getServiceReference).andThen(Utils::getProperties).andThen(Whiteboard::getApplicationBase);
    public static final String DEFAULT_NAME = ".default";

    public static OSGi<Void> createWhiteboard(Dictionary<String, ?> configuration) {
        AriesJaxRSServiceRuntime runtime = new AriesJaxRSServiceRuntime();

        Map<String, ?> configurationMap = Maps.from(configuration);

        return
            bundleContext().flatMap(bundleContext ->
            registerJaxRSServiceRuntime(runtime, bundleContext, configurationMap).flatMap(runtimeRegistration ->
            createDefaultJaxRsServiceRegistrator(configurationMap, runtime).flatMap(defaultServiceRegistrator ->
            just(new ServiceRegistrationChangeCounter(runtimeRegistration)).flatMap(counter ->
            just(runtimeRegistration.getReference()).flatMap(runtimeReference ->
                all(
                    countChanges(whiteboardApplications(runtimeReference, runtime, Maps.from(configuration)), counter),
                    countChanges(whiteBoardApplicationSingletons(runtimeReference, runtime), counter),
                    countChanges(whiteboardExtensions(runtimeReference, defaultServiceRegistrator, runtime), counter),
                    countChanges(whiteboardSingletons(runtimeReference, defaultServiceRegistrator, runtime), counter)
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

    private static ExtensionManagerBus createBus(
        BundleContext bundleContext, Map<String, ?> configuration) {

        BundleWiring wiring = bundleContext.getBundle().adapt(
            BundleWiring.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>)configuration;

        properties.put("org.apache.cxf.bus.id", configuration.get(
            Constants.SERVICE_PID));

        ExtensionManagerBus bus = new ExtensionManagerBus(
            null, properties, wiring.getClassLoader());

        bus.initialize();

        return bus;
    }

    private static OSGi<CXFJaxRsServiceRegistrator>
        createDefaultJaxRsServiceRegistrator(
            Map<String, ?> configuration, AriesJaxRSServiceRuntime runtime) {

        Map<String, Object> properties = new HashMap<>(configuration);
        properties.put(JAX_RS_NAME, ".default");
        properties.put(JAX_RS_APPLICATION_BASE, "/");
        properties.put("service.id", (long)-1);

        return
            bundleContext().flatMap(bundleContext ->
            just(createBus(bundleContext, configuration)).flatMap(bus ->
            registerCXFServletService(bus, "", configuration).foreach(
                __ -> runtime.setApplicationForPath("/", properties),
                __ -> runtime.unsetApplicationForPath("/")).then(
            just(new CXFJaxRsServiceRegistrator(bus, new DefaultApplication()))
        )));
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
            JaxRSServiceRuntime runtime,
            BundleContext bundleContext, Map<String, ?> configuration) {

        Map<String, Object> properties = new HashMap<>(configuration);

        properties.putIfAbsent(
            HTTP_WHITEBOARD_TARGET, "(osgi.http.endpoint=*)");

        properties.putIfAbsent(Constants.SERVICE_RANKING, -1);

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

                return register(JaxRSServiceRuntime.class, runtime, properties);
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

    private static OSGi<?> whiteBoardApplicationSingletons(
        ServiceReference<?> jaxRsRuntimeServiceReference,
        AriesJaxRSServiceRuntime runtime) {
        return
            serviceReferences(format("(%s=*)", JAX_RS_APPLICATION_SELECT)).
                filter(new TargetFilter<>(jaxRsRuntimeServiceReference)).
                flatMap(ref ->
            just(ref.getProperty(JAX_RS_APPLICATION_SELECT).toString()).
                flatMap(applicationFilter ->
            serviceReferences(
                CXFJaxRsServiceRegistrator.class, applicationFilter).
                flatMap(registratorReference ->
            just(
                getApplicationName(getProperties(registratorReference))).
                flatMap(applicationName ->
            waitForExtensionDependencies(ref,
                service(registratorReference).flatMap(registrator ->
                safeRegisterGeneric(ref, applicationName, registrator, runtime)
        ))))));
    }

    private static OSGi<?> whiteboardApplications(
        ServiceReference<?> jaxRsRuntimeServiceReference,
        AriesJaxRSServiceRuntime runtime,
        Map<String, ?> configuration) {

        OSGi<ServiceTuple<Application>> gettableAplicationForWhiteboard =
            onlyGettables(
                getApplicationsForWhiteboard(jaxRsRuntimeServiceReference),
                runtime::addNotGettableApplication,
                runtime::removeNotGettableApplication);

        OSGi<ServiceTuple<Application>> highestRankedPerPath = highestPer(
            APPLICATION_BASE, gettableAplicationForWhiteboard,
            t -> runtime.addShadowedApplication(t.getServiceReference()),
            t -> runtime.removeShadowedApplication(t.getServiceReference())
        );

        return
            bundleContext().flatMap(
                bundleContext -> highestRankedPerPath.flatMap(
                    ref -> deployApplication(configuration, bundleContext, ref)
                ).map(
                    ServiceTuple::getServiceReference
                ).map(
                    Utils::getProperties
                ).foreach(
                    p -> runtime.setApplicationForPath(
                        getApplicationBase(p), p),
                    p -> runtime.unsetApplicationForPath(getApplicationBase(p))
                )
            );
    }

    private static OSGi<ServiceTuple<Application>> deployApplication(
        Map<String, ?> configuration, BundleContext bundleContext,
        ServiceTuple<Application> tuple) {

        ExtensionManagerBus bus = createBus(bundleContext, configuration);

        ServiceReference<Application> serviceReference =
            tuple.getServiceReference();

        Map<String, Object> properties = getProperties(serviceReference);

        properties.computeIfAbsent(
            JAX_RS_NAME,
            (__) -> AriesJaxRSServiceRuntime.generateApplicationName(
                properties));

        return
            all(
                deployRegistrator(bus, tuple.getService(), properties),
                registerCXFServletService(
                    bus, getApplicationBase(properties), properties)).
            then(
                just(tuple)
            );
    }

    private static OSGi<ServiceReference<Application>>
        getApplicationsForWhiteboard(
            ServiceReference<?> jaxRsRuntimeServiceReference) {

        return
            serviceReferences(Application.class, getApplicationFilter()).
            filter(new TargetFilter<>(jaxRsRuntimeServiceReference));
    }

    private static OSGi<?> whiteboardExtensions(
        ServiceReference<?> jaxRsRuntimeServiceReference,
        CXFJaxRsServiceRegistrator defaultServiceRegistrator,
        AriesJaxRSServiceRuntime runtime) {

        return
            serviceReferences(getExtensionFilter()).
                filter(new TargetFilter<>(jaxRsRuntimeServiceReference)).
                flatMap(ref ->
            waitForExtensionDependencies(ref,
                safeRegisterExtension(
                    ref, DEFAULT_NAME, defaultServiceRegistrator, runtime)
            ).foreach(
                __ -> runtime.addApplicationExtension(DEFAULT_NAME, ref),
                __ -> runtime.removeApplicationExtension(DEFAULT_NAME, ref)
            )
        );
    }

    private static OSGi<?> whiteboardSingletons(
        ServiceReference<?> jaxRsRuntimeServiceReference,
        CXFJaxRsServiceRegistrator defaultServiceRegistrator,
        AriesJaxRSServiceRuntime runtime) {

        return
            serviceReferences(getSingletonsFilter()).
                filter(new TargetFilter<>(jaxRsRuntimeServiceReference)).
                flatMap(ref ->
            waitForExtensionDependencies(
                ref,
                safeRegisterEndpoint(
                    ref, DEFAULT_NAME, defaultServiceRegistrator,
                    runtime)
            ).foreach(
                __ -> runtime.addApplicationEndpoint(DEFAULT_NAME, ref),
                __ -> runtime.removeApplicationExtension(DEFAULT_NAME, ref)
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

    private static OSGi<ServiceRegistration<Servlet>> registerCXFServletService(
        Bus bus, String address, Map<String, ?> configuration) {

        Map<String, Object> properties = new HashMap<>(configuration);

        properties.putIfAbsent(
            HTTP_WHITEBOARD_TARGET, "(osgi.http.endpoint=*)");

        properties.putIfAbsent(
            HTTP_WHITEBOARD_CONTEXT_SELECT,
            format(
                "(%s=%s)",
                HTTP_WHITEBOARD_CONTEXT_NAME,
                HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME));

        properties.putIfAbsent(HTTP_WHITEBOARD_SERVLET_PATTERN, address + "/*");

        CXFNonSpringServlet cxfNonSpringServlet = createCXFServlet(bus);

        return register(Servlet.class, cxfNonSpringServlet, properties);
    }

    private static CXFNonSpringServlet createCXFServlet(Bus bus) {
        CXFNonSpringServlet cxfNonSpringServlet = new CXFNonSpringServlet();
        cxfNonSpringServlet.setBus(bus);
        return cxfNonSpringServlet;
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

    public static String getApplicationBase(
        Map<String, Object> properties) {

        return properties.get(JAX_RS_APPLICATION_BASE).toString();
    }



}
