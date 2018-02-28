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

import org.apache.aries.jax.rs.whiteboard.internal.cxf.CxfJaxrsServiceRegistrator;
import org.apache.aries.jax.rs.whiteboard.internal.utils.Utils;
import org.apache.aries.jax.rs.whiteboard.internal.utils.ApplicationExtensionRegistration;
import org.apache.aries.jax.rs.whiteboard.internal.utils.PropertyHolder;
import org.apache.aries.jax.rs.whiteboard.internal.utils.ServiceTuple;
import org.apache.aries.osgi.functional.CachingServiceReference;
import org.apache.aries.osgi.functional.OSGi;
import org.apache.aries.osgi.functional.OSGiResult;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;

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
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static org.apache.aries.jax.rs.whiteboard.internal.AriesJaxrsServiceRuntime.getApplicationName;
import static org.apache.aries.jax.rs.whiteboard.internal.utils.Utils.canonicalize;
import static org.apache.aries.jax.rs.whiteboard.internal.utils.Utils.generateApplicationName;
import static org.apache.aries.jax.rs.whiteboard.internal.utils.Utils.getProperties;
import static org.apache.aries.jax.rs.whiteboard.internal.utils.Utils.highestPer;
import static org.apache.aries.jax.rs.whiteboard.internal.utils.Utils.onlyGettables;
import static org.apache.aries.jax.rs.whiteboard.internal.utils.Utils.service;
import static org.apache.aries.jax.rs.whiteboard.internal.utils.Utils.updateProperty;
import static org.apache.aries.osgi.functional.OSGi.all;
import static org.apache.aries.osgi.functional.OSGi.changeContext;
import static org.apache.aries.osgi.functional.OSGi.effects;
import static org.apache.aries.osgi.functional.OSGi.ignore;
import static org.apache.aries.osgi.functional.OSGi.just;
import static org.apache.aries.osgi.functional.OSGi.nothing;
import static org.apache.aries.osgi.functional.OSGi.onClose;
import static org.apache.aries.osgi.functional.OSGi.once;
import static org.apache.aries.osgi.functional.OSGi.register;
import static org.apache.aries.osgi.functional.OSGi.serviceReferences;
import static org.apache.aries.osgi.functional.Utils.highest;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET;
import static org.osgi.service.jaxrs.runtime.JaxrsServiceRuntimeConstants.JAX_RS_SERVICE_ENDPOINT;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_DEFAULT_APPLICATION;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_EXTENSION;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_EXTENSION_SELECT;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_NAME;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_RESOURCE;

/**
 * @author Carlos Sierra Andrés
 */
public class Whiteboard {

    public static final Map<String, Class<?>> SUPPORTED_EXTENSION_INTERFACES =
        Collections.unmodifiableMap(
            Stream.of(ContainerRequestFilter.class,
                ContainerResponseFilter.class,
                ReaderInterceptor.class,
                WriterInterceptor.class,
                MessageBodyReader.class,
                MessageBodyWriter.class,
                ContextResolver.class,
                ExceptionMapper.class,
                ParamConverterProvider.class,
                Feature.class,
                DynamicFeature.class,
                org.apache.cxf.feature.Feature.class)
            .collect(toMap(Class::getName, Function.identity())));
    static final String DEFAULT_NAME = ".default";
    private static final Function<CachingServiceReference<Application>, String>
        APPLICATION_BASE = sr -> getApplicationBase(sr::getProperty);

    private final AriesJaxrsServiceRuntime _runtime;
    private final Map<String, ?> _configurationMap;
    private final BundleContext _bundleContext;
    private final ServiceRegistrationChangeCounter _counter;
    private final ServiceReference<?> _runtimeReference;
    private final OSGi<Void> _program;
    private final List<Object> _endpoints;
    private final ServiceRegistration<?> _runtimeRegistration;
    private OSGiResult _osgiResult;

