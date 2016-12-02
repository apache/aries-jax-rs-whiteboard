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

import java.util.Map;

import javax.ws.rs.core.Application;

import org.apache.cxf.Bus;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ApplicationServiceTrackerCustomizer
    implements ServiceTrackerCustomizer<Application, TrackedJaxRsRegistrator> {

    private BundleContext _bundleContext;
    private Bus _bus;

    public ApplicationServiceTrackerCustomizer(BundleContext bundleContext, Bus bus) {
        _bundleContext = bundleContext;
        _bus = bus;
    }

    @Override
    public TrackedJaxRsRegistrator addingService(ServiceReference<Application> serviceReference) {
        Application application = _bundleContext.getService(serviceReference);
        try {
            Map<String, Object> props = CXFJaxRsServiceRegistrator
                .getProperties(serviceReference, "osgi.jaxrs.application.base");
            CXFJaxRsServiceRegistrator registrator = new CXFJaxRsServiceRegistrator(_bus, application, props);
            return new TrackedJaxRsRegistrator(registrator, _bundleContext, props);
        }
        catch (Throwable e) {
            _bundleContext.ungetService(serviceReference);
            throw e;
        }
    }

    @Override
    public void modifiedService(ServiceReference<Application> serviceReference, TrackedJaxRsRegistrator tracked) {
        removedService(serviceReference, tracked);
        addingService(serviceReference);
    }

    @Override
    public void removedService(ServiceReference<Application> reference, TrackedJaxRsRegistrator tracked) {
        _bundleContext.ungetService(reference);
        tracked.close();
    }
}
