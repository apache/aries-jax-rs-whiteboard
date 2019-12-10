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

import org.apache.aries.component.dsl.CachingServiceReference;
import org.apache.aries.component.dsl.OSGi;
import org.apache.aries.component.dsl.OSGiResult;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

import java.util.HashSet;

import static org.apache.aries.component.dsl.OSGi.fromOsgiRunnable;

public class ServiceReferenceRegistry implements AutoCloseable {

    public ServiceReferenceRegistry() {
        _extensionPublishers = new HashSet<>();
        _registeredExtensions = new HashSet<>();
    }

    @Override
    public void close() {
        for (ServiceReferenceFilteredPublisher extensionPublisher :
            new HashSet<>(_extensionPublishers)) {

            extensionPublisher.close();
        }
    }

    public OSGi<CachingServiceReference<?>> waitFor(String filterString) {
        Filter filter;

        try {
            filter = FrameworkUtil.createFilter(filterString);
        }
        catch (InvalidSyntaxException e) {
            throw new RuntimeException();
        }

        return fromOsgiRunnable((bc, p) -> {
            synchronized (ServiceReferenceRegistry.this) {
                ServiceReferenceFilteredPublisher ep =
                    new ServiceReferenceFilteredPublisher(p, filter);

                _extensionPublishers.add(ep);

                for (CachingServiceReference<?> extension :
                    new HashSet<>(_registeredExtensions)) {

                    ep.publishIfMatched(extension);
                }

                return () -> {
                    synchronized (ServiceReferenceRegistry.this) {
                        _extensionPublishers.remove(ep);

                        for (CachingServiceReference<?> extension :
                            new HashSet<>(_registeredExtensions)) {

                            ep.retractIfMatched(extension);
                        }
                    }
                };
            }

        });
    }

    public void register(CachingServiceReference<?> serviceReference) {
        synchronized (ServiceReferenceRegistry.this) {
            _registeredExtensions.add(serviceReference);

            for (ServiceReferenceFilteredPublisher publisher :
                new HashSet<>(_extensionPublishers)) {

                publisher.publishIfMatched(serviceReference);
            }
        }
    }

    public void unregister(CachingServiceReference<?> serviceReference) {
        synchronized (ServiceReferenceRegistry.this) {
            for (ServiceReferenceFilteredPublisher publisher :
                new HashSet<>(_extensionPublishers)) {

                publisher.retractIfMatched(serviceReference);
            }

            _registeredExtensions.remove(serviceReference);
        }
    }

    private final HashSet<ServiceReferenceFilteredPublisher>
        _extensionPublishers;
    private final HashSet<CachingServiceReference<?>> _registeredExtensions;

}
