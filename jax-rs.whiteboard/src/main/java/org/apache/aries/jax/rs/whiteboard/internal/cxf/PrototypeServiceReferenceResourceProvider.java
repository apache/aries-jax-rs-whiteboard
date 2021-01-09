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

package org.apache.aries.jax.rs.whiteboard.internal.cxf;

import org.apache.aries.component.dsl.CachingServiceReference;
import org.apache.aries.jax.rs.whiteboard.internal.utils.Utils;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.message.Message;
import org.osgi.framework.ServiceObjects;

public class PrototypeServiceReferenceResourceProvider
    implements ResourceProvider, ServiceReferenceResourceProvider {

    public PrototypeServiceReferenceResourceProvider(
        CachingServiceReference<?> serviceReference,
        Class<?> serviceClass, ServiceObjects<?> serviceObjects) {

        _serviceReference = serviceReference;
        _serviceClass = serviceClass;
        _serviceObjects = serviceObjects;

        _messageKey = _MESSAGE_INSTANCE_KEY_PREFIX + _serviceClass;
    }

    @Override
    public Object getInstance(Message m) {
        Object object = m.get(_messageKey);

        if (object != null) {
            return object;
        }

        if (isAvailable()) {
            Object service = _serviceObjects.getService();

            m.put(_messageKey, service);

            return service;
        }
        else {
            return null;
        }
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void releaseInstance(Message m, Object o) {
        ((ServiceObjects)_serviceObjects).ungetService(o);

        m.remove(_messageKey);
    }

    @Override
    public Class<?> getResourceClass() {
        return _serviceClass;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    public CachingServiceReference<?> getImmutableServiceReference() {
        return _serviceReference;
    }

    public boolean isAvailable() {
        return Utils.isAvailable(_serviceObjects.getServiceReference());
    }

    private static final String _MESSAGE_INSTANCE_KEY_PREFIX =
        "org.apache.aries.jax.rs.whiteboard.internal.cxf." +
            "PrototypeServiceReferenceResourceProvider.";

    private final String _messageKey;
    private Class<?> _serviceClass;
    private final ServiceObjects<?> _serviceObjects;
    private CachingServiceReference<?> _serviceReference;

}
