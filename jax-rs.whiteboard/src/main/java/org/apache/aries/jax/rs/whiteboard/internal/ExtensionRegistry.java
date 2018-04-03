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

public class ExtensionRegistry implements AutoCloseable {

    public ExtensionRegistry() {
        _extensionPublishers = new HashSet<>();
        _registeredExtensions = new HashSet<>();
    }

    @Override
    public void close() {
        for (ExtensionPublisher extensionPublisher : _extensionPublishers) {
            extensionPublisher.close();
        }
    }

    public OSGi<CachingServiceReference<?>> waitForExtension(
        String extensionFilter) {

        Filter filter;

        try {
            filter = FrameworkUtil.createFilter(extensionFilter);
        }
        catch (InvalidSyntaxException e) {
            throw new RuntimeException();
        }

        return fromOsgiRunnable((bc, p) -> {
            synchronized (ExtensionRegistry.this) {
                ExtensionPublisher ep = new ExtensionPublisher(p, filter);

                _extensionPublishers.add(ep);

                for (CachingServiceReference<?> extension :
                    _registeredExtensions) {

                    ep.enable(extension);
                }

                return () -> {
                    synchronized (ExtensionRegistry.this) {
                        _extensionPublishers.remove(ep);

                        for (CachingServiceReference<?> extension :
                            _registeredExtensions) {

                            ep.disable(extension);
                        }
                    }
                };
            }

        });
    }

    public void registerExtension(
        CachingServiceReference<?> extension) {

        synchronized (ExtensionRegistry.this) {
            for (ExtensionPublisher publisher : _extensionPublishers) {
                publisher.enable(extension);
            }

            _registeredExtensions.add(extension);
        }
    }

    public void unregisterExtension(
        CachingServiceReference<?> extension) {

        synchronized (ExtensionRegistry.this) {
            for (ExtensionPublisher publisher : _extensionPublishers) {
                publisher.disable(extension);
            }

            _registeredExtensions.remove(extension);
        }
    }

    private final HashSet<ExtensionPublisher> _extensionPublishers;
    private final HashSet<CachingServiceReference<?>> _registeredExtensions;

    private static class ExtensionPublisher implements AutoCloseable {

        public ExtensionPublisher(
            Publisher<? super CachingServiceReference<?>> publisher,
            Filter filter) {

            _publisher = publisher;
            _filter = filter;
        }

        public void close() {
            if (_closed.compareAndSet(false, true)) {
                if (_enabled.compareAndSet(false, true)) {
                    if (_result != null) {
                        _result.close();
                    }
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