    private Whiteboard(
        BundleContext bundleContext, Dictionary<String, ?> configuration) {

        _bundleContext = bundleContext;
        _runtime = new AriesJaxrsServiceRuntime();
        _configurationMap = Maps.from(configuration);
        _endpoints = new ArrayList<>();
        _runtimeRegistration = registerJaxRSServiceRuntime(
            new HashMap<>(_configurationMap));
        _runtimeReference = _runtimeRegistration.getReference();
        _counter = new ServiceRegistrationChangeCounter(_runtimeRegistration);
        _program =
            all(
                ignore(registerDefaultApplication()),
                ignore(getAllServices())
            );
    }

    public static Whiteboard createWhiteboard(
        BundleContext bundleContext, Dictionary<String, ?> configuration) {

        return new Whiteboard(bundleContext, configuration);
    }

    public void start() {
        _osgiResult = _program.run(_bundleContext);
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

    private OSGi<?> applicationExtensions(
        OSGi<CachingServiceReference<Object>> extensions) {

        return
            onlyValid(
                onlySupportedInterfaces(
                        extensions,
                        _runtime::addInvalidExtension,
                        _runtime::removeInvalidExtension),
                    _runtime::addInvalidExtension,
                    _runtime::removeInvalidExtension).
                flatMap(extensionReference ->
            chooseApplication(
                    extensionReference, allApplicationReferences(),
                    _runtime::addApplicationDependentExtension,
                    _runtime::removeApplicationDependentExtension).
                flatMap(registratorReference ->
            waitForExtensionDependencies(
                    extensionReference, registratorReference,
                    _runtime::addDependentExtension,
                    _runtime::removeDependentExtension).
                then(
            safeRegisterExtension(extensionReference, registratorReference)
        )));
    }

    private OSGi<?> applicationResources(
        OSGi<CachingServiceReference<Object>> resources) {

        return
            onlyValid(
                    resources, _runtime::addInvalidResource,
                    _runtime::removeInvalidResource).
                flatMap(resourceReference ->
            chooseApplication(
                    resourceReference, defaultApplication(),
                    _runtime::addApplicationDependentResource,
                    _runtime::removeApplicationDependentResource).
                flatMap(registratorReference ->
            waitForExtensionDependencies(
                    resourceReference, registratorReference,
                    _runtime::addDependentService,
                    _runtime::removeDependentService).
            then(
                safeRegisterEndpoint(resourceReference, registratorReference)
            )));
    }

    @SuppressWarnings("unchecked")
    private OSGi<?> getAllServices() {
        OSGi<CachingServiceReference<Object>> applicationsForWhiteboard =
            (OSGi)getApplicationsForWhiteboard();
        return
            highestPer(
                sr -> getApplicationName(sr::getProperty),
                all(
                    countChanges(getResourcesForWhiteboard(), _counter),
                    countChanges(
                        getApplicationExtensionsForWhiteboard(), _counter),
                    countChanges(applicationsForWhiteboard, _counter)
                ),
                this::registerShadowedService,
                this::unregisterShadowedService
            ).distribute(
                p -> ignore(applications((OSGi)p.filter(this::isApplication))),
                p -> ignore(applicationResources(p.filter(this::isResource))),
                p -> ignore(applicationExtensions(p.filter(this::isExtension)))
            );
    }

    private boolean isApplication(CachingServiceReference<?> sr) {
        return _applicationsFilter.match(sr.getServiceReference());
    }

    private boolean isExtension(CachingServiceReference<?> sr) {
        return _extensionsFilter.match(sr.getServiceReference());
    }

    private boolean isResource(CachingServiceReference<?> sr) {
        return _resourcesFilter.match(sr.getServiceReference());
    }

    private void registerShadowedService(CachingServiceReference<?> sr) {
        if (isApplication(sr)) {
            _runtime.addClashingApplication(sr);
        }
        if (isExtension(sr)) {
            _runtime.addClashingExtension(sr);
        }
        if (isResource(sr)) {
            _runtime.addClashingResource(sr);
        }
    }

    private void unregisterShadowedService(CachingServiceReference<?> sr) {
        if (isApplication(sr)) {
            _runtime.removeClashingApplication(sr);
        }
        if (isExtension(sr)) {
            _runtime.removeClashingExtension(sr);
        }
        if (isApplication(sr)) {
            _runtime.removeClashingResource(sr);
        }
    }

    private static <T> OSGi<CachingServiceReference<T>> onlyValid(
        OSGi<CachingServiceReference<T>> serviceReferences,
        Consumer<CachingServiceReference<T>> onAddingInvalid,
        Consumer<CachingServiceReference<T>> onRemovingInvalid) {

        return serviceReferences.flatMap(serviceReference -> {

            OSGi<CachingServiceReference<T>> error = effects(
                () -> onAddingInvalid.accept(serviceReference),
                () -> onRemovingInvalid.accept(serviceReference)
            ).then(
                nothing()
            );

            Object propertyObject = serviceReference.getProperty(JAX_RS_NAME);

            if (propertyObject != null &&
                !propertyObject.toString().equals(JAX_RS_DEFAULT_APPLICATION) &&
                propertyObject.toString().startsWith(".")) {

                return error;
            }

            if (!testFilters(
                serviceReference.getProperty(JAX_RS_APPLICATION_SELECT))) {

                return error;
            }

            if (!testFilters(
                serviceReference.getProperty(JAX_RS_EXTENSION_SELECT))) {

                return error;
            }

            return just(serviceReference);
        });
    }

    private static <T> boolean testFilters(Object propertyObject) {
        if (propertyObject != null) {
            try {
                String[] properties = canonicalize(propertyObject);

                for (String property : properties) {
                    FrameworkUtil.createFilter(property);
                }
            }
            catch (InvalidSyntaxException e) {
                return false;
            }
        }
        return true;
    }

    private OSGi<ApplicationReferenceWithContext> waitForApplicationContext(
        OSGi<CachingServiceReference<Application>> application,
        Consumer<CachingServiceReference<Application>> onWaitingForContext,
        Consumer<CachingServiceReference<Application>> onResolvedContext) {

        return application.flatMap(serviceReference -> {
            Object propertyObject = serviceReference.getProperty(
                HTTP_WHITEBOARD_CONTEXT_SELECT);

            if (propertyObject == null) {
                return just(
                    new ApplicationReferenceWithContext(
                        null, serviceReference));
            }

            String contextSelect = propertyObject.toString();

            try {
                FrameworkUtil.createFilter(contextSelect);
            }
            catch (InvalidSyntaxException e) {
                return effects(
                    () -> _runtime.addInvalidApplication(serviceReference),
                    () -> _runtime.removeInvalidApplication(serviceReference)
                ).then(
                    nothing()
                );
            }

            return
                effects(
                    () -> onWaitingForContext.accept(serviceReference),
                    () -> onResolvedContext.accept(serviceReference)
                ).then(
            highest(
                serviceReferences(ServletContextHelper.class, contextSelect)
            ).flatMap(
                sr -> just(
                    new ApplicationReferenceWithContext(sr, serviceReference))
            ).effects(
                __ -> onResolvedContext.accept(serviceReference),
                __ -> onWaitingForContext.accept(serviceReference)
            ));
        });
    }

    private OSGi<?> applications(
        OSGi<CachingServiceReference<Application>> applications) {

        OSGi<CachingServiceReference<Application>> applicationsForWhiteboard =
            waitForApplicationDependencies(
                onlyValid(
                    applications,
                    _runtime::addInvalidApplication,
                    _runtime::removeInvalidApplication)
                );

        OSGi<ApplicationReferenceWithContext> applicationsWithContext =
            waitForApplicationContext(
                applicationsForWhiteboard, _runtime::addDependentApplication,
                _runtime::removeDependentApplication);

        OSGi<ApplicationReferenceWithContext> highestRankedPerPath =
            highestPer(
                ApplicationReferenceWithContext::getActualBasePath,
                applicationsWithContext,
                t -> _runtime.addShadowedApplication(
                    t.getApplicationReference()),
                t -> _runtime.removeShadowedApplication(
                    t.getApplicationReference())
        );

        return highestRankedPerPath.flatMap(application ->
            onlyGettables(
                just(application.getApplicationReference()),
                _runtime::addNotGettableApplication,
                _runtime::removeNotGettableApplication).
            recoverWith(
                (t, e) ->
                    just(t).map(
                        ServiceTuple::getCachingServiceReference
                    ).effects(
                        _runtime::addErroredApplication,
                        _runtime::removeErroredApplication
                    ).then(
                        nothing()
                    )
                ).
                flatMap(at ->
            deployApplication(at, application.getContextReference()).foreach(
                registrator ->
                    _runtime.setApplicationForPath(
                        getApplicationBase(
                            at.getCachingServiceReference()::getProperty),
                            at.getCachingServiceReference(), registrator),
                registrator ->
                    _runtime.unsetApplicationForPath(
                        getApplicationBase(
                            at.getCachingServiceReference()::getProperty))
                )
        ));
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

    private OSGi<CachingServiceReference<CxfJaxrsServiceRegistrator>>
        defaultApplication() {

        return
            highest(
                serviceReferences(
                    CxfJaxrsServiceRegistrator.class,
                    String.format("(%s=%s)", JAX_RS_NAME, DEFAULT_NAME)
                ).filter(
                    new TargetFilter<>(_runtimeReference)
                )
            );
    }

    private OSGi<CxfJaxrsServiceRegistrator> deployApplication(
        ServiceTuple<Application> tuple,
        CachingServiceReference<ServletContextHelper> contextReference) {

        Supplier<Map<String, ?>> properties = () -> {
            CachingServiceReference<Application> serviceReference =
                tuple.getCachingServiceReference();

            Map<String, Object> props = getProperties(
                serviceReference);

            props.computeIfAbsent(
                JAX_RS_NAME, (__) -> generateApplicationName(
                    serviceReference::getProperty));

            props.put(
                "original.service.id",
                serviceReference.getProperty("service.id"));

            props.put(
                "original.service.bundleid",
                serviceReference.getProperty("service.bundleid"));

            return props;
        };

        return
            deployRegistrator(tuple, properties).flatMap(registrator ->
            registerCXFServletService(
                registrator.getBus(), properties, contextReference).then(
            just(registrator)
        ));
    }

    private OSGi<CxfJaxrsServiceRegistrator> deployRegistrator(
        ServiceTuple<Application> tuple, Supplier<Map<String, ?>> props) {

        return
            just(() ->
                    new CxfJaxrsServiceRegistrator(
                        createBus(), tuple, props.get())).
                flatMap(registrator ->
            onClose(registrator::close).then(
            register(
                    CxfJaxrsServiceRegistrator.class, () -> registrator, props).
                then(
            just(registrator)
        )));
    }

    private OSGi<CachingServiceReference<Object>>
        getApplicationExtensionsForWhiteboard() {

        return serviceReferences(_applicationExtensionsFilter.toString()).
            filter(new TargetFilter<>(_runtimeReference));
    }

    private OSGi<CachingServiceReference<Application>>
        getApplicationsForWhiteboard() {

        return
            serviceReferences(
                    Application.class, _applicationsFilter.toString()).
                filter(new TargetFilter<>(_runtimeReference));
    }

    private OSGi<CachingServiceReference<Object>> getResourcesForWhiteboard() {
        return serviceReferences(_resourcesFilter.toString()).
            filter(new TargetFilter<>(_runtimeReference));
    }

    private OSGi<ServiceRegistration<Application>>
        registerDefaultApplication() {

        return register(
            Application.class,
            () -> new DefaultApplication() {

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
            () -> {
                Map<String, Object> properties = new HashMap<>(
                    _configurationMap);
                properties.put(JAX_RS_NAME, DEFAULT_NAME);
                properties.put(JAX_RS_APPLICATION_BASE, "/");
                properties.put("service.ranking", Integer.MIN_VALUE);

                return properties;
            });
    }

    private ServiceRegistration<?> registerJaxRSServiceRuntime(
        Map<String, Object> properties) {

        properties.putIfAbsent(Constants.SERVICE_RANKING, Integer.MIN_VALUE);

        return _bundleContext.registerService(
            JaxrsServiceRuntime.class, _runtime, new Hashtable<>(properties));
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
        CachingServiceReference<CxfJaxrsServiceRegistrator>
            registratorReference) {

        String applicationName = getApplicationName(
            registratorReference::getProperty);

        Bundle originalBundle = _bundleContext.getBundle(
            (long)registratorReference.getProperty(
                "original.service.bundleid"));

        return
            service(registratorReference).flatMap(registrator ->
            changeContext(
                originalBundle.getBundleContext(),
                onlyGettables(
                    just(serviceReference),
                    _runtime::addNotGettableEndpoint,
                    _runtime::removeNotGettableEndpoint
                )
            ).recoverWith((t, e) ->
                just(serviceReference).
                effects(
                    _runtime::addErroredEndpoint,
                    _runtime::removeErroredEndpoint).
                then(nothing())
            ).flatMap(st ->
                just(st.getServiceObjects()).
                    map(
                        Utils::getResourceProvider
                ).effects(
                    rp -> _runtime.addApplicationEndpoint(
                        applicationName, st.getCachingServiceReference(),
                        registrator.getBus(), st.getService().getClass()),
                    rp -> _runtime.removeApplicationEndpoint(
                        applicationName, st.getCachingServiceReference())
                ).effects(
                    registrator::add,
                    registrator::remove
            )));
    }

    private OSGi<?> safeRegisterExtension(
        CachingServiceReference<?> serviceReference,
        CachingServiceReference<CxfJaxrsServiceRegistrator> registratorReference) {

        Bundle originalBundle = _bundleContext.getBundle(
            (long)registratorReference.getProperty(
                "original.service.bundleid"));

        return
            just(() -> getApplicationName(registratorReference::getProperty)).
                flatMap(applicationName ->
            service(registratorReference).flatMap(registrator ->
            changeContext(
                originalBundle.getBundleContext(),
                    onlyGettables(
                        just(serviceReference),
                        _runtime::addNotGettableExtension,
                        _runtime::removeNotGettableExtension
                    )
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
                t -> _runtime.addApplicationExtension(
                    applicationName, serviceReference,
                    t.getService().getClass()),
                __ -> _runtime.removeApplicationExtension(
                    applicationName, serviceReference)
            ).then(
            register(
                ApplicationExtensionRegistration.class,
                () -> new ApplicationExtensionRegistration(){},
                () -> {
                    Map<String, Object> properties = getProperties(
                        serviceReference);

                    properties.put(
                        "original.application.name", applicationName);
                    properties.put(
                        "original.objectClass",
                        serviceReference.getProperty("objectClass"));

                    return properties;
                }
            ))));
    }



    private OSGi<CachingServiceReference<Application>>
        waitForApplicationDependencies(
            OSGi<CachingServiceReference<Application>> references) {

        return references.flatMap(reference -> {
            String[] extensionDependencies = canonicalize(
                reference.getProperty(JAX_RS_EXTENSION_SELECT));

            OSGi<CachingServiceReference<Application>> program = just(
                reference);

            if (extensionDependencies.length > 0) {
                program = effects(
                    () -> _runtime.addDependentApplication(reference),
                    () -> _runtime.removeDependentApplication(reference)
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
                                    return just(reference);
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
                                    reference.getServiceReference())) {

                                    return just(reference);
                                }

                                return nothing();
                            }
                        ).effects(
                        __ -> {},
                        __ -> _runtime.addDependentApplication(
                            reference)
                    ).
                        then(program);
            }

            program = program.effects(
                __ -> _runtime.removeDependentApplication(reference),
                __ -> {}
            );

            return program;
        });
    }

