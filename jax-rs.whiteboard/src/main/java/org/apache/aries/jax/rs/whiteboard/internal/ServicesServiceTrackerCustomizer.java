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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ServicesServiceTrackerCustomizer
    implements ServiceTrackerCustomizer
        <Object, ServiceTracker
            <CXFJaxRsServiceRegistrator, ?>> {

    private final BundleContext _bundleContext;

    public ServicesServiceTrackerCustomizer(BundleContext bundleContext) {
        _bundleContext = bundleContext;
    }

    @Override
    public ServiceTracker
        <CXFJaxRsServiceRegistrator, ?>
    addingService(ServiceReference<Object> reference) {

        String applicationSelector =
            reference.getProperty("jaxrs.application.select").toString();

        Bundle bundle = reference.getBundle();

        BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);

        ClassLoader classLoader = bundleWiring.getClassLoader();

        Object service = _bundleContext.getService(reference);

        try {
            Filter filter = _bundleContext.createFilter(
                "(&(objectClass=" + CXFJaxRsServiceRegistrator.class.getName() +
                    ")" + applicationSelector + ")");

            ServiceTracker
                <CXFJaxRsServiceRegistrator, ?>
                serviceTracker = new ServiceTracker<>(
                    _bundleContext, filter,
                    new AddonsServiceTrackerCustomizer(
                        _bundleContext, classLoader, service));

            serviceTracker.open();

            return serviceTracker;
        }
        catch (InvalidSyntaxException ise) {
            _bundleContext.ungetService(reference);

            throw new RuntimeException(ise);
        }
    }

    @Override
    public void modifiedService(
        ServiceReference<Object> reference,
        ServiceTracker<CXFJaxRsServiceRegistrator, ?> serviceTracker) {

        removedService(reference, serviceTracker);

        addingService(reference);
    }

    @Override
    public void removedService(
        ServiceReference<Object> reference,
        ServiceTracker<CXFJaxRsServiceRegistrator, ?> serviceTracker) {

        serviceTracker.close();

        _bundleContext.ungetService(reference);
    }

}
