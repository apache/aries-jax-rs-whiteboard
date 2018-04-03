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
import org.apache.aries.osgi.functional.OSGiResult;
import org.apache.aries.osgi.functional.Publisher;
import org.osgi.framework.Filter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class FilteredPublisher implements AutoCloseable {

    public FilteredPublisher(
        Publisher<? super CachingServiceReference<?>> publisher,
        Filter filter) {

        _publisher = publisher;
        _filter = filter;
    }

    public void close() {
        if (_closed.compareAndSet(false, true)) {
            _results.forEach((__, result) -> result.close());

            _results.clear();
        }
    }

    public void publishIfMatched(CachingServiceReference<?> serviceReference) {
        if (_closed.get()) {
            return;
        }

        if (_filter.match(serviceReference.getServiceReference())) {
            OSGiResult result = _publisher.publish(serviceReference);

            OSGiResult old = _results.put(serviceReference, result);

            if (old != null) {
                old.close();
            }

            if (_closed.get()) {
                result.close();
            }
        }
    }

    public void retractIfMatched(CachingServiceReference<?> serviceReference) {
        if (_closed.get()) {
            return;
        }

        if (_filter.match(serviceReference.getServiceReference())) {
            OSGiResult result = _results.remove(serviceReference);

            if (result != null) {
                result.close();
            }
        }
    }

    private Publisher<? super CachingServiceReference<?>> _publisher;
    private Filter _filter;
    private AtomicBoolean _closed = new AtomicBoolean(false);
    private Map<CachingServiceReference<?>, OSGiResult> _results =
        new HashMap<>();

}
