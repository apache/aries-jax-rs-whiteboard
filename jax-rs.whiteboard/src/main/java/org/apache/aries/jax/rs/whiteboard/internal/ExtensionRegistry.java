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
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

import java.util.HashSet;

import static org.apache.aries.osgi.functional.OSGi.fromOsgiRunnable;

public class ExtensionRegistry implements AutoCloseable {

    public ExtensionRegistry() {
        _extensionPublishers = new HashSet<>();
        _registeredExtensions = new HashSet<>();
    }

    @Override
    public void close() {
        for (FilteredPublisher extensionPublisher : _extensionPublishers) {
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
                FilteredPublisher ep = new FilteredPublisher(p, filter);

                _extensionPublishers.add(ep);

                for (CachingServiceReference<?> extension :
                    _registeredExtensions) {

                    ep.publishIfMatched(extension);
                }

                return () -> {
                    synchronized (ExtensionRegistry.this) {
                        _extensionPublishers.remove(ep);

                        for (CachingServiceReference<?> extension :
                            _registeredExtensions) {

                            ep.retractIfMatched(extension);
                        }
                    }
                };
            }

        });
    }

    public void registerExtension(
        CachingServiceReference<?> extension) {

        synchronized (ExtensionRegistry.this) {
            for (FilteredPublisher publisher : _extensionPublishers) {
                publisher.publishIfMatched(extension);
            }

            _registeredExtensions.add(extension);
        }
    }

    public void unregisterExtension(
        CachingServiceReference<?> extension) {

        synchronized (ExtensionRegistry.this) {
            for (FilteredPublisher publisher : _extensionPublishers) {
                publisher.retractIfMatched(extension);
            }

            _registeredExtensions.remove(extension);
        }
    }

    private final HashSet<FilteredPublisher> _extensionPublishers;
    private final HashSet<CachingServiceReference<?>> _registeredExtensions;

}
