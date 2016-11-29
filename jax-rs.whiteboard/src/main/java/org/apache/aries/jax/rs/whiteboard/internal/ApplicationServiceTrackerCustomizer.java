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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.ws.rs.core.Application;

import org.apache.cxf.Bus;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

class ApplicationServiceTrackerCustomizer
    implements ServiceTrackerCustomizer
        <Application, ApplicationServiceTrackerCustomizer.Tracked> {

    private BundleContext _bundleContext;
    private Bus _bus;

    public ApplicationServiceTrackerCustomizer(
        BundleContext bundleContext, Bus bus) {

        _bundleContext = bundleContext;
        _bus = bus;
    }

    @Override
    public Tracked addingService(
        ServiceReference<Application> serviceReference) {

        Application application = _bundleContext.getService(
            serviceReference);

        try {
            String[] propertyKeys = serviceReference.getPropertyKeys();

            Map<String, Object> properties = new HashMap<>(
                propertyKeys.length);

            for (String propertyKey : propertyKeys) {
                properties.put(
                    propertyKey, serviceReference.getProperty(propertyKey));
            }

            properties.put(
                "CXF_ENDPOINT_ADDRESS",
                serviceReference.getProperty("osgi.jaxrs.application.base").
                    toString());

            CXFJaxRsServiceRegistrator cxfJaxRsServiceRegistrator =
                new CXFJaxRsServiceRegistrator(_bus, application, properties);

            ServiceRegistration<CXFJaxRsServiceRegistrator>
                cxfJaxRsServiceRegistratorRegistration =
                    _bundleContext.registerService(
                        CXFJaxRsServiceRegistrator.class,
                        cxfJaxRsServiceRegistrator,
                        new Hashtable<>(properties));

            return new Tracked(
                cxfJaxRsServiceRegistrator, application,
                cxfJaxRsServiceRegistratorRegistration);
        }
        catch (Throwable e) {
            _bundleContext.ungetService(serviceReference);

            throw e;
        }
    }

    @Override
    public void modifiedService(
        ServiceReference<Application> serviceReference, Tracked tracked) {

        removedService(serviceReference, tracked);

        addingService(serviceReference);
    }

    @Override
    public void removedService(
        ServiceReference<Application> reference, Tracked tracked) {

        _bundleContext.ungetService(reference);

        tracked._cxfJaxRsServiceRegistrator.close();

        tracked._cxfJaxRsServiceRegistratorServiceRegistration.unregister();
    }

    public static class Tracked {

        private final CXFJaxRsServiceRegistrator _cxfJaxRsServiceRegistrator;
        private final Application _application;
        private final ServiceRegistration<CXFJaxRsServiceRegistrator>
            _cxfJaxRsServiceRegistratorServiceRegistration;

        public Tracked(
            CXFJaxRsServiceRegistrator cxfJaxRsServiceRegistrator,
            Application application,
            ServiceRegistration<CXFJaxRsServiceRegistrator>
                cxfJaxRsServiceRegistratorServiceRegistration) {

            _cxfJaxRsServiceRegistrator = cxfJaxRsServiceRegistrator;
            _application = application;
            _cxfJaxRsServiceRegistratorServiceRegistration =
                cxfJaxRsServiceRegistratorServiceRegistration;
        }

    }
}


