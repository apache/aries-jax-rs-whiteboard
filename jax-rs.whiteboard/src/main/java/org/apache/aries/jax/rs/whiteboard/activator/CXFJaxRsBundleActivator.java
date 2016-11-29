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

package org.apache.aries.jax.rs.whiteboard.activator;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.cxf.Bus;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.ServiceTracker;

import org.apache.aries.jax.rs.whiteboard.internal.BusServiceTrackerCustomizer;
import org.apache.aries.jax.rs.whiteboard.internal.ServicesServiceTrackerCustomizer;

public class CXFJaxRsBundleActivator implements BundleActivator {

    private ServiceTracker<?, ?> _busServiceTracker;
    private ServiceTracker<?, ?> _singletonsTracker;
    private ServicesRegistrator _servicesRegistrator;

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        Thread thread = Thread.currentThread();

        ClassLoader contextClassLoader = thread.getContextClassLoader();

        Bundle bundle = bundleContext.getBundle();

        BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);

        thread.setContextClassLoader(bundleWiring.getClassLoader());

        try {

            // Initialize instance so it is never looked up again

            RuntimeDelegate.getInstance();
        }
        finally {
            thread.setContextClassLoader(contextClassLoader);
        }

        Dictionary<String, Object> runtimeProperties = new Hashtable<>();

        runtimeProperties.put("endpoints", new ArrayList<String>());

        // TODO make the context path of the JAX-RS Whiteboard configurable.

        _servicesRegistrator = new ServicesRegistrator(bundleContext);

        _servicesRegistrator.start();

        _busServiceTracker = new ServiceTracker<>(
            bundleContext, Bus.class,
            new BusServiceTrackerCustomizer(bundleContext));

        _busServiceTracker.open();

        Filter filter = bundleContext.createFilter(
            "(jaxrs.application.select=*)");

        _singletonsTracker = new ServiceTracker<>(
            bundleContext, filter,
            new ServicesServiceTrackerCustomizer(bundleContext));

        _singletonsTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        _busServiceTracker.close();

        _singletonsTracker.close();

        _servicesRegistrator.stop();
    }

}
