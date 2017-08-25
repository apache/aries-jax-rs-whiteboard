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
import org.apache.aries.osgi.functional.OSGi;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.http.runtime.HttpServiceRuntime;
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
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.lang.String.format;
import static org.apache.aries.jax.rs.whiteboard.internal.AriesJaxRSServiceRuntime.getApplicationName;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.canonicalize;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.generateApplicationName;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.getProperties;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.getResourceProvider;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.highestPer;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.ignore;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.onlyGettables;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.service;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.serviceObjects;
import static org.apache.aries.osgi.functional.OSGi.all;
import static org.apache.aries.osgi.functional.OSGi.bundleContext;
import static org.apache.aries.osgi.functional.OSGi.just;
import static org.apache.aries.osgi.functional.OSGi.nothing;
import static org.apache.aries.osgi.functional.OSGi.onClose;
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

    public static final Function<ServiceTuple<Application>, String>
        APPLICATION_BASE =
        ((Function<ServiceTuple<Application>, ServiceReference<Application>>)
            ServiceTuple::getServiceReference).andThen(
                sr -> getApplicationBase(sr::getProperty));

    public static final Collection<String> SUPPORTED_EXTENSION_INTERFACES = new HashSet<>(
        Arrays.asList(
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
        ));

    public static final String DEFAULT_NAME = ".default";

    public static OSGi<ApplicationReference> allApplicationReferences() {
        return
            serviceReferences(CXFJaxRsServiceRegistrator.class).
                flatMap(registratorReference ->
            just(getApplicationName(registratorReference::getProperty)).
                flatMap(applicationName ->
            service(registratorReference).flatMap(registrator ->
            just(new ApplicationReference(applicationName, registrator))
        )));
    }

    public static OSGi<?> createWhiteboard(
        Dictionary<String, ?> configuration) {

        AriesJaxRSServiceRuntime runtime = new AriesJaxRSServiceRuntime();

        Map<String, ?> configurationMap = Maps.from(configuration);

        return
            bundleContext().flatMap(bundleContext ->
            registerJaxRSServiceRuntime(
                    runtime, bundleContext, configurationMap).
                flatMap(runtimeRegistration ->
            createDefaultJaxRsServiceRegistrator(configurationMap, runtime).
                flatMap(defaultApplicationReference ->
            just(new ServiceRegistrationChangeCounter(runtimeRegistration)).
                flatMap(counter ->
            just(runtimeRegistration.getReference()).flatMap(runtimeReference ->
                all(
                    ignore(
                        whiteboardApplications(
                            runtimeReference, runtime, configurationMap,
                            counter)),
                    ignore(
                        whiteBoardApplicationResources(
                            bundleContext, runtimeReference,
                            defaultApplicationReference, runtime, counter)),
                    ignore(
                        whiteBoardApplicationExtensions(
                            bundleContext, runtimeReference,
                            defaultApplicationReference, runtime, counter)
            )))))));
    }

    public static OSGi<?> deployRegistrator(
        Bus bus, ServiceTuple<Application> tuple, Map<String, Object> props,
        AriesJaxRSServiceRuntime runtime) {

        try {
            CXFJaxRsServiceRegistrator registrator =
                new CXFJaxRsServiceRegistrator(bus, tuple.getService());

            return
                onClose(registrator::close).then(
                register(CXFJaxRsServiceRegistrator.class, registrator, props)
            );
        }
        catch (Exception e) {
            ServiceReference<Application> serviceReference =
                tuple.getServiceReference();

            runtime.addErroredApplication(serviceReference);

            return onClose(
                () -> runtime.removeErroredApplication(serviceReference)
            ).then(
                nothing()
            );
        }
    }

    public static String getApplicationBase(PropertyHolder properties) {
        return properties.get(JAX_RS_APPLICATION_BASE).toString();
    }

    public static <T> OSGi<ResourceProvider>
        registerEndpoint(
            CXFJaxRsServiceRegistrator registrator,
            ServiceObjects<T> serviceObjects) {

        ResourceProvider resourceProvider = getResourceProvider(serviceObjects);
        registrator.add(resourceProvider);
        return just(resourceProvider);
    }

    public static <T> OSGi<?> safeRegisterEndpoint(
        ServiceReference<T> serviceReference,
        String applicationName,
        CXFJaxRsServiceRegistrator registrator,
        AriesJaxRSServiceRuntime runtime) {

        return
            onlyGettables(
                just(serviceReference),
                runtime::addNotGettableEndpoint,
                runtime::removeNotGettableEndpoint
            ).flatMap(
                tuple -> serviceObjects(serviceReference).flatMap(
                    serviceObjects -> registerEndpoint(
                        registrator, serviceObjects).flatMap(
                            resourceProvider ->
                                onClose(
                                    () -> Utils.unregisterEndpoint(
                                        registrator, resourceProvider)
                                )
                    )
                )
            ).foreach(
                __ -> runtime.addApplicationEndpoint(
                    applicationName, serviceReference),
                __ -> runtime.removeApplicationEndpoint(
                    applicationName, serviceReference)
            );
    }

    public static OSGi<?> safeRegisterExtension(
        ServiceReference<?> serviceReference, String applicationName,
        CXFJaxRsServiceRegistrator registrator,
        AriesJaxRSServiceRuntime runtime) {

        Map<String, Object> properties = getProperties(serviceReference);

        properties.put(
            JAX_RS_NAME, applicationName);
        properties.put(
            "original.objectClass",
            serviceReference.getProperty("objectClass"));

        return
            onlyGettables(
                just(serviceReference),
                runtime::addNotGettableExtension,
                runtime::removeNotGettableExtension
            ).foreach(
                registrator::addProvider,
                registrator::removeProvider
            ).foreach(
                __ -> runtime.addApplicationExtension(
                    applicationName, serviceReference),
                __ -> runtime.removeApplicationExtension(
                    applicationName, serviceReference)
            ).then(
                register(
                    ApplicationExtensionRegistration.class,
                    new ApplicationExtensionRegistration(){}, properties)
            );
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

    private static OSGi<ApplicationReference> chooseApplication(
        ServiceReference<?> serviceReference, OSGi<ApplicationReference> theDefault) {

        Object applicationSelectProperty = serviceReference.getProperty(
            JAX_RS_APPLICATION_SELECT);

        if (applicationSelectProperty == null) {
            return theDefault;
        }

        String applicationName = getApplicationName(
            serviceReference::getProperty);

        return
            serviceReferences(
                    CXFJaxRsServiceRegistrator.class,
                    applicationSelectProperty.toString()).
                flatMap(registratorReference ->
            service(registratorReference).flatMap(registrator ->
            just(new ApplicationReference(applicationName, registrator))
        ));
    }

    private static <T> OSGi<T> countChanges(
        OSGi<T> program, ChangeCounter counter) {

        return program.map(t -> {counter.inc(); return t;});
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

    private static CXFNonSpringServlet createCXFServlet(Bus bus) {
        CXFNonSpringServlet cxfNonSpringServlet = new CXFNonSpringServlet();
        cxfNonSpringServlet.setBus(bus);
        return cxfNonSpringServlet;
    }

    private static OSGi<ApplicationReference>
        createDefaultJaxRsServiceRegistrator(
            Map<String, ?> configuration, AriesJaxRSServiceRuntime runtime) {

        Map<String, Object> properties = new HashMap<>(configuration);
        properties.put(JAX_RS_NAME, DEFAULT_NAME);
        properties.put(JAX_RS_APPLICATION_BASE, "/");
        properties.put("service.id", (long)-1);

        return
            bundleContext().flatMap(bundleContext ->
            just(createBus(bundleContext, configuration)).flatMap(bus ->
            registerCXFServletService(bus, "", configuration).foreach(
                __ -> runtime.setDefaultApplication(properties),
                __ -> runtime.clearDefaultApplication()).then(
            just(new CXFJaxRsServiceRegistrator(bus, new DefaultApplication())).
                flatMap(registrator ->
            just(new ApplicationReference(DEFAULT_NAME, registrator))
        ))));
    }

    private static OSGi<ServiceTuple<Application>> deployApplication(
        Map<String, ?> configuration, BundleContext bundleContext,
        ServiceTuple<Application> tuple, AriesJaxRSServiceRuntime runtime) {

        ExtensionManagerBus bus = createBus(bundleContext, configuration);

        ServiceReference<Application> serviceReference =
            tuple.getServiceReference();

        Map<String, Object> properties = getProperties(serviceReference);

        properties.computeIfAbsent(
            JAX_RS_NAME, (__) -> generateApplicationName(
                serviceReference::getProperty));

        return
            deployRegistrator(bus, tuple, properties, runtime).then(
            registerCXFServletService(
                bus, getApplicationBase(properties::get), properties)).then(
            just(tuple)
        );
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

    private static OSGi<ServiceReference<Application>>
        getApplicationsForWhiteboard(
            ServiceReference<?> jaxRsRuntimeServiceReference) {

        return
            serviceReferences(Application.class, getApplicationFilter()).
            filter(new TargetFilter<>(jaxRsRuntimeServiceReference));
    }

    private static String getExtensionsFilter() {
        return format("(%s=true)", JAX_RS_EXTENSION);
    }

    private static String getResourcesFilter() {
        return format("(%s=true)", JAX_RS_RESOURCE);
    }

    private static OSGi<ServiceReference<Object>> onlySupportedInterfaces(
        OSGi<ServiceReference<Object>> program,
        Consumer<ServiceReference<?>> onInvalidAdded,
        Consumer<ServiceReference<?>> onInvalidRemoved) {

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

        properties.putIfAbsent(HTTP_WHITEBOARD_SERVLET_PATTERN, address + "/*");

        CXFNonSpringServlet cxfNonSpringServlet = createCXFServlet(bus);

        return register(Servlet.class, cxfNonSpringServlet, properties);
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

    private static boolean signalsValidInterface(
        ServiceReference<Object> serviceReference) {

        String[] objectClasses = canonicalize(serviceReference.getProperty(
            "objectClass"));

        return Arrays.stream(objectClasses).
            anyMatch(SUPPORTED_EXTENSION_INTERFACES::contains);
    }

    private static OSGi<?> waitForExtensionDependencies(
        BundleContext bundleContext,
        ServiceReference<?> serviceReference,
        ApplicationReference applicationReference,
        AriesJaxRSServiceRuntime runtime, OSGi<?> program) {

        String[] extensionDependencies = Utils.canonicalize(
            serviceReference.getProperty(JAX_RS_EXTENSION_SELECT));

        if (extensionDependencies.length > 0) {
            runtime.addDependentService(serviceReference);
        }

        for (String extensionDependency : extensionDependencies) {
            try {
                extensionDependency = extensionDependency.replace(
                    "(objectClass=", "(original.objectClass=");

                Filter extensionFilter = bundleContext.createFilter(
                    extensionDependency);

                program =
                    serviceReferences(ApplicationExtensionRegistration.class).
                        filter(
                            sr -> getApplicationName(sr::getProperty).equals(
                                applicationReference.getApplicationName())
                        ).
                        filter(
                            extensionFilter::match
                        ).foreach(
                            __ -> {},
                            __ -> runtime.addDependentService(serviceReference)
                        ).
                        then(program);
            }
            catch (InvalidSyntaxException e) {

            }
        }

        program = program.foreach(
            __ -> runtime.removeDependentService(serviceReference)
        );

        return program;
    }

    private static OSGi<?> whiteBoardApplicationExtensions(
        BundleContext bundleContext,
        ServiceReference<?> jaxRsRuntimeServiceReference,
        ApplicationReference defaultApplicationReference,
        AriesJaxRSServiceRuntime runtime, ServiceRegistrationChangeCounter counter) {
        return
            onlySupportedInterfaces(
                countChanges(serviceReferences(getApplicationExtensionsFilter()).
                    filter(new TargetFilter<>(jaxRsRuntimeServiceReference)),
                    counter),
                runtime::addInvalidExtension, runtime::removeInvalidExtension).
                    flatMap(endpointReference ->
            chooseApplication(
                    endpointReference,
                    all(
                        just(defaultApplicationReference),
                        allApplicationReferences())).
                flatMap(applicationReference ->
            waitForExtensionDependencies(
                bundleContext, endpointReference, applicationReference, runtime,
            safeRegisterExtension(
                endpointReference,
                applicationReference.getApplicationName(),
                applicationReference.getRegistrator(), runtime)
        )));
    }

    private static OSGi<?> whiteBoardApplicationResources(
        BundleContext bundleContext,
        ServiceReference<?> jaxRsRuntimeServiceReference,
        ApplicationReference defaultApplicationReference,
        AriesJaxRSServiceRuntime runtime, ServiceRegistrationChangeCounter counter) {
        return
            countChanges(
                    serviceReferences(getResourcesFilter()).
                        filter(
                            new TargetFilter<>(jaxRsRuntimeServiceReference)),
                    counter).
                flatMap(resourceReference ->
            chooseApplication(
                resourceReference, just(defaultApplicationReference)).
                flatMap(applicationReference ->
            waitForExtensionDependencies(
                bundleContext, resourceReference, applicationReference,
                runtime,
                safeRegisterEndpoint(
                    resourceReference,
                    applicationReference.getApplicationName(),
                    applicationReference.getRegistrator(), runtime)
        )));
    }

    private static OSGi<?> whiteboardApplications(
        ServiceReference<?> jaxRsRuntimeServiceReference,
        AriesJaxRSServiceRuntime runtime,
        Map<String, ?> configuration, ServiceRegistrationChangeCounter counter) {

        OSGi<ServiceTuple<Application>> gettableAplicationForWhiteboard =
            onlyGettables(
                countChanges(
                    getApplicationsForWhiteboard(jaxRsRuntimeServiceReference),
                    counter),
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
                    tuple -> deployApplication(
                        configuration, bundleContext, tuple, runtime)
                ).map(
                    ServiceTuple::getServiceReference
                ).map(
                    Utils::getProperties
                ).foreach(
                    p -> runtime.setApplicationForPath(
                        getApplicationBase(p::get), p),
                    p -> runtime.unsetApplicationForPath(
                        getApplicationBase(p::get))
                )
            );
    }

    private static interface ChangeCounter {

        public void inc();

    }

    private static class ServiceRegistrationChangeCounter
        implements ChangeCounter{

        private static final String changecount = "service.changecount";
        private final AtomicLong _atomicLong = new AtomicLong();
        private final Hashtable<String, Object> _properties;
        private ServiceRegistration<?> _serviceRegistration;

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

    private static class ApplicationReference {
        private final String _applicationName;
        private final CXFJaxRsServiceRegistrator _registrator;

        public ApplicationReference(
            String applicationName,
            CXFJaxRsServiceRegistrator registrator) {

            _applicationName = applicationName;
            _registrator = registrator;
        }

        public String getApplicationName() {
            return _applicationName;
        }

        public CXFJaxRsServiceRegistrator getRegistrator() {
            return _registrator;
        }

    }

}
