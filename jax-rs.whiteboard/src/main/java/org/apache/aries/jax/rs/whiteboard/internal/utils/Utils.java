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

package org.apache.aries.jax.rs.whiteboard.internal.utils;

import org.apache.aries.osgi.functional.CachingServiceReference;
import org.apache.aries.osgi.functional.OSGi;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.apache.aries.jax.rs.whiteboard.internal.utils.LogUtils.ifDebugEnabled;
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

    public static Map<String, Object> getProperties(
        CachingServiceReference<?> sref) {

        String[] propertyKeys = sref.getPropertyKeys();
        Map<String, Object> properties = new HashMap<>(propertyKeys.length);

        for (String key : propertyKeys) {
            properties.put(key, sref.getProperty(key));
        }

        return properties;
    }

    public static long getServiceId(
        CachingServiceReference<?> cachingServiceReference) {

        return (long)cachingServiceReference.getProperty("service.id");
    }

    public static long getServiceId(ServiceReference<?> serviceReference) {
        return (long)serviceReference.getProperty("service.id");
    }

    public static <T> ServiceReferenceResourceProvider getResourceProvider(
        ServiceObjects<T> serviceObjects) {

        CachingServiceReference<T> serviceReference =
            new CachingServiceReference<>(
                serviceObjects.getServiceReference());

        return new ServiceReferenceResourceProvider(
            serviceReference, serviceObjects);
    }

    public static <K, T extends Comparable<? super T>> OSGi<T> highestPer(
        Function<T, OSGi<K>> keySupplier, OSGi<T> program,
        Consumer<? super T> onAddingShadowed,
        Consumer<? super T> onRemovedShadowed) {

        return program.splitBy(
            keySupplier,
            (__, p) -> highest(
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
        Consumer<CachingServiceReference<T>> whenLeavingNotGettable,
        Logger log) {

        return bundleContext().flatMap(bundleContext ->
            program.recoverWith(
                (serviceReference, e) ->
                    notGettableResult(
                        whenAddedNotGettable, whenLeavingNotGettable,
                        serviceReference, log)
            ).flatMap(serviceReference -> {
                ServiceObjects<T> serviceObjects =
                    bundleContext.getServiceObjects(
                        serviceReference.getServiceReference());
                T service = serviceObjects.getService();

                if (service == null) {
                    return notGettableResult(
                        whenAddedNotGettable, whenLeavingNotGettable,
                        serviceReference, log);
                }

                return
                    just(new ServiceTuple<>(
                            serviceReference, serviceObjects, service)).
                    effects(__ -> {}, ServiceTuple::dispose).
                    effects(
                        ifDebugEnabled(
                            log,
                            () -> "Obtained instance from " + serviceReference),
                        ifDebugEnabled(
                            log,
                            () -> "Released instance from " + serviceReference)
                    );
            }));
    }

    private static <T, S> OSGi<S> notGettableResult(
        Consumer<CachingServiceReference<T>> whenAddedNotGettable,
        Consumer<CachingServiceReference<T>> whenLeavingNotGettable,
        CachingServiceReference<T> immutable, Logger log) {

        return effects(
            () -> whenAddedNotGettable.accept(immutable),
            () -> whenLeavingNotGettable.accept(immutable)
        ).effects(
            ifDebugEnabled(log, () -> "Tracked not gettable reference {}"),
            ifDebugEnabled(log, () -> "Untracked not gettable reference {}")
        ).
        then(
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

}
