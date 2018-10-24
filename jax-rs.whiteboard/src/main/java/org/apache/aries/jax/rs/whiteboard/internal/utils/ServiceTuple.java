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
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;

import java.util.concurrent.atomic.AtomicReference;

public class ServiceTuple<T> implements Comparable<ServiceTuple<T>> {

    private final CachingServiceReference<T> _serviceReference;
    private ServiceObjects<T> _serviceObjects;
    private AtomicReference<T> _service;

    ServiceTuple(
        CachingServiceReference<T> cachingServiceReference,
        ServiceObjects<T> serviceObjects, T service) {

        _serviceReference = cachingServiceReference;
        _serviceObjects = serviceObjects;
        _service = new AtomicReference<>(service);
    }

    @Override
    public int compareTo(ServiceTuple<T> o) {
        return _serviceReference.compareTo(o._serviceReference);
    }

    public void dispose() {
        T service = _service.getAndSet(null);

        if (service != null) {
            _serviceObjects.ungetService(service);
        }
    }

    public void refresh() {
        dispose();

        if (isAvailable()) {
            _service.set(_serviceObjects.getService());
        }
    }

    public T getService() {
        return _service.get();
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

    public CachingServiceReference<T> getCachingServiceReference() {
        return _serviceReference;
    }

    public boolean isAvailable() {
        return Utils.isAvailable(_serviceObjects.getServiceReference());
    }

}
