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
import org.apache.aries.osgi.functional.OSGiResult;
import org.apache.aries.osgi.functional.Publisher;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.aries.osgi.functional.OSGi.fromOsgiRunnable;

public class ApplicationExtensionRegistry implements AutoCloseable {

    public ApplicationExtensionRegistry() {
        _applicationPublishers = new HashMap<>();
        _applicationRegisteredExtensions = new HashMap<>();
    }

    public void close() {
        _applicationPublishers.forEach(
            (__, aeps) -> aeps.forEach(ApplicationExtensionPublisher::close)
        );
    }

    public OSGi<CachingServiceReference<?>> waitForApplicationExtension(
            String applicationName, String extensionFilter) {

        Filter filter;

        try {
            filter = FrameworkUtil.createFilter(extensionFilter);
        }
        catch (InvalidSyntaxException e) {
            throw new RuntimeException();
        }

        return fromOsgiRunnable((bc, p) -> {
            synchronized (ApplicationExtensionRegistry.this) {
                ApplicationExtensionPublisher aep =
                    new ApplicationExtensionPublisher(p, filter);

                _applicationPublishers.compute(
                    applicationName,
                    (__, set) -> {
                        if (set == null) {
                            set = new HashSet<>();
                        }

                        set.add(aep);

                        return set;
                    });

                Collection<CachingServiceReference<?>> extensions =
                    _applicationRegisteredExtensions.get(applicationName);

                if (extensions != null) {
                    for (CachingServiceReference<?> extension : extensions) {
                        aep.enable(extension);
                    }
                }

                return () -> {
                    synchronized (ApplicationExtensionRegistry.this) {
                        Collection<ApplicationExtensionPublisher> set =
                            _applicationPublishers.get(applicationName);

                        set.remove(aep);

                        if (extensions != null) {
                            for (CachingServiceReference<?> extension :
                                extensions) {

                                aep.disable(extension);
                            }
                        }
                    }
                };
            }

        });
    }

    public void registerExtensionInApplication(
        String applicationName, CachingServiceReference<?> extension) {

        synchronized (ApplicationExtensionRegistry.this) {
            Collection<ApplicationExtensionPublisher> publishers =
                _applicationPublishers.get(applicationName);

            if (publishers != null) {
                for (ApplicationExtensionPublisher publisher : publishers) {
                    publisher.enable(extension);
                }
            }

            _applicationRegisteredExtensions.compute(
                applicationName,
                (__, set) -> {
                    if (set == null) {
                        set = new HashSet<>();
                    }

                    set.add(extension);

                    return set;
                }
            );
        }
    }

    public void unregisterExtensionInApplication(
        String applicationName, CachingServiceReference<?> extension) {

        synchronized (ApplicationExtensionRegistry.this) {
            Collection<ApplicationExtensionPublisher> publishers =
                _applicationPublishers.get(applicationName);

            if (publishers != null) {
                for (ApplicationExtensionPublisher publisher : publishers) {
                    publisher.disable(extension);
                }
            }

            _applicationRegisteredExtensions.compute(
                applicationName,
                (__, set) -> {
                    if (set == null) {
                        set = new HashSet<>();
                    }

                    set.remove(extension);

                    return set;
                }
            );
        }
    }

    private final HashMap
        <String, Collection<ApplicationExtensionPublisher>>
            _applicationPublishers;
    private final HashMap<String, Collection<CachingServiceReference<?>>>
        _applicationRegisteredExtensions;

    private static class ApplicationExtensionPublisher
        implements AutoCloseable {

        public ApplicationExtensionPublisher(
            Publisher<? super CachingServiceReference<?>> publisher,
            Filter filter) {

            _publisher = publisher;
            _filter = filter;
        }

        public void close() {
            if (_closed.compareAndSet(false, true)) {
                if (_enabled.compareAndSet(true, false)) {
                    if (_result != null) {
                        _result.close();
                    }

                    _result = null;
                }
            }

        }


        public void enable(CachingServiceReference<?> serviceReference) {
            if (_closed.get()) {
                return;
            }

            if (_filter.match(serviceReference.getServiceReference())) {

                if (_enabled.compareAndSet(false, true)) {
                    _result = _publisher.publish(serviceReference);
                }
            }
        }

        public void disable(CachingServiceReference<?> serviceReference) {
            if (_closed.get()) {
                return;
            }

            if (_filter.match(serviceReference.getServiceReference())) {
                if (_enabled.compareAndSet(true, false)) {
                    if (_result != null) {
                        _result.close();
                    }

                    _result = null;
                }
            }
        }

        private Publisher<? super CachingServiceReference<?>> _publisher;
        private Filter _filter;
        private AtomicBoolean _enabled = new AtomicBoolean(false);
        private AtomicBoolean _closed = new AtomicBoolean(false);
        private OSGiResult _result;

    }

}
