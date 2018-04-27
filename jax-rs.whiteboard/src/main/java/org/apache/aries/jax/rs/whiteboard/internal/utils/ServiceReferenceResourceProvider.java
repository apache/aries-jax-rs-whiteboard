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

package org.apache.aries.jax.rs.whiteboard.internal.utils;

import org.apache.aries.component.dsl.CachingServiceReference;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.message.Message;
import org.osgi.framework.ServiceObjects;

public class ServiceReferenceResourceProvider
    implements ResourceProvider {

    private final ServiceObjects<?> _serviceObjects;
    private CachingServiceReference<?> _serviceReference;

    ServiceReferenceResourceProvider(
        CachingServiceReference<?> serviceReference,
        ServiceObjects<?> serviceObjects) {
        _serviceReference = serviceReference;

        _serviceObjects = serviceObjects;
    }

    @Override
    public Object getInstance(Message m) {
        return _serviceObjects.getService();
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void releaseInstance(Message m, Object o) {
        ((ServiceObjects)_serviceObjects).ungetService(o);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class<?> getResourceClass() {
        Object service = _serviceObjects.getService();

        try {
            return service.getClass();
        }
        finally {
            ((ServiceObjects)_serviceObjects).ungetService(service);
        }
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    public CachingServiceReference<?> getImmutableServiceReference() {
        return _serviceReference;
    }

}
