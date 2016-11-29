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

import java.util.Arrays;
import java.util.Collection;

import javax.ws.rs.core.Application;

import org.apache.cxf.Bus;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class BusServiceTrackerCustomizer
    implements ServiceTrackerCustomizer<Bus, Collection<ServiceTracker<?, ?>>> {

    private BundleContext _bundleContext;

    public BusServiceTrackerCustomizer(BundleContext bundleContext) {
        _bundleContext = bundleContext;
    }

    @Override
    public Collection<ServiceTracker<?, ?>>
    addingService(ServiceReference<Bus> serviceReference) {

        Bus bus = _bundleContext.getService(serviceReference);

        try {
            ServiceTracker<Application,?> applicationTracker =
                new ServiceTracker<>(_bundleContext, getApplicationFilter(),
                    new ApplicationServiceTrackerCustomizer(
                        _bundleContext, bus));

            applicationTracker.open();

            ServiceTracker<Object, ?> singletonsServiceTracker =
                new ServiceTracker<>(_bundleContext, getSingletonsFilter(),
                    new SingletonServiceTrackerCustomizer(_bundleContext, bus));

            singletonsServiceTracker.open();

            ServiceTracker<Object, ?> filtersAndInterceptorsServiceTracker =
                new ServiceTracker<>(_bundleContext, getFiltersFilter(),
                    new FiltersAndInterceptorsServiceTrackerCustomizer(
                        _bundleContext));

            filtersAndInterceptorsServiceTracker.open();

            return Arrays.asList(applicationTracker, singletonsServiceTracker,
                filtersAndInterceptorsServiceTracker);
        }
        catch (InvalidSyntaxException ise) {
            throw new RuntimeException(ise);
        }
        catch (Exception e) {
            _bundleContext.ungetService(serviceReference);

            throw e;
        }
    }

    private Filter getFiltersFilter() throws InvalidSyntaxException {
        return _bundleContext.createFilter("(osgi.jaxrs.filter.base=*)");
    }

    private Filter getApplicationFilter() throws InvalidSyntaxException {
        return _bundleContext.createFilter(
            "(&(objectClass=" + Application.class.getName() + ")" +
                "(osgi.jaxrs.application.base=*))");
    }

    private Filter getSingletonsFilter() throws InvalidSyntaxException {
        return _bundleContext.createFilter("(osgi.jaxrs.resource.base=*)");
    }

    @Override
    public void modifiedService(
        ServiceReference<Bus> reference,
        Collection<ServiceTracker<?, ?>> serviceTrackers) {

        removedService(reference, serviceTrackers);

        addingService(reference);
    }

    @Override
    public void removedService(
        ServiceReference<Bus> serviceReference,
        Collection<ServiceTracker<?, ?>> serviceTrackers) {

        _bundleContext.ungetService(serviceReference);

        for (ServiceTracker<?, ?> serviceTracker : serviceTrackers) {
            serviceTracker.close();
        }
    }

}
