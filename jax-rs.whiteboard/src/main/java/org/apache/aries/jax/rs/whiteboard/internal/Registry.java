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

import org.apache.aries.component.dsl.OSGi;
import org.apache.aries.component.dsl.OSGiResult;
import org.apache.aries.component.dsl.internal.ConcurrentDoublyLinkedList;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import static org.apache.aries.component.dsl.OSGi.fromOsgiRunnable;

public class Registry<T> implements AutoCloseable {

    public Registry() {
        _publishers = new HashSet<>();
        _servicesWithPropertiesList = new ConcurrentDoublyLinkedList<>();
    }

    @Override
    public void close() {
        for (FilteredPublisher publisher : new HashSet<>(_publishers)) {
            publisher.close();
        }
    }

    public OSGi<T> waitForService(String filterString) {
        Filter filter;

        try {
            filter = FrameworkUtil.createFilter(filterString);
        }
        catch (InvalidSyntaxException e) {
            throw new RuntimeException();
        }

        return fromOsgiRunnable((bc, p) -> {
            synchronized (Registry.this) {
                FilteredPublisher<T> ep = new FilteredPublisher<>(p, filter);

                _publishers.add(ep);

                for (ServiceWithProperties<T> serviceWithProperties :
                    new ArrayList<>(_servicesWithPropertiesList)) {

                    ep.publishIfMatched(
                        serviceWithProperties.service,
                        serviceWithProperties.properties);
                }

                return () -> {
                    synchronized (Registry.this) {
                        _publishers.remove(ep);

                        for (ServiceWithProperties<T> serviceWithProperties :
                            new ArrayList<>(_servicesWithPropertiesList)) {

                            ep.retract(serviceWithProperties.service);
                        }
                    }
                };
            }

        });
    }

    public OSGi<T> registerService(T service, Map<String, ?> properties) {
        return (bc, p) -> {
            synchronized (Registry.this) {
                ConcurrentDoublyLinkedList.Node node =
                    _servicesWithPropertiesList.addLast(
                        new ServiceWithProperties<>(service, properties));

                OSGiResult result = p.publish(service);

                for (FilteredPublisher<T> publisher :
                    new HashSet<>(_publishers)) {

                    publisher.publishIfMatched(service, properties);
                }

                return () -> {
                    synchronized (Registry.this) {
                        for (FilteredPublisher<T> publisher :
                            new HashSet<>(_publishers)) {

                            publisher.retract(service);
                        }

                        result.close();
                        node.remove();
                    }
                };
            }
        };
    }


    private final HashSet<FilteredPublisher<T>> _publishers;
    private final ConcurrentDoublyLinkedList<ServiceWithProperties<T>>
        _servicesWithPropertiesList;

    private static class ServiceWithProperties<T> {

        T service;
        Map<String, ?> properties;

        ServiceWithProperties(T service, Map<String, ?> properties) {
            this.service = service;
            this.properties = properties;
        }
    }

}
