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

package org.apache.aries.jax.rs.whiteboard.activator;

import javax.servlet.Servlet;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.aries.jax.rs.whiteboard.internal.CXFJaxRsServiceRegistrator;
import org.apache.aries.jax.rs.whiteboard.internal.CXFJaxRsServiceRegistrator.ServiceInformation;
import org.apache.aries.osgi.functional.OSGi;
import org.apache.aries.osgi.functional.OSGiResult;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.CXFBusFactory;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import static java.lang.String.format;
import static org.apache.aries.jax.rs.whiteboard.AriesJaxRSWhiteboardConstants.*;
import static org.apache.aries.osgi.functional.OSGi.bundleContext;
import static org.apache.aries.osgi.functional.OSGi.just;
import static org.apache.aries.osgi.functional.OSGi.onClose;
import static org.apache.aries.osgi.functional.OSGi.register;
import static org.apache.aries.osgi.functional.OSGi.serviceReferences;
import static org.apache.aries.osgi.functional.OSGi.services;
import static org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants.*;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN;

public class CXFJaxRsBundleActivator implements BundleActivator {

    private BundleContext _bundleContext;
    private OSGiResult<?> _applicationsResult;
    private OSGiResult<?> _singletonsResult;
    private OSGiResult<?> _extensionsResult;
    private OSGiResult<?> _applicationSingletonsResult;

    private static <T> OSGi<T> service(ServiceReference<T> serviceReference) {
        return
            bundleContext().flatMap(bundleContext ->
            onClose(() -> bundleContext.ungetService(serviceReference)).then(
            just(bundleContext.getService(serviceReference))
        ));
    }

    private static <T> OSGi<ServiceObjects<T>> serviceObjects(
        ServiceReference<T> serviceReference) {

        return
            bundleContext().flatMap(bundleContext ->
            just(bundleContext.getServiceObjects(serviceReference))
        );
    }

    private static OSGi<CXFJaxRsServiceRegistrator> cxfRegistrator(
        Bus bus, Application application, Map<String, Object> props) {

        CXFJaxRsServiceRegistrator registrator =
            new CXFJaxRsServiceRegistrator(bus, application, props);

        return
            onClose(registrator::close).then(
            register(CXFJaxRsServiceRegistrator.class, registrator, props).then(
            just(registrator)
        ));
    }

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        _bundleContext = bundleContext;
        initRuntimeDelegate(bundleContext);

        // TODO make the context path of the JAX-RS Whiteboard configurable.
        Bus bus = BusFactory.newInstance(
            CXFBusFactory.class.getName()).createBus();
        registerCXFServletService(bus);

        OSGi<?> applications =
            serviceReferences(Application.class, getApplicationFilter()).
                flatMap(ref ->
            just(
                CXFJaxRsServiceRegistrator.getProperties(
                    ref, JAX_RS_APPLICATION_BASE)).
                flatMap(properties ->
            service(ref).flatMap(application ->
            cxfRegistrator(bus, application, properties)
        )));

        _applicationsResult = applications.run(bundleContext);

        Application defaultApplication = new Application() {};

        CXFJaxRsServiceRegistrator defaultServiceRegistrator =
            new CXFJaxRsServiceRegistrator(
                bus, defaultApplication, new HashMap<>());

        OSGi<?> singletons =
            serviceReferences(getSingletonsFilter()).
                flatMap(serviceReference ->
            waitForExtensionDependencies(serviceReference,
                just(
                    CXFJaxRsServiceRegistrator.getProperties(
                        serviceReference, JAX_RS_RESOURCE_BASE)).
                    flatMap(properties ->
                service(serviceReference).flatMap(service ->
                safeRegisterEndpoint(
                    serviceReference, defaultServiceRegistrator)
            )))
        );

        _singletonsResult = singletons.run(bundleContext);

        OSGi<?> extensions =
            serviceReferences(getExtensionFilter()).flatMap(ref ->
            waitForExtensionDependencies(ref,
                safeRegisterExtension(ref, defaultServiceRegistrator)
            )
        );

        _extensionsResult = extensions.run(bundleContext);

        OSGi<?> applicationSingletons =
            serviceReferences(format("(%s=*)", JAX_RS_APPLICATION_SELECT)).
                flatMap(ref ->
            just(ref.getProperty(JAX_RS_APPLICATION_SELECT).toString()).
                flatMap(applicationFilter ->
            services(CXFJaxRsServiceRegistrator.class, applicationFilter).
                flatMap(registrator ->
            testProvider(ref).flatMap(isProvider -> {
                if (isProvider) {
                    return safeRegisterExtension(ref, registrator);
                }
                else {
                    return safeRegisterEndpoint(ref, registrator);
                }
            })
        )));

