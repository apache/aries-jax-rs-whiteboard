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
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.message.Message;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.apache.aries.osgi.functional.OSGi.bundleContext;
import static org.apache.aries.osgi.functional.OSGi.just;
import static org.apache.aries.osgi.functional.OSGi.nothing;
import static org.apache.aries.osgi.functional.OSGi.onClose;

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

    public static Map<String, Object> getProperties(ServiceReference<?> sref) {
        String[] propertyKeys = sref.getPropertyKeys();
        Map<String, Object> properties = new HashMap<>(propertyKeys.length);

        for (String key : propertyKeys) {
            properties.put(key, sref.getProperty(key));
        }

        return properties;
    }

    public static <T> ResourceProvider getResourceProvider(
        ServiceObjects<T> serviceObjects) {

        ServiceReference<T> serviceReference =
            serviceObjects.getServiceReference();

        return new ServiceReferenceResourceProvider(
            serviceReference, serviceObjects);
    }

    public static <K, T extends Comparable<? super T>> OSGi<T> highestPer(
        Function<T, K> keySupplier, OSGi<T> program,
        Consumer<T> onAddingShadowed, Consumer<T> onRemovedShadowed) {

        return program.route(
            new HighestPerRouter<>(
                keySupplier, onAddingShadowed, onRemovedShadowed));
    }

    public static <T> OSGi<ServiceTuple<T>> onlyGettables(
        OSGi<ServiceReference<T>> program,
        Consumer<ServiceReference<T>> whenAddedNotGettable,
        Consumer<ServiceReference<T>> whenLeavingNotGettable) {

        return bundleContext().flatMap(bundleContext ->
            program.flatMap(serviceReference -> {
                T service = null;

                try {
                    service = bundleContext.getService(serviceReference);
                }
                catch (Exception e){
                }
                if (service == null) {
                    whenAddedNotGettable.accept(serviceReference);

                    return
                        onClose(
                            () -> whenLeavingNotGettable.accept(
                                serviceReference)
                        ).then(
                            nothing()
                        );
                }
                return
                    onClose(
                        () -> bundleContext.ungetService(serviceReference)
                    ).then(
                        just(new ServiceTuple<>(serviceReference, service))
                    );
            }));
    }

    public static <T extends Comparable<? super T>> OSGi<T> highestRanked(
        OSGi<T> program) {

        return program.route(new HighestRankedRouter<>());
    }

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

    public static void unregisterEndpoint(
        CXFJaxRsServiceRegistrator registrator,
        ResourceProvider resourceProvider) {

        registrator.remove(resourceProvider);
    }

    public static void updateProperty(
        ServiceRegistration<?> serviceRegistration, String key, Object value) {

        ServiceReference<?> serviceReference =
            serviceRegistration.getReference();

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
        private ServiceReference<?> _serviceReference;

        ServiceReferenceResourceProvider(
            ServiceReference<?> serviceReference,
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

        ServiceReference<?> getServiceReference() {
            return _serviceReference;
        }

    }

    public static class ServiceTuple<T> implements Comparable<ServiceTuple<T>> {

        private final ServiceReference<T> _serviceReference;
        private final T _service;

        ServiceTuple(ServiceReference<T> a, T service) {
            _serviceReference = a;
            _service = service;
        }

        @Override
        public int compareTo(ServiceTuple<T> o) {
            return _serviceReference.compareTo(o._serviceReference);
        }

        public T getService() {
            return _service;
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

        ServiceReference<T> getServiceReference() {
            return _serviceReference;
        }



    }

    private static class HighestRankedRouter<T extends Comparable<? super T>>
        implements Consumer<OSGi.Router<T>> {

        private final TreeSet<Event<T>> _set;
        private final Comparator<Event<T>> _comparator;

        public HighestRankedRouter() {
            _comparator = Comparator.comparing(Event::getContent);

            _set = new TreeSet<>(_comparator);
        }

        @Override
        public void accept(OSGi.Router<T> router) {
            router.onIncoming(ev -> {
                synchronized (_set) {
                    Event<T> last = _set.size() > 0 ? _set.last() : null;

                    boolean higher =
                        (last == null) ||
                            (_comparator.compare(ev, last) > 0);

                    if (higher) {
                        if (last != null) {
                            router.signalLeave(last);
                        }

                        router.signalAdd(ev);
                    }

                    _set.add(ev);
                }
            });
            router.onLeaving(ev -> {
                synchronized (_set) {
                    if (_set.isEmpty()) {
                        return;
                    }

                    T content = ev.getContent();

                    Event<T> last = _set.last();

                    if (content.equals(last.getContent())) {
                        router.signalLeave(ev);

                        Event<T> penultimate = _set.lower(ev);

                        if (penultimate != null) {
                            router.signalAdd(penultimate);
                        }
                    }

                    _set.removeIf(t -> content.equals(t.getContent()));
                }
            });
            router.onClose(() -> {
                synchronized (_set) {
                    Iterator<Event<T>> iterator = _set.descendingIterator();

                    while (iterator.hasNext()) {
                        Event<T> event = iterator.next();

                        router.signalLeave(event);

                        iterator.remove();
                    }
                }
            });
        }

    }

    private static class HighestPerRouter<T extends Comparable<? super T>, K>
        implements Consumer<OSGi.Router<T>> {

        private final Function<T, K> _keySupplier;
        private final ConcurrentHashMap<K, TreeSet<Event<T>>> _map =
            new ConcurrentHashMap<>();
        private final Consumer<T> _onAddingShadowed;
        private final Consumer<T> _onRemovedShadowed;

        public HighestPerRouter(
            Function<T, K> keySupplier,
            Consumer<T> onAddingShadowed, Consumer<T> onRemovedShadowed) {

            _keySupplier = keySupplier;
            _onAddingShadowed = onAddingShadowed;
            _onRemovedShadowed = onRemovedShadowed;
        }

        @Override
        public void accept(OSGi.Router<T> router) {
            router.onIncoming(e -> {
                K key = _keySupplier.apply(e.getContent());

                Comparator<Event<T>> comparator = Comparator.comparing(
                    Event::getContent);

                _map.compute(
                    key,
                    (__, set) -> {
                        if (set == null) {
                            set = new TreeSet<>(comparator);
                        }

                        Event<T> last = set.size() > 0 ? set.last() : null;

                        boolean higher =
                            (last == null) ||
                                (comparator.compare(e, last) > 0);

                        if (higher) {
                            if (last != null) {
                                router.signalLeave(last);

                                _onAddingShadowed.accept(last.getContent());
                            }

                            router.signalAdd(e);
                        } else {
                            _onAddingShadowed.accept(e.getContent());
                        }

                        set.add(e);

                        return set;
                    });
            });
            router.onLeaving(e -> {
                T content = e.getContent();

                K key = _keySupplier.apply(content);

                _map.compute(
                    key,
                    (__, set) -> {
                        if (set.isEmpty()) {
                            return set;
                        }

                        Event<T> last = set.last();

                        if (content.equals(last.getContent())) {
                            router.signalLeave(e);

                            Event<T> penultimate = set.lower(last);

                            if (penultimate != null) {
                                router.signalAdd(penultimate);

                                _onRemovedShadowed.accept(
                                    penultimate.getContent());
                            }
                        } else {
                            _onRemovedShadowed.accept(content);
                        }

                        set.removeIf(t -> t.getContent().equals(content));

                        return set;
                    }
                );
            });
        }

    }

}
