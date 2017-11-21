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

import org.apache.aries.jax.rs.whiteboard.internal.Utils.ApplicationExtensionRegistration;
import org.apache.aries.jax.rs.whiteboard.internal.Utils.PropertyHolder;
import org.apache.aries.jax.rs.whiteboard.internal.Utils.ServiceTuple;
import org.apache.aries.osgi.functional.CachingServiceReference;
import org.apache.aries.osgi.functional.OSGi;
import org.apache.aries.osgi.functional.OSGiResult;
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
import org.osgi.service.jaxrs.runtime.JaxRSServiceRuntime;

import javax.servlet.Servlet;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Feature;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.String.format;
import static org.apache.aries.jax.rs.whiteboard.internal.AriesJaxRSServiceRuntime.getApplicationName;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.canonicalize;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.generateApplicationName;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.getProperties;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.highestPer;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.ignoreResult;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.onlyGettables;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.service;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.updateProperty;
import static org.apache.aries.osgi.functional.OSGi.all;
import static org.apache.aries.osgi.functional.OSGi.effects;
import static org.apache.aries.osgi.functional.OSGi.just;
import static org.apache.aries.osgi.functional.OSGi.nothing;
import static org.apache.aries.osgi.functional.OSGi.onClose;
import static org.apache.aries.osgi.functional.OSGi.once;
import static org.apache.aries.osgi.functional.OSGi.register;
import static org.apache.aries.osgi.functional.OSGi.serviceReferences;
import static org.apache.aries.osgi.functional.Utils.highest;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED;
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

    static final Collection<String> SUPPORTED_EXTENSION_INTERFACES =
        Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                ContainerRequestFilter.class.getName(),
                ContainerResponseFilter.class.getName(),
                ReaderInterceptor.class.getName(),
                WriterInterceptor.class.getName(),
                MessageBodyReader.class.getName(),
                MessageBodyWriter.class.getName(),
                ContextResolver.class.getName(),
                ExceptionMapper.class.getName(),
                ParamConverterProvider.class.getName(),
                Feature.class.getName(),
                DynamicFeature.class.getName()
        )));
    static final String DEFAULT_NAME = ".default";
    private static final Function<ServiceTuple<Application>, String>
        APPLICATION_BASE =
        ((Function<ServiceTuple<Application>, CachingServiceReference<Application>>)
            ServiceTuple::getCachingServiceReference).andThen(
                sr -> getApplicationBase(sr::getProperty));
    private static final Function<ServiceTuple<Application>, String>
        APPLICATION_NAME =
        ((Function<ServiceTuple<Application>, CachingServiceReference<Application>>)
            ServiceTuple::getCachingServiceReference).andThen(
            sr -> getApplicationName(sr::getProperty));

    private final AriesJaxRSServiceRuntime _runtime;
    private final Map<String, ?> _configurationMap;
    private final BundleContext _bundleContext;
    private ServiceRegistrationChangeCounter _counter;
    private ServiceReference<?> _runtimeReference;
    private final List<Object> _endpoints;
    private ServiceRegistration<?> _runtimeRegistration;
    private OSGiResult _osgiResult;

    private Whiteboard(
        BundleContext bundleContext, Dictionary<String, ?> configuration) {

        _bundleContext = bundleContext;
        _runtime = new AriesJaxRSServiceRuntime();
        _configurationMap = Maps.from(configuration);
        _endpoints = new ArrayList<>();
    }

    public static Whiteboard createWhiteboard(
        BundleContext bundleContext, Dictionary<String, ?> configuration) {

        return new Whiteboard(bundleContext, configuration);
    }

    public void start() {
        _runtimeRegistration = registerJaxRSServiceRuntime(
            new HashMap<>(_configurationMap));
        _runtimeReference = _runtimeRegistration.getReference();
        _counter = new ServiceRegistrationChangeCounter(_runtimeRegistration);

        OSGi<Void> program = all(
            ignoreResult(registerDefaultApplication()),
            ignoreResult(applications()),
            ignoreResult(applicationResources()),
            ignoreResult(applicationExtensions()
        ));

        _osgiResult = program.run(_bundleContext);
    }

    public void stop() {
        _osgiResult.close();

        _runtimeRegistration.unregister();
    }

    public void addHttpEndpoints(List<String> endpoints) {
        synchronized (_runtimeRegistration) {
            _endpoints.addAll(endpoints);

            updateProperty(
                _runtimeRegistration, JAX_RS_SERVICE_ENDPOINT, _endpoints);
        }
    }

    private OSGi<?> applicationExtensions() {
        return
            onlySupportedInterfaces(
                    countChanges(
                        getApplicationExtensionsForWhiteboard(), _counter),
                    _runtime::addInvalidExtension,
                    _runtime::removeInvalidExtension).
                flatMap(resourceReference ->
            chooseApplication(
                    resourceReference, Whiteboard::allApplicationReferences,
                    _runtime::addApplicationDependentExtension,
                    _runtime::removeApplicationDependentExtension).
                flatMap(registratorReference ->
            waitForExtensionDependencies(
                resourceReference,
                getApplicationName(registratorReference::getProperty),
            safeRegisterExtension(resourceReference, registratorReference)
        )));
    }

    private OSGi<?> applicationResources() {
        return
            countChanges(getResourcesForWhiteboard(), _counter).
                flatMap(resourceReference ->
            chooseApplication(
                resourceReference, this::defaultApplication,
                _runtime::addApplicationDependentResource,
                _runtime::removeApplicationDependentResource).
                flatMap(registratorReference ->
            waitForExtensionDependencies(
                resourceReference,
                getApplicationName(registratorReference::getProperty),
            safeRegisterEndpoint(resourceReference, registratorReference)
        )));
    }

    private OSGi<?> applications() {
        OSGi<ServiceTuple<Application>> gettableAplicationForWhiteboard =
            onlyGettables(
                countChanges(
                    getApplicationsForWhiteboard(), _counter).
                flatMap(
                    sr -> waitForApplicationDependencies(sr, just(sr))),
                _runtime::addNotGettableApplication,
                _runtime::removeNotGettableApplication);

        OSGi<ServiceTuple<Application>> highestRankedPerName = highestPer(
            APPLICATION_NAME, gettableAplicationForWhiteboard,
            t -> _runtime.addClashingApplication(t.getCachingServiceReference()),
            t -> _runtime.removeClashingApplication(t.getCachingServiceReference())
        );

        OSGi<ServiceTuple<Application>> highestRankedPerPath = highestPer(
            APPLICATION_BASE, highestRankedPerName,
            t -> _runtime.addShadowedApplication(t.getCachingServiceReference()),
            t -> _runtime.removeShadowedApplication(t.getCachingServiceReference())
        );

        return
            highestRankedPerPath.recoverWith(
                (t, e) ->
                    just(t).map(
                        ServiceTuple::getCachingServiceReference
                    ).effects(
                        _runtime::addErroredApplication,
                        _runtime::removeErroredApplication
                    ).then(
                        nothing()
                    )
            ).flatMap(
                this::deployApplication
            ).map(
                ServiceTuple::getCachingServiceReference
            ).map(
                Utils::getProperties
            ).foreach(
                p -> _runtime.setApplicationForPath(
                    getApplicationBase(p::get), p),
                p -> _runtime.unsetApplicationForPath(
                    getApplicationBase(p::get))
            );
    }

    private ExtensionManagerBus createBus() {
        BundleWiring wiring = _bundleContext.getBundle().adapt(
            BundleWiring.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = new HashMap<>(_configurationMap);

        ExtensionManagerBus bus = new ExtensionManagerBus(
            null, properties, wiring.getClassLoader());

        bus.initialize();

        return bus;
    }

    private OSGi<CachingServiceReference<CXFJaxRsServiceRegistrator>>
        defaultApplication() {

        return
            highest(
                serviceReferences(
                    CXFJaxRsServiceRegistrator.class,
                    String.format("(%s=%s)", JAX_RS_NAME, DEFAULT_NAME)
                ).filter(
                    new TargetFilter<>(_runtimeReference)
                )
            );
    }

    private OSGi<ServiceTuple<Application>> deployApplication(
        ServiceTuple<Application> tuple) {

        return
            just(this::createBus).flatMap(bus ->
            just(() -> {
                CachingServiceReference<Application> serviceReference =
                    tuple.getCachingServiceReference();

                Map<String, Object> properties = getProperties(
                    serviceReference);

                properties.computeIfAbsent(
                    JAX_RS_NAME, (__) -> generateApplicationName(
                        serviceReference::getProperty));

                return properties;
            }).flatMap(properties ->
            deployRegistrator(bus, tuple, properties).then(
            registerCXFServletService(
                bus, getApplicationBase(properties::get), properties)).then(
            just(tuple)
        )));
    }

    private OSGi<?> deployRegistrator(
        Bus bus, ServiceTuple<Application> tuple, Map<String, Object> props) {

        return
            just(() -> new CXFJaxRsServiceRegistrator(bus, tuple.getService())).
                flatMap(registrator ->

            onClose(registrator::close).then(
            register(CXFJaxRsServiceRegistrator.class, registrator, props)));
    }

    private OSGi<CachingServiceReference<Object>>
        getApplicationExtensionsForWhiteboard() {

        return serviceReferences(getApplicationExtensionsFilter()).
            filter(new TargetFilter<>(_runtimeReference));
    }

    private OSGi<CachingServiceReference<Application>>
        getApplicationsForWhiteboard() {

        return
            serviceReferences(Application.class, getApplicationFilter()).
            filter(new TargetFilter<>(_runtimeReference));
    }

    private OSGi<CachingServiceReference<Object>> getResourcesForWhiteboard() {
        return serviceReferences(getResourcesFilter()).
            filter(
                new TargetFilter<>(_runtimeReference));
    }

    private OSGi<ServiceRegistration<Application>>
        registerDefaultApplication() {

        return
            just(() -> {
                Map<String, Object> properties = new HashMap<>(
                    _configurationMap);
                properties.put(JAX_RS_NAME, DEFAULT_NAME);
                properties.put(JAX_RS_APPLICATION_BASE, "/");
                properties.put("service.ranking", Integer.MIN_VALUE);

                return properties;
        }).flatMap(properties ->
        register(
            Application.class,
            new DefaultApplication() {

                @Override
                public Set<Object> getSingletons() {
                    Object defaultApplication = _configurationMap.get(
                        "org.apache.aries.jax.rs.whiteboard.default." +
                            "application");

                    if (defaultApplication == null ||
                        Boolean.parseBoolean(defaultApplication.toString())) {

                        return Collections.singleton(new DefaultWeb());
                    }
                    else {
                        return Collections.emptySet();
                    }
                }

            },
            properties));
    }

    private ServiceRegistration<?> registerJaxRSServiceRuntime(
        Map<String, Object> properties) {

        properties.putIfAbsent(Constants.SERVICE_RANKING, Integer.MIN_VALUE);

        return _bundleContext.registerService(
            JaxRSServiceRuntime.class, _runtime, new Hashtable<>(properties));
    }

    public void removeHttpEndpoints(List<String> endpoints) {
        synchronized (_runtimeRegistration) {
            _endpoints.removeAll(endpoints);

            updateProperty(
                _runtimeRegistration, JAX_RS_SERVICE_ENDPOINT, _endpoints);
        }
    }

    private <T> OSGi<?> safeRegisterEndpoint(
        CachingServiceReference<T> serviceReference,
        CachingServiceReference<CXFJaxRsServiceRegistrator> registratorReference) {

        String applicationName = getApplicationName(
            registratorReference::getProperty);

        return
            service(registratorReference).flatMap(registrator ->
            onlyGettables(
                just(serviceReference),
                _runtime::addNotGettableEndpoint,
                _runtime::removeNotGettableEndpoint
            ).recoverWith((t, e) ->
                just(serviceReference).
                effects(
                    _runtime::addErroredEndpoint,
                    _runtime::removeErroredEndpoint).
                then(nothing())
            ).map(
                ServiceTuple::getCachingServiceReference
            ).flatMap(
                Utils::serviceObjects
            ).map(
                Utils::getResourceProvider
            ).effects(
                registrator::add,
                registrator::remove
            ).effects(
                __ -> _runtime.addApplicationEndpoint(
                    applicationName, serviceReference),
                __ -> _runtime.removeApplicationEndpoint(
                    applicationName, serviceReference)
            ));
    }

    private OSGi<?> safeRegisterExtension(
        CachingServiceReference<?> serviceReference,
        CachingServiceReference<CXFJaxRsServiceRegistrator> registratorReference) {

        return
            just(() -> getApplicationName(registratorReference::getProperty)).
                flatMap(applicationName ->
            just(() ->{
                Map<String, Object> properties = getProperties(
                    serviceReference);

                properties.put(JAX_RS_NAME, applicationName);
                properties.put(
                    "original.objectClass",
                    serviceReference.getProperty("objectClass"));

                return properties;
            }).flatMap(properties ->
            service(registratorReference).flatMap(registrator ->
            onlyGettables(
                just(serviceReference),
                _runtime::addNotGettableExtension,
                _runtime::removeNotGettableExtension
            ).recoverWith(
                (t, e) ->
                    just(t.getCachingServiceReference()).
                    effects(
                        _runtime::addErroredExtension,
                        _runtime::removeErroredExtension
                    ).
                    then(nothing())
            ).effects(
                registrator::addProvider,
                registrator::removeProvider
            ).effects(
                __ -> _runtime.addApplicationExtension(
                    applicationName, serviceReference),
                __ -> _runtime.removeApplicationExtension(
                    applicationName, serviceReference)
            ).then(
            register(
                ApplicationExtensionRegistration.class,
                new ApplicationExtensionRegistration(){}, properties)
            ))));
    }



    private OSGi<CachingServiceReference<Application>>
        waitForApplicationDependencies(
            CachingServiceReference<Application> applicationReference,
            OSGi<CachingServiceReference<Application>> program) {

        String[] extensionDependencies = canonicalize(
            applicationReference.getProperty(JAX_RS_EXTENSION_SELECT));

        if (extensionDependencies.length > 0) {
            program = effects(
                () -> _runtime.addDependentApplication(applicationReference),
                () -> _runtime.removeDependentApplication(applicationReference)
            ).then(program);
        }
        else {
            return program;
        }

        for (String extensionDependency : extensionDependencies) {
            extensionDependency = String.format(
                "(&(!(objectClass=%s))%s)",
                ApplicationExtensionRegistration.class.getName(),
                extensionDependency);

            program =
                once(serviceReferences(extensionDependency)).
                    flatMap(
                        sr -> {
                            Object applicationSelectProperty =
                                sr.getProperty(JAX_RS_APPLICATION_SELECT);

                            if (applicationSelectProperty == null) {
                                return just(applicationReference);
                            }

                            Filter filter;

                            try {
                                filter = _bundleContext.createFilter(
                                    applicationSelectProperty.toString());
                            }
                            catch (InvalidSyntaxException e) {
                                return nothing();
                            }

                            if (filter.match(
                                applicationReference.getServiceReference())) {

                                return just(applicationReference);
                            }

                            return nothing();
                        }
                    ).effects(
                    __ -> {},
                    __ -> _runtime.addDependentApplication(
                        applicationReference)
                ).
                    then(program);
        }

        program = program.effects(
            __ -> _runtime.removeDependentApplication(applicationReference),
            __ -> {}
        );

        return program;
    }

    private OSGi<?> waitForExtensionDependencies(
        CachingServiceReference<?> serviceReference, String applicationName,
        OSGi<?> program) {

        String[] extensionDependencies = canonicalize(
            serviceReference.getProperty(JAX_RS_EXTENSION_SELECT));

        if (extensionDependencies.length > 0) {
            _runtime.addDependentService(serviceReference);
        }
        else {
            return program;
        }

        for (String extensionDependency : extensionDependencies) {
            try {
                extensionDependency = extensionDependency.replace(
                    "(objectClass=", "(original.objectClass=");

                Filter extensionFilter = _bundleContext.createFilter(
                    extensionDependency);

                program =
                    once(serviceReferences(ApplicationExtensionRegistration.class).
                        filter(
                            sr -> getApplicationName(sr::getProperty).equals(
                                applicationName)
                        ).map(
                            CachingServiceReference::getServiceReference
                        ).filter(
                            extensionFilter::match
                        )
                    ).effects(
                        __ -> {},
                        __ -> _runtime.addDependentService(serviceReference)
                    ).
                    then(program);
            }
            catch (InvalidSyntaxException e) {

            }
        }

        program = onClose(
            () -> _runtime.removeDependentService(serviceReference)).
            then(program);

        program = program.foreach(
            __ -> _runtime.removeDependentService(serviceReference),
            __ -> {}
        );

        return program;
    }

    static String getApplicationBase(PropertyHolder properties) {
        return properties.get(JAX_RS_APPLICATION_BASE).toString();
    }

    private static OSGi<CachingServiceReference<CXFJaxRsServiceRegistrator>>
        allApplicationReferences() {

        return serviceReferences(CXFJaxRsServiceRegistrator.class);
    }

    private static OSGi<CachingServiceReference<CXFJaxRsServiceRegistrator>>
        chooseApplication(
            CachingServiceReference<?> serviceReference,
            Supplier<OSGi<CachingServiceReference<CXFJaxRsServiceRegistrator>>>
                theDefault,
            Consumer<CachingServiceReference<?>> onWaiting,
            Consumer<CachingServiceReference<?>> onResolved) {

        Object applicationSelectProperty = serviceReference.getProperty(
            JAX_RS_APPLICATION_SELECT);

        if (applicationSelectProperty == null) {
            return theDefault.get();
        }

        return
            just(0).
            effects(
                __ -> onWaiting.accept(serviceReference),
                __ -> onResolved.accept(serviceReference)).then(
            serviceReferences(
                CXFJaxRsServiceRegistrator.class,
                applicationSelectProperty.toString()).
            effects(__ -> onResolved.accept(serviceReference), __ -> {}));
    }

    private static <T> OSGi<T> countChanges(
        OSGi<T> program, ChangeCounter counter) {

        return program.flatMap(t -> {
            counter.inc();

            return onClose(counter::inc).then(just(t));
        });
    }

    private static CXFNonSpringServlet createCXFServlet(Bus bus) {
        CXFNonSpringServlet cxfNonSpringServlet = new CXFNonSpringServlet();
        cxfNonSpringServlet.setBus(bus);
        return cxfNonSpringServlet;
    }

    private static String getApplicationExtensionsFilter() {
        return format(
            "(&(!(objectClass=%s))(%s=%s)%s)",
            ApplicationExtensionRegistration.class.getName(),
            JAX_RS_EXTENSION, true, getExtensionsFilter());
    }

    private static String getApplicationFilter() {
        return format("(%s=*)", JAX_RS_APPLICATION_BASE);
    }

    private static String getExtensionsFilter() {
        return format("(%s=true)", JAX_RS_EXTENSION);
    }

    private static String getResourcesFilter() {
        return format("(%s=true)", JAX_RS_RESOURCE);
    }

    private static OSGi<CachingServiceReference<Object>> onlySupportedInterfaces(
        OSGi<CachingServiceReference<Object>> program,
        Consumer<CachingServiceReference<?>> onInvalidAdded,
        Consumer<CachingServiceReference<?>> onInvalidRemoved) {

        return program.flatMap(sr -> {
            if (signalsValidInterface(sr)) {
                return just(sr);
            }
            else {
                onInvalidAdded.accept(sr);
                return
                    onClose(() -> onInvalidRemoved.accept(sr)).then(nothing());
            }
        });
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

        if (!address.startsWith("/")) {
            address = "/" + address;
        }

        if (address.endsWith("/")) {
            address = address.substring(0, address.length() - 1);
        }

        properties.putIfAbsent(HTTP_WHITEBOARD_SERVLET_PATTERN, address + "/*");
        properties.putIfAbsent(HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED, true);

        CXFNonSpringServlet cxfNonSpringServlet = createCXFServlet(bus);

        return register(Servlet.class, cxfNonSpringServlet, properties);
    }

    private static boolean signalsValidInterface(
        CachingServiceReference<Object> serviceReference) {

        String[] objectClasses = canonicalize(serviceReference.getProperty(
            "objectClass"));

        return Arrays.stream(objectClasses).
            anyMatch(SUPPORTED_EXTENSION_INTERFACES::contains);
    }

    private interface ChangeCounter {

        void inc();

    }

    private static class ServiceRegistrationChangeCounter
        implements ChangeCounter{

        private static final String changecount = "service.changecount";
        private final AtomicLong _atomicLong = new AtomicLong();
        private final ServiceRegistration<?> _serviceRegistration;

        ServiceRegistrationChangeCounter(
            ServiceRegistration<?> serviceRegistration) {

            _serviceRegistration = serviceRegistration;
        }

        @Override
        public void inc() {
            long l = _atomicLong.incrementAndGet();

            synchronized (_serviceRegistration) {
                updateProperty(_serviceRegistration, changecount, l);
            }
        }
    }

}