        _applicationSingletonsResult = applicationSingletons.run(bundleContext);
    }

    private OSGi<Boolean> testProvider(ServiceReference<?> serviceReference) {
        return bundleContext().flatMap(bundleContext -> {
            Object service = bundleContext.getService(serviceReference);
            Class<?> serviceClass = service.getClass();
            if (serviceClass.isAnnotationPresent(Provider.class)) {
                return just(Boolean.TRUE);
            }
            else {
                return just(Boolean.FALSE);
            }
        });
    }

    private OSGi<?> safeRegisterExtension(
        ServiceReference<Object> ref,
        CXFJaxRsServiceRegistrator registrator) {

        return
            service(ref).flatMap(extension ->
            onClose(() -> registrator.removeProvider(extension)).
                foreach(ign ->
            registrator.addProvider(extension)
        ));
    }

    /**
     * Initialize instance so it is never looked up again
     * @param bundleContext
     */
    private void initRuntimeDelegate(BundleContext bundleContext) {
        Thread thread = Thread.currentThread();
        ClassLoader oldClassLoader = thread.getContextClassLoader();
        BundleWiring bundleWiring = bundleContext.getBundle().adapt(
            BundleWiring.class);
        thread.setContextClassLoader(bundleWiring.getClassLoader());
        try {
            RuntimeDelegate.getInstance();
        }
        finally {
            thread.setContextClassLoader(oldClassLoader);
        }
    }

    private String[] canonicalize(Object propertyValue) {
        if (propertyValue == null) {
            return new String[0];
        }
        if (propertyValue instanceof String[]) {
            return (String[]) propertyValue;
        }
        return new String[]{propertyValue.toString()};
    }

    private String buildExtensionFilter(String filter) {
        return format("(&%s%s)", getExtensionFilter(), filter);
    }

    private OSGi<?> waitForExtensionDependencies(
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

    private <T> OSGi<?> safeRegisterEndpoint(
        ServiceReference<T> ref, CXFJaxRsServiceRegistrator registrator) {

        return
            bundleContext().flatMap(bundleContext ->
            serviceObjects(ref).flatMap(service ->
            registerEndpoint(ref, registrator, service).
                flatMap(serviceInformation ->
            onClose(
                () -> unregisterEndpoint(registrator, serviceInformation)))));
    }

    private <T> OSGi<ServiceInformation> registerEndpoint(
        ServiceReference<?> ref,
        CXFJaxRsServiceRegistrator registrator,
        ServiceObjects<T> serviceObjects) {

        Thread thread = Thread.currentThread();
        ClassLoader contextClassLoader = thread.getContextClassLoader();
        ClassLoader classLoader = ref.getBundle().adapt(BundleWiring.class).
            getClassLoader();
        Object resourceBaseObject = ref.getProperty(JAX_RS_RESOURCE_BASE);

        ResourceProvider resourceProvider = getResourceProvider(serviceObjects);

        String resourceBase;

        if (resourceBaseObject == null) {
            resourceBase = "";
        }
        else {
            resourceBase = resourceBaseObject.toString();
        }
        try {
            thread.setContextClassLoader(classLoader);
            ServiceInformation serviceInformation = new ServiceInformation(
                resourceBase, resourceProvider);
            registrator.add(serviceInformation);
            return just(serviceInformation);
        }
        finally {
            thread.setContextClassLoader(contextClassLoader);
        }
    }

    private <T> ResourceProvider getResourceProvider(
        ServiceObjects<T> serviceObjects) {

        ResourceProvider resourceProvider;
        T service = serviceObjects.getService();
        Class<?> serviceClass = service.getClass();

        resourceProvider = new ResourceProvider() {

            @Override
            public Object getInstance(Message m) {
                return serviceObjects.getService();
            }

            @Override
            public void releaseInstance(Message m, Object o) {
                serviceObjects.ungetService((T)o);
            }

            @Override
            public Class<?> getResourceClass() {
                return serviceClass;
            }

            @Override
            public boolean isSingleton() {
                return false;
            }
        };

        serviceObjects.ungetService(service);
        return resourceProvider;
    }

    private void unregisterEndpoint(
        CXFJaxRsServiceRegistrator registrator,
        ServiceInformation serviceInformation) {

        registrator.remove(serviceInformation);
    }

    private ServiceRegistration<Servlet> registerCXFServletService(Bus bus) {
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(HTTP_WHITEBOARD_CONTEXT_SELECT,
            format("(%s=%s)", HTTP_WHITEBOARD_CONTEXT_NAME,
                HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME));
        properties.put(HTTP_WHITEBOARD_SERVLET_PATTERN, "/*");
        properties.put(Constants.SERVICE_RANKING, -1);
        CXFNonSpringServlet cxfNonSpringServlet = createCXFServlet(bus);
        return _bundleContext.registerService(
            Servlet.class, cxfNonSpringServlet, properties);
    }

    private CXFNonSpringServlet createCXFServlet(Bus bus) {
        CXFNonSpringServlet cxfNonSpringServlet = new CXFNonSpringServlet();
        cxfNonSpringServlet.setBus(bus);
        return cxfNonSpringServlet;
    }

    private String getExtensionFilter() {
        return format("(%s=*)", JAX_RS_EXTENSION_NAME);
    }

    private String getApplicationFilter() {
        return format("(%s=*)", JAX_RS_APPLICATION_BASE);
    }

    private String getSingletonsFilter() {
        return format("(%s=*)", JAX_RS_RESOURCE_BASE);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        _applicationSingletonsResult.close();
        _applicationsResult.close();
        _extensionsResult.close();
        _singletonsResult.close();
    }

}
