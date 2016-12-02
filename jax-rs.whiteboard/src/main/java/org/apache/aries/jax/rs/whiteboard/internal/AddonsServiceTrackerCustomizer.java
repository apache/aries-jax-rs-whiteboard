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

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class AddonsServiceTrackerCustomizer
    implements
        ServiceTrackerCustomizer<CXFJaxRsServiceRegistrator, CXFJaxRsServiceRegistrator> {

    private final BundleContext _bundleContext;
    private final ClassLoader _classLoader;
    private final Object _service;

    public AddonsServiceTrackerCustomizer(
        BundleContext bundleContext, ClassLoader classLoader,
        Object service) {

        _bundleContext = bundleContext;
        _classLoader = classLoader;
        _service = service;
    }

    @Override
    public CXFJaxRsServiceRegistrator addingService(ServiceReference<CXFJaxRsServiceRegistrator> reference) {
        CXFJaxRsServiceRegistrator registrator = _bundleContext.getService(reference);
        try {
            runInClassLoader(_classLoader, () -> registrator.add(_service));
            return registrator;
        }
        catch (Exception e) {
            _bundleContext.ungetService(reference);
            throw e;
        }
    }

    @Override
    public void modifiedService(
        ServiceReference<CXFJaxRsServiceRegistrator> reference,
        CXFJaxRsServiceRegistrator cxfJaxRsServiceRegistrator) {

        removedService(reference, cxfJaxRsServiceRegistrator);
        addingService(reference);
    }

    @Override
    public void removedService(
        ServiceReference<CXFJaxRsServiceRegistrator> reference,
        CXFJaxRsServiceRegistrator cxfJaxRsServiceRegistrator) {

        cxfJaxRsServiceRegistrator.remove(_service);
        _bundleContext.ungetService(reference);
    }
    
    private void runInClassLoader(ClassLoader cl, Runnable runable) {
        Thread thread = Thread.currentThread();
        ClassLoader contextClassLoader = thread.getContextClassLoader();
        try {
            thread.setContextClassLoader(_classLoader);
            runable.run();
        }
        finally {
            thread.setContextClassLoader(contextClassLoader);
        }
    }
    
}
