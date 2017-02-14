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

import javax.servlet.Servlet;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.aries.jax.rs.whiteboard.internal.ApplicationServiceTrackerCustomizer;
import org.apache.aries.jax.rs.whiteboard.internal.FiltersAndInterceptorsServiceTrackerCustomizer;
import org.apache.aries.jax.rs.whiteboard.internal.SingletonServiceTrackerCustomizer;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.CXFBusFactory;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.ServiceTracker;

import java.util.Dictionary;
import java.util.Hashtable;

import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN;

public class CXFJaxRsBundleActivator implements BundleActivator {

    private BundleContext _bundleContext;
    private ServiceTracker<Application, ?> _applicationTracker;
    private ServiceTracker<Object, ?> _singletonsServiceTracker;
    private ServiceTracker<Object, ?> _filtersAndInterceptorsServiceTracker;

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        _bundleContext = bundleContext;
        initRuntimeDelegate(bundleContext);

        // TODO make the context path of the JAX-RS Whiteboard configurable.
        Bus bus = BusFactory.newInstance(
            CXFBusFactory.class.getName()).createBus();
        registerCXFServletService(bus);

        _applicationTracker = new ServiceTracker<>(
            bundleContext, getApplicationFilter(),
            new ApplicationServiceTrackerCustomizer(bundleContext, bus));
        _applicationTracker.open();

        _singletonsServiceTracker = new ServiceTracker<>(
            bundleContext, getSingletonsFilter(),
            new SingletonServiceTrackerCustomizer(bundleContext, bus));
        _singletonsServiceTracker.open();

        _filtersAndInterceptorsServiceTracker = new ServiceTracker<>(
            bundleContext, getFiltersFilter(),
            new FiltersAndInterceptorsServiceTrackerCustomizer(bundleContext));
        _filtersAndInterceptorsServiceTracker.open();
    }

    /**
     * Initialize instance so it is never looked up again
     * @param bundleContext
     */
    private void initRuntimeDelegate(BundleContext bundleContext) {
        Thread thread = Thread.currentThread();
        ClassLoader oldClassLoader = thread.getContextClassLoader();
        BundleWiring bundleWiring = bundleContext.getBundle().adapt(
            BundleWiring.class);
        thread.setContextClassLoader(bundleWiring.getClassLoader());
        try {
            RuntimeDelegate.getInstance();
        }
        finally {
            thread.setContextClassLoader(oldClassLoader);
        }
    }

    private ServiceRegistration<Servlet> registerCXFServletService(Bus bus) {
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(HTTP_WHITEBOARD_CONTEXT_SELECT,
            "(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=" + 
                HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME + ")");
        properties.put(HTTP_WHITEBOARD_SERVLET_PATTERN, "/*");
        properties.put(Constants.SERVICE_RANKING, -1);
        CXFNonSpringServlet cxfNonSpringServlet = createCXFServlet(bus);
        return _bundleContext.registerService(
            Servlet.class, cxfNonSpringServlet, properties);
    }

    private CXFNonSpringServlet createCXFServlet(Bus bus) {
        CXFNonSpringServlet cxfNonSpringServlet = new CXFNonSpringServlet();
        cxfNonSpringServlet.setBus(bus);
        return cxfNonSpringServlet;
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
    public void stop(BundleContext context) throws Exception {
        _applicationTracker.close();
        _filtersAndInterceptorsServiceTracker.close();
        _singletonsServiceTracker.close();
    }

}
