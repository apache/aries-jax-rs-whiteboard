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

import org.apache.aries.osgi.functional.Event;
import org.apache.aries.osgi.functional.OSGi;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.message.Message;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jaxrs.runtime.dto.FailedApplicationDTO;

import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;

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

    public static OSGi<?> cxfRegistrator(
        Bus bus, Application application, Map<String, Object> props) {

        try {
            CXFJaxRsServiceRegistrator registrator =
                new CXFJaxRsServiceRegistrator(bus, application);

            return
                onClose(registrator::close).then(
                register(CXFJaxRsServiceRegistrator.class, registrator, props)
            );
        }
        catch (Exception e) {
            return register(
                FailedApplicationDTO.class, new FailedApplicationDTO(), props);
        }
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

    public static <T extends Comparable<? super T>> OSGi<T> repeatInOrder(
        OSGi<T> program) {

        return program.route(new RepeatInOrderRouter<>());
    }

    public static <T> OSGi<ResourceProvider>
        registerEndpoint(
            ServiceReference<?> serviceReference,
            CXFJaxRsServiceRegistrator registrator,
            ServiceObjects<T> serviceObjects) {

        ResourceProvider resourceProvider = getResourceProvider(serviceObjects);
        registrator.add(resourceProvider);
        return just(resourceProvider);
    }

    public static String safeToString(Object object) {
        return object == null ? "" : object.toString();
    }

    public static <T> ResourceProvider getResourceProvider(
        ServiceObjects<T> serviceObjects) {

        ServiceReference<T> serviceReference = serviceObjects.getServiceReference();

        return new ComparableResourceProvider(serviceReference, serviceObjects);
    }

    public static void unregisterEndpoint(
        CXFJaxRsServiceRegistrator registrator,
        ResourceProvider resourceProvider) {

        registrator.remove(resourceProvider);
    }

    private static class RepeatInOrderRouter<T extends Comparable<? super T>>
        implements Consumer<OSGi.Router<T>> {

        private final TreeSet<Event<T>> _treeSet;

        public RepeatInOrderRouter() {
            Comparator<Event<T>> comparing = Comparator.comparing(
                Event::getContent);

            _treeSet = new TreeSet<>(comparing.reversed());
        }

        @Override
        public void accept(OSGi.Router<T> router) {
            router.onIncoming(ev -> {
                _treeSet.add(ev);

                SortedSet<Event<T>> events = _treeSet.tailSet(ev, false);
                events.forEach(router::signalLeave);

                router.signalAdd(ev);

                events.forEach(router::signalAdd);
            });
            router.onLeaving(ev -> {
                _treeSet.remove(ev);

                SortedSet<Event<T>> events = _treeSet.tailSet(ev, false);
                events.forEach(router::signalLeave);

                router.signalLeave(ev);

                events.forEach(router::signalAdd);
            });
            router.onClose(() -> {
                _treeSet.forEach(router::signalLeave);

                _treeSet.clear();
            });
        }

    }

    public static class ComparableResourceProvider
        implements ResourceProvider, Comparable<ComparableResourceProvider> {

        private ServiceReference<?> _serviceReference;
        private final ServiceObjects<?> _serviceObjects;

        public ComparableResourceProvider(
            ServiceReference<?> serviceReference,
            ServiceObjects<?> serviceObjects) {
            _serviceReference = serviceReference;

            _serviceObjects = serviceObjects;
        }

        @Override
        public int compareTo(ComparableResourceProvider o) {
            return _serviceReference.compareTo(o._serviceReference);
        }

        @Override
        public Object getInstance(Message m) {
            return _serviceObjects.getService();
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public void releaseInstance(Message m, Object o) {
            ((ServiceObjects)_serviceObjects).ungetService(o);
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public Class<?> getResourceClass() {
            Object service = _serviceObjects.getService();

            try {
                return service.getClass();
            }
            finally {
                ((ServiceObjects)_serviceObjects).ungetService(service);
            }
        }

        @Override
        public boolean isSingleton() {
            return false;
        }

    }

}
