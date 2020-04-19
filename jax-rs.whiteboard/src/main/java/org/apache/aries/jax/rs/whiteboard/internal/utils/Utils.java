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

import org.apache.aries.component.dsl.CachingServiceReference;
import org.apache.aries.component.dsl.OSGi;
import org.osgi.framework.Bundle;
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
import static org.apache.aries.component.dsl.OSGi.bundleContext;
import static org.apache.aries.component.dsl.OSGi.effects;
import static org.apache.aries.component.dsl.OSGi.just;
import static org.apache.aries.component.dsl.OSGi.nothing;
import static org.apache.aries.component.dsl.Utils.highest;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_NAME;

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

    public static String canonicalizeAddress(String address) {
        if (address == null) {
            return "";
        }

        if (address.length() == 0) {
            return address;
        }

        if (!address.startsWith("/")) {
            address = "/" + address;
        }

        if (address.endsWith("/")) {
            address = address.substring(0, address.length() - 1);
        }

        return address;
    }

    public static String generateApplicationName(
        PropertyHolder propertyHolder) {

        return ".generated.for." + propertyHolder.get("service.id");
    }

    public static Map<String, Object> getApplicationProperties(
        CachingServiceReference<?> reference) {

        Map<String, Object> properties = Utils.getProperties(reference);

        properties.putIfAbsent(
            JAX_RS_NAME, generateApplicationName(reference::getProperty));

        return properties;
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

    public static Map<String, Object> getProperties(ServiceReference<?> sref) {
        String[] propertyKeys = sref.getPropertyKeys();
        Map<String, Object> properties = new HashMap<>(propertyKeys.length);

        for (String key : propertyKeys) {
            properties.put(key, sref.getProperty(key));
        }

        return properties;
    }

    public static String getString(Object string) {
        if (string == null) {
            return "";
        }
        else {
            return String.valueOf(string);
        }
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

    public static boolean isAvailable(ServiceReference<?> serviceReference) {
        Bundle bundle = serviceReference.getBundle();

        if (bundle == null) {
            return false;
        }

        for (ServiceReference<?> registeredService :
            bundle.getRegisteredServices()) {

            if (registeredService.equals(serviceReference)) {
                return true;
            }
        }

        return false;
    }

    public static void mergePropertyMaps(
        Map<String, Object> receptor, Map<String, ?> map) {

        for (Map.Entry<String, ?> entry :
            map.entrySet()) {

            String key = entry.getKey();

            if (key.startsWith(".")) {
                continue;
            }

            receptor.putIfAbsent(key, entry.getValue());
        }
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

    public static <T> OSGi<T> service(
        CachingServiceReference<T> immutableServiceReference) {

        ServiceReference<T> serviceReference =
            immutableServiceReference.getServiceReference();

        return
            bundleContext().flatMap(bundleContext ->
            effects(
                () -> {},
                () -> bundleContext.ungetService(serviceReference)).then(
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

}
