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

import org.apache.aries.jax.rs.whiteboard.internal.CXFJaxRsServiceRegistrator.ResourceInformation;
import org.apache.aries.osgi.functional.OSGi;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.message.Message;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleWiring;

import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;
import java.util.Map;

import static org.apache.aries.osgi.functional.OSGi.bundleContext;
import static org.apache.aries.osgi.functional.OSGi.just;
import static org.apache.aries.osgi.functional.OSGi.onClose;
import static org.apache.aries.osgi.functional.OSGi.register;

/**
 * @author Carlos Sierra Andrés
 */
public class Utils {

    public static <T> OSGi<T> service(ServiceReference<T> serviceReference) {
        return
            bundleContext().flatMap(bundleContext ->
            onClose(() -> bundleContext.ungetService(serviceReference)).then(
            just(bundleContext.getService(serviceReference))
        ));
    }

    public static <T> OSGi<ServiceObjects<T>> serviceObjects(
        ServiceReference<T> serviceReference) {

        return
            bundleContext().flatMap(bundleContext ->
            just(bundleContext.getServiceObjects(serviceReference))
        );
    }

    public static OSGi<CXFJaxRsServiceRegistrator> cxfRegistrator(
        Bus bus, Application application, Map<String, Object> props) {

        CXFJaxRsServiceRegistrator registrator =
            new CXFJaxRsServiceRegistrator(bus, application, props);

        return
            onClose(registrator::close).then(
            register(CXFJaxRsServiceRegistrator.class, registrator, props).then(
            just(registrator)
        ));
    }

    public static OSGi<?> safeRegisterGeneric(
        ServiceReference<?> serviceReference,
        CXFJaxRsServiceRegistrator registrator) {

        return bundleContext().flatMap(bundleContext -> {
            Object service = bundleContext.getService(serviceReference);
            Class<?> serviceClass = service.getClass();
            bundleContext.ungetService(serviceReference);
            if (serviceClass.isAnnotationPresent(Provider.class)) {
                return safeRegisterExtension(serviceReference, registrator);
            }
            else {
                return safeRegisterEndpoint(serviceReference, registrator);
            }
        });
    }

    public static OSGi<?> safeRegisterExtension(
        ServiceReference<?> serviceReference,
        CXFJaxRsServiceRegistrator registrator) {

        return
            service(serviceReference).flatMap(extension ->
            onClose(() -> registrator.removeProvider(extension)).
                foreach(ign ->
            registrator.addProvider(extension)
        ));
    }

    public static <T> OSGi<?> safeRegisterEndpoint(
        ServiceReference<T> ref, CXFJaxRsServiceRegistrator registrator) {

        return
            bundleContext().flatMap(bundleContext ->
            serviceObjects(ref).flatMap(service ->
            registerEndpoint(ref, registrator, service).
                flatMap(serviceInformation ->
            onClose(() ->
                unregisterEndpoint(registrator, serviceInformation)))));
    }

    public static <T> OSGi<ResourceInformation> registerEndpoint(
        ServiceReference<?> serviceReference,
        CXFJaxRsServiceRegistrator registrator,
        ServiceObjects<T> serviceObjects) {

        Thread thread = Thread.currentThread();
        ClassLoader contextClassLoader = thread.getContextClassLoader();
        Bundle bundle = serviceReference.getBundle();
        BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
        ClassLoader classLoader = bundleWiring.getClassLoader();
        ResourceProvider resourceProvider = getResourceProvider(serviceObjects);
        try {
            thread.setContextClassLoader(classLoader);
            ResourceInformation<ServiceReference<?>> resourceInformation =
                new ResourceInformation<>(serviceReference, resourceProvider);
            registrator.add(resourceInformation);
            return just(resourceInformation);
        }
        finally {
            thread.setContextClassLoader(contextClassLoader);
        }
    }

    public static String safeToString(Object object) {
        return object == null ? "" : object.toString();
    }

    public static <T> ResourceProvider getResourceProvider(
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

    public static void unregisterEndpoint(
        CXFJaxRsServiceRegistrator registrator,
        ResourceInformation resourceInformation) {

        registrator.remove(resourceInformation);
    }

}
