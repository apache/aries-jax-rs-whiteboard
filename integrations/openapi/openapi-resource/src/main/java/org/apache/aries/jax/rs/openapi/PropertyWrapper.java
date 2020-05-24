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

package org.apache.aries.jax.rs.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import org.apache.aries.component.dsl.CachingServiceReference;

import java.util.Collection;
import java.util.function.Consumer;

public class PropertyWrapper {

    private CachingServiceReference<OpenAPI> cachingServiceReference;

    public PropertyWrapper(CachingServiceReference<OpenAPI> cachingServiceReference) {
        this.cachingServiceReference = cachingServiceReference;
    }

    public void applyBoolean(String property, Consumer<Boolean> consumer) {
        Object value = cachingServiceReference.getProperty(property);

        if (value instanceof Boolean) {
            consumer.accept((Boolean) value);
        }
        if (value instanceof String) {
            consumer.accept(Boolean.parseBoolean((String) value));
        }
    }

    public void applyString(String property, Consumer<String> consumer) {
        Object value = cachingServiceReference.getProperty(property);

        if ((value != null)) {
            consumer.accept(value.toString());
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Collection<String>> void applyStringCollection(
        String property, Consumer<T> consumer) {

        Object value = cachingServiceReference.getProperty(property);

        if (value instanceof Collection) {
            consumer.accept((T)value);
        }
    }

    public void applyLong(String property, Consumer<Long> consumer) {
        Object value = cachingServiceReference.getProperty(property);

        if (value instanceof Long) {
            consumer.accept((Long) value);
        }
        if (value instanceof String) {
            consumer.accept(Long.parseLong((String) value));
        }
    }
}