    private OSGi<?> waitForExtensionDependencies(
        CachingServiceReference<?> serviceReference,
        CachingServiceReference<CxfJaxrsServiceRegistrator>
            applicationRegistratorReference,
        Consumer<CachingServiceReference<?>> onAddingDependent,
        Consumer<CachingServiceReference<?>> onRemovingDependent) {

        String applicationName = getApplicationName(
            applicationRegistratorReference::getProperty);

        String[] extensionDependencies = canonicalize(
            serviceReference.getProperty(JAX_RS_EXTENSION_SELECT));

        OSGi<CachingServiceReference<?>> program = just(serviceReference);

        if (extensionDependencies.length > 0) {
            onAddingDependent.accept(serviceReference);
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

                if (
                    extensionFilter.match(_runtimeReference) ||
                    extensionFilter.match(
                        applicationRegistratorReference.getServiceReference())) {

                    continue;
                }

                program =
                    once(serviceReferences(ApplicationExtensionRegistration.class).
                        filter(
                            sr -> sr.getProperty("original.application.name").
                                    equals(applicationName)
                        ).map(
                            CachingServiceReference::getServiceReference
                        ).filter(
                            extensionFilter::match
                        )
                    ).effects(
                        __ -> {},
                        __ -> onAddingDependent.accept(serviceReference)
                    ).
                    then(program);
            }
            catch (InvalidSyntaxException e) {

            }
        }

