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

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.apache.cxf.Bus;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class SingletonServiceTrackerCustomizer
    implements ServiceTrackerCustomizer<Object, TrackedJaxRsRegistrator> {

    private BundleContext _bundleContext;
    private Bus _bus;

    public SingletonServiceTrackerCustomizer(BundleContext bundleContext, Bus bus) {
        _bundleContext = bundleContext;
        _bus = bus;
    }

    @Override
    public TrackedJaxRsRegistrator addingService(
        ServiceReference<Object> serviceReference) {

        final Object service = _bundleContext.getService(serviceReference);
        Application application = new Application() {
            @Override
            public Set<Object> getSingletons() {
                return Collections.singleton(service);
            }
        };
        try {
            Map<String, Object> properties = CXFJaxRsServiceRegistrator
                .getProperties(serviceReference, "osgi.jaxrs.resource.base");
            CXFJaxRsServiceRegistrator cxfJaxRsServiceRegistrator = 
                new CXFJaxRsServiceRegistrator(_bus, application, properties);
            return new TrackedJaxRsRegistrator(cxfJaxRsServiceRegistrator, _bundleContext, properties);
        }
        catch (Exception e) {
            _bundleContext.ungetService(serviceReference);
            throw e;
        }
    }

    @Override
    public void modifiedService(ServiceReference<Object> serviceReference, TrackedJaxRsRegistrator tracked) {
        removedService(serviceReference, tracked);
        addingService(serviceReference);
    }

    @Override
    public void removedService(ServiceReference<Object> reference, TrackedJaxRsRegistrator tracked) {
        _bundleContext.ungetService(reference);
        tracked.close();
    }

}
