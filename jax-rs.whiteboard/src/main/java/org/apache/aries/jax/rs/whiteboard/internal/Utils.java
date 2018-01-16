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

import org.apache.aries.osgi.functional.CachingServiceReference;
import org.apache.aries.osgi.functional.OSGi;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.message.Message;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import java.util.Collection;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.apache.aries.osgi.functional.OSGi.bundleContext;
import static org.apache.aries.osgi.functional.OSGi.effects;
import static org.apache.aries.osgi.functional.OSGi.just;
import static org.apache.aries.osgi.functional.OSGi.nothing;
import static org.apache.aries.osgi.functional.OSGi.onClose;
import static org.apache.aries.osgi.functional.Utils.highest;

/**
 * @author Carlos Sierra Andrés
 */
public class Utils {

    public static String[] canonicalize(Object propertyValue) {
        if (propertyValue == null) {
            return new String[0];
        }
        if (propertyValue instanceof String[]) {
            return (String[]) propertyValue;
        }
        if (propertyValue instanceof Collection) {
            return
                ((Collection<?>)propertyValue).stream().
                    map(
                        Object::toString
                    ).toArray(
                        String[]::new
                    );
        }

        return new String[]{propertyValue.toString()};
    }

    public static String generateApplicationName(
        PropertyHolder propertyHolder) {

        return ".generated.for." + propertyHolder.get("service.id");
    }

    public static Map<String, Object> getProperties(CachingServiceReference<?> sref) {
        String[] propertyKeys = sref.getPropertyKeys();
        Map<String, Object> properties = new HashMap<>(propertyKeys.length);

        for (String key : propertyKeys) {
            properties.put(key, sref.getProperty(key));
        }

        return properties;
    }

    public static int getRanking(
        CachingServiceReference<?> cachingServiceReference) {

        Object property = cachingServiceReference.getProperty(
            "service.ranking");

        if (property == null) {
            return 0;
        }

        if (property instanceof Number) {
            return ((Number)property).intValue();
        }
        else {
            return 0;
        }
    }

    public static <T> ResourceProvider getResourceProvider(
        ServiceObjects<T> serviceObjects) {

        CachingServiceReference<T> serviceReference =
            new CachingServiceReference<>(
                serviceObjects.getServiceReference());

        return new ServiceReferenceResourceProvider(
            serviceReference, serviceObjects);
    }

    public static <K, T extends Comparable<? super T>> OSGi<T> highestPer(
        Function<T, K> keySupplier, OSGi<T> program,
        Consumer<? super T> onAddingShadowed,
        Consumer<? super T> onRemovedShadowed) {

        return program.splitBy(
            keySupplier,
            p -> highest(
                p, Comparator.naturalOrder(),
                discards -> discards.
                    effects(onAddingShadowed, onRemovedShadowed).
                    then(nothing())
            )
        );
    }

    public static <T> OSGi<ServiceTuple<T>> onlyGettables(
        OSGi<CachingServiceReference<T>> program,
        Consumer<CachingServiceReference<T>> whenAddedNotGettable,
        Consumer<CachingServiceReference<T>> whenLeavingNotGettable) {

        return bundleContext().flatMap(bundleContext ->
            program.recoverWith(
                (serviceReference, e) ->
                    notGettableResult(
                        whenAddedNotGettable, whenLeavingNotGettable,
                        serviceReference)
            ).flatMap(serviceReference -> {
                ServiceObjects<T> serviceObjects =
                    bundleContext.getServiceObjects(
                        serviceReference.getServiceReference());
                T service = serviceObjects.getService();

                if (service == null) {
                    return notGettableResult(
                        whenAddedNotGettable, whenLeavingNotGettable,
                        serviceReference);
                }

                return
                    onClose(
                        () -> serviceObjects.ungetService(service)
                    ).then(
                        just(new ServiceTuple<>(
                            serviceReference, serviceObjects, service))
                    );
            }));
    }

    private static <T, S> OSGi<S> notGettableResult(
        Consumer<CachingServiceReference<T>> whenAddedNotGettable,
        Consumer<CachingServiceReference<T>> whenLeavingNotGettable,
        CachingServiceReference<T> immutable) {

        return effects(
            () -> whenAddedNotGettable.accept(immutable),
            () -> whenLeavingNotGettable.accept(immutable)
        ).then(
            nothing()
        );
    }

    public static <T> OSGi<T> service(
        CachingServiceReference<T> immutableServiceReference) {

        ServiceReference<T> serviceReference =
            immutableServiceReference.getServiceReference();

        return
            bundleContext().flatMap(bundleContext ->
            onClose(() -> bundleContext.ungetService(serviceReference)).then(
            just(bundleContext.getService(serviceReference))
        ));
    }

    public static <T> OSGi<ServiceObjects<T>> serviceObjects(
        CachingServiceReference<T> immutableServiceReference) {

        return
            bundleContext().flatMap(bundleContext ->
            just(bundleContext.getServiceObjects(
                immutableServiceReference.getServiceReference()))
        );
    }

    public static void updateProperty(
        ServiceRegistration<?> serviceRegistration, String key, Object value) {

        CachingServiceReference<?> serviceReference =
            new CachingServiceReference<>(serviceRegistration.getReference());

        Dictionary<String, Object> properties = new Hashtable<>();

        for (String propertyKey : serviceReference.getPropertyKeys()) {
            properties.put(
                propertyKey, serviceReference.getProperty(propertyKey));
        }

        properties.put(key, value);

        serviceRegistration.setProperties(properties);
    }

    public static OSGi<Void> ignoreResult(OSGi<?> program) {
        return program.map(t -> null);
    }

    public interface PropertyHolder {
        Object get(String propertyName);
    }

    public interface ApplicationExtensionRegistration {}

    public static class ServiceReferenceResourceProvider
        implements ResourceProvider {

        private final ServiceObjects<?> _serviceObjects;
        private CachingServiceReference<?> _serviceReference;

        ServiceReferenceResourceProvider(
            CachingServiceReference<?> serviceReference,
            ServiceObjects<?> serviceObjects) {
            _serviceReference = serviceReference;

            _serviceObjects = serviceObjects;
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

        CachingServiceReference<?> getImmutableServiceReference() {
            return _serviceReference;
        }

    }

    public static class ServiceTuple<T> implements Comparable<ServiceTuple<T>> {

        private final CachingServiceReference<T> _serviceReference;
        private ServiceObjects<T> _serviceObjects;
        private final T _service;

        ServiceTuple(
            CachingServiceReference<T> cachingServiceReference,
            ServiceObjects<T> serviceObjects, T service) {

            _serviceReference = cachingServiceReference;
            _serviceObjects = serviceObjects;
            _service = service;
        }

        @Override
        public int compareTo(ServiceTuple<T> o) {
            return _serviceReference.compareTo(o._serviceReference);
        }

        public T getService() {
            return _service;
        }

        public ServiceObjects<T> getServiceObjects() {
            return _serviceObjects;
        }

        @Override
        public int hashCode() {
            return _serviceReference.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ServiceTuple<?> that = (ServiceTuple<?>) o;

            return _serviceReference.equals(that._serviceReference);
        }

        CachingServiceReference<T> getCachingServiceReference() {
            return _serviceReference;
        }

    }

}
