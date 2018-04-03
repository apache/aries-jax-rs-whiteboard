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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import static org.apache.aries.osgi.functional.OSGi.fromOsgiRunnable;

public class ApplicationExtensionRegistry implements AutoCloseable {

    public ApplicationExtensionRegistry() {
        _applicationPublishers = new HashMap<>();
        _applicationRegisteredExtensions = new HashMap<>();
    }

    public void close() {
        _applicationPublishers.forEach(
            (__, aeps) -> aeps.forEach(FilteredPublisher::close)
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
                FilteredPublisher aep =
                    new FilteredPublisher(p, filter);

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
                        aep.publishIfMatched(extension);
                    }
                }

                return () -> {
                    synchronized (ApplicationExtensionRegistry.this) {
                        Collection<FilteredPublisher> set =
                            _applicationPublishers.get(applicationName);

                        set.remove(aep);

                        if (extensions != null) {
                            for (CachingServiceReference<?> extension :
                                extensions) {

                                aep.retractIfMatched(extension);
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
            Collection<FilteredPublisher> publishers =
                _applicationPublishers.get(applicationName);

            if (publishers != null) {
                for (FilteredPublisher publisher : publishers) {
                    publisher.publishIfMatched(extension);
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
            Collection<FilteredPublisher> publishers =
                _applicationPublishers.get(applicationName);

            if (publishers != null) {
                for (FilteredPublisher publisher : publishers) {
                    publisher.retractIfMatched(extension);
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
        <String, Collection<FilteredPublisher>>
            _applicationPublishers;
    private final HashMap<String, Collection<CachingServiceReference<?>>>
        _applicationRegisteredExtensions;

}