        program = onClose(
            () -> onRemovingDependent.accept(serviceReference)).
            then(program);

        program = program.effects(
            __ -> onRemovingDependent.accept(serviceReference),
            __ -> {}
        );

        return program;
    }

    static String getApplicationBase(PropertyHolder properties) {
        return properties.get(JAX_RS_APPLICATION_BASE).toString();
    }

    private static OSGi<CachingServiceReference<CxfJaxrsServiceRegistrator>>
        allApplicationReferences() {

        return serviceReferences(CxfJaxrsServiceRegistrator.class);
    }

    private static OSGi<CachingServiceReference<CxfJaxrsServiceRegistrator>>
        chooseApplication(
            CachingServiceReference<?> serviceReference,
            OSGi<CachingServiceReference<CxfJaxrsServiceRegistrator>>
                theDefault,
            Consumer<CachingServiceReference<?>> onWaiting,
            Consumer<CachingServiceReference<?>> onResolved) {

        Object applicationSelectProperty = serviceReference.getProperty(
            JAX_RS_APPLICATION_SELECT);

        if (applicationSelectProperty == null) {
            return theDefault;
        }

        return
            effects(
                () -> onWaiting.accept(serviceReference),
                () -> onResolved.accept(serviceReference)).then(
            serviceReferences(
                CxfJaxrsServiceRegistrator.class,
                applicationSelectProperty.toString()).
            effects(__ -> onResolved.accept(serviceReference), __ -> {}));
    }

    private static <T> OSGi<T> countChanges(
        OSGi<T> program, ChangeCounter counter) {

        return program.effects(
            __ -> counter.inc(),
            __ -> counter.inc()
        );
    }

    private static CXFNonSpringServlet createCXFServlet(Bus bus) {
        CXFNonSpringServlet cxfNonSpringServlet = new CXFNonSpringServlet();
        cxfNonSpringServlet.setBus(bus);
        return cxfNonSpringServlet;
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
                return effects(
                    () -> onInvalidAdded.accept(sr),
                    () -> onInvalidRemoved.accept(sr)).
                    then(nothing());
            }
        });
    }

    private static OSGi<ServiceRegistration<Servlet>> registerCXFServletService(
        Bus bus, Supplier<Map<String, ?>> configurationSup,
        CachingServiceReference<ServletContextHelper> contextReference) {

        Map<String, ?> configuration = configurationSup.get();

        Supplier<Map<String, ?>> propertiesSup = () -> {
            HashMap<String, Object> properties = new HashMap<>(configuration);

            properties.putIfAbsent(
                HTTP_WHITEBOARD_TARGET, "(osgi.http.endpoint=*)");

            return properties;
        };

        String address = getApplicationBase(configuration::get);

        if (!address.startsWith("/")) {
            address = "/" + address;
        }

        if (address.endsWith("/")) {
            address = address.substring(0, address.length() - 1);
        }

        String finalAddress = address;

        String applicationName = getApplicationName(configuration::get);

        Supplier<Map<String, ?>> contextPropertiesSup;

        OSGi<?> program = just(0);

        if (contextReference == null) {
            contextPropertiesSup = () -> {
                HashMap<String, Object> contextProperties = new HashMap<>();

                String contextName;

                if ("".equals(finalAddress)) {
                    contextName = HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME;
                } else {
                    contextName = "context.for" + applicationName;
                }

                contextProperties.put(
                    HTTP_WHITEBOARD_CONTEXT_NAME, contextName);
                contextProperties.put(
                    HTTP_WHITEBOARD_CONTEXT_PATH, finalAddress);

                return contextProperties;
            };

            program = register(
                ServletContextHelper.class,
                () -> new ServletContextHelper() {}, contextPropertiesSup);
        }
        else {
            contextPropertiesSup = () -> {
                HashMap<String, Object> properties = new HashMap<>();

                properties.put(
                    HTTP_WHITEBOARD_CONTEXT_NAME,
                    contextReference.getProperty(HTTP_WHITEBOARD_CONTEXT_NAME));

                return properties;
            };
        }

        Supplier<Map<String, ?>> servletPropertiesSup = () -> {
            HashMap<String, Object> servletProperties = new HashMap<>(
                propertiesSup.get());

            Map<String, ?> contextProperties = contextPropertiesSup.get();

            servletProperties.put(
                HTTP_WHITEBOARD_CONTEXT_SELECT,
                format("(%s=%s)", HTTP_WHITEBOARD_CONTEXT_NAME,
                    contextProperties.get(HTTP_WHITEBOARD_CONTEXT_NAME)));

            if (contextReference == null) {
                servletProperties.put(HTTP_WHITEBOARD_SERVLET_PATTERN, "/*");
            }
            else {
                servletProperties.put(
                    HTTP_WHITEBOARD_SERVLET_PATTERN,
                    finalAddress + "/*");
            }
            servletProperties.put(
                HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED, true);

            return servletProperties;
        };

        return program.then(
            register(
                Servlet.class, () -> createCXFServlet(bus),
                servletPropertiesSup));
    }

    private static boolean signalsValidInterface(
        CachingServiceReference<Object> serviceReference) {

        String[] objectClasses = canonicalize(serviceReference.getProperty(
            "objectClass"));

        return Arrays.stream(objectClasses).
            anyMatch(SUPPORTED_EXTENSION_INTERFACES::containsKey);
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

    private static final Filter _extensionsFilter;

    private static final Filter _resourcesFilter;

    private static Filter _applicationsFilter;

    private static Filter _applicationExtensionsFilter;

    static {
        try {
            _applicationsFilter = FrameworkUtil.createFilter(
                format(
                    "(&(objectClass=%s)(%s=*))", Application.class.getName(),
                    JAX_RS_APPLICATION_BASE));
            String extensionFilterString = format(
                "(%s=true)", JAX_RS_EXTENSION);
            _extensionsFilter = FrameworkUtil.createFilter(
                extensionFilterString);
            _applicationExtensionsFilter = FrameworkUtil.createFilter(
                format(
                    "(&(!(objectClass=%s))(%s=%s)%s)",
                    ApplicationExtensionRegistration.class.getName(),
                    JAX_RS_EXTENSION, true, extensionFilterString));
            _resourcesFilter = FrameworkUtil.createFilter(
                format("(%s=true)", JAX_RS_RESOURCE));
        }
        catch (InvalidSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static class ApplicationReferenceWithContext
        implements Comparable<ApplicationReferenceWithContext> {

        @Override
        public int compareTo(ApplicationReferenceWithContext o) {
            return _applicationReference.compareTo(o._applicationReference);
        }

        private CachingServiceReference<ServletContextHelper> _contextReference;
        private CachingServiceReference<Application> _applicationReference;

        public ApplicationReferenceWithContext(
            CachingServiceReference<ServletContextHelper> contextReference,
            CachingServiceReference<Application> applicationReference) {

            _contextReference = contextReference;
            _applicationReference = applicationReference;
        }

        public String getActualBasePath() {
            String applicationBase = getApplicationBase(
                _applicationReference::getProperty);

            if (_contextReference == null) {
                return applicationBase;
            }

            Object property = _contextReference.getProperty(
                HTTP_WHITEBOARD_CONTEXT_PATH);

            if (property == null) {
                return applicationBase;
            }

            String contextPath = property.toString();

            return contextPath + applicationBase;
        }

        public CachingServiceReference<ServletContextHelper> getContextReference() {
            return _contextReference;
        }

        public CachingServiceReference<Application> getApplicationReference() {
            return _applicationReference;
        }

    }

}
