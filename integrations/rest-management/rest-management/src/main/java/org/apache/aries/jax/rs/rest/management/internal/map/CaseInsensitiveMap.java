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

package org.apache.aries.jax.rs.rest.management.internal.map;

import static java.util.stream.Collectors.toCollection;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * This Map is used for case-insensitive key lookup during filter
 * evaluation. This Map implementation only supports the get operation using
 * a String key as no other operations are used by the Filter
 * implementation.
 */
public class CaseInsensitiveMap<T> extends AbstractMap<String, T> {
    private final String[] keys;
    private final Set<Map.Entry<String, T>> properties;

    /**
     * Create a case insensitive map from the specified dictionary.
     *
     * @param dictionary
     * @throws IllegalArgumentException If {@code dictionary} contains case
     *             variants of the same key name.
     */
    public CaseInsensitiveMap(final Dictionary<String, T> dictionary) {
        this(() -> {
            Set<Map.Entry<String, T>> entries = new HashSet<>(dictionary.size());
            for (Enumeration<String> e = dictionary.keys(); e.hasMoreElements();) {
                final String k = e.nextElement();
                entries.add(
                    new SimpleImmutableEntry<String, T>(k, null) {
                        public T getValue() {
                            return dictionary.get(getKey());
                        }
                    }
                );
            }
            return entries;
        });
    }

    /**
     * Create a case insensitive map from the specified map.
     *
     * @param properties
     * @throws IllegalArgumentException If {@code dictionary} contains case
     *             variants of the same key name.
     */
    public CaseInsensitiveMap(final Map<String, T> properties) {
        this(() -> properties.entrySet());
    }

    CaseInsensitiveMap(final Supplier<Set<Map.Entry<String, T>>> properties) {
        this.properties = properties.get().stream().sorted(
            (a, b) -> a.getKey().compareTo(b.getKey())
        ).collect(
            toCollection(LinkedHashSet::new)
        );

        List<String> keyList = new ArrayList<>(this.properties.size());

        this.properties.forEach(e -> {
            for (String i : keyList) {
                if (e.getKey().equalsIgnoreCase(i)) {
                    throw new IllegalArgumentException();
                }
            }
            keyList.add(e.getKey());
        });

        this.keys = keyList.toArray(new String[0]);
    }

    @Override
    public T get(Object o) {
        String k = (String) o;
        for (String key : keys) {
            if (key.equalsIgnoreCase(k)) {
                return super.get(key);
            }
        }
        return null;
    }

    @Override
    public Set<Entry<String, T>> entrySet() {
        return this.properties;
    }

}