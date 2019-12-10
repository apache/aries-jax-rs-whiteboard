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

import org.apache.aries.component.dsl.OSGiResult;
import org.apache.aries.component.dsl.Publisher;
import org.osgi.framework.Filter;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class FilteredPublisher<T> implements AutoCloseable {

    public FilteredPublisher(Publisher<? super T> publisher, Filter filter) {
        _publisher = publisher;
        _filter = filter;
    }

    public void close() {
        if (_closed.compareAndSet(false, true)) {
            _results.forEach((__, result) -> result.forEach(OSGiResult::close));

            _results.clear();
        }
    }

    public void publishIfMatched(T t, Map<String, ?> properties) {
        if (_closed.get()) {
            return;
        }

        if (_filter.matches(properties)) {
            OSGiResult result = _publisher.publish(t);

            _results.compute(
                    t,
                    (__, results) -> {
                        if (results == null) {
                            results = new ArrayList<>();
                        }

                        results.add(result);

                        return results;
                    });

            if (_closed.get()) {
                result.close();
            }
        }
    }

    public void retract(T t) {
        if (_closed.get()) {
            return;
        }

        List<OSGiResult> resultList = _results.remove(t);

        if (resultList != null) {
            resultList.forEach(OSGiResult::close);
        }
    }

    private Publisher<? super T> _publisher;
    private Filter _filter;
    private AtomicBoolean _closed = new AtomicBoolean(false);
    private IdentityHashMap<T, List<OSGiResult>> _results = new IdentityHashMap<>();

}
