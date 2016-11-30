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

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.CXFBusFactory;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;

import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN;

import java.util.Dictionary;
import java.util.Hashtable;

public class ServicesRegistrator
    implements ServiceTrackerCustomizer<ServletContextHelper, Object> {

    private static final Logger _logger = LoggerFactory.getLogger(ServicesRegistrator.class);

    private final BundleContext _bundleContext;
    private ServiceRegistration<Bus> _busServiceRegistration;
    private ServiceTracker<ServletContextHelper, Object> _serviceTracker;
    private ServiceRegistration<Servlet> _servletServiceRegistration;

    public ServicesRegistrator(BundleContext bundleContext) {
        _bundleContext = bundleContext;
    }

    @Override
    public Object addingService(ServiceReference<ServletContextHelper> reference) {
        Bus bus = BusFactory.newInstance(CXFBusFactory.class.getName()).createBus();
        _servletServiceRegistration = createCXFServletService(bus);
        _busServiceRegistration = createBusService(reference, bus);
        return new Object();
    }

    private ServiceRegistration<Servlet> createCXFServletService(Bus bus) {
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(HTTP_WHITEBOARD_CONTEXT_SELECT,
            "(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=" + HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME + ")");
        properties.put(HTTP_WHITEBOARD_SERVLET_PATTERN, "/*");
        properties.put(Constants.SERVICE_RANKING, -1);
        
        CXFNonSpringServlet cxfNonSpringServlet = new CXFNonSpringServlet();
        cxfNonSpringServlet.setBus(bus);
        return _bundleContext.registerService(Servlet.class, cxfNonSpringServlet, properties);
    }

    private ServiceRegistration<Bus> createBusService(ServiceReference<ServletContextHelper> reference, Bus bus) {
        Dictionary<String, Object> properties = new Hashtable<>();
        String contextPath = (String)reference.getProperty(HTTP_WHITEBOARD_CONTEXT_PATH);
        properties.put(HTTP_WHITEBOARD_CONTEXT_PATH, contextPath);
        return _bundleContext.registerService(Bus.class, bus, properties);
    }

    @Override
    public void modifiedService(ServiceReference<ServletContextHelper> reference, Object object) {        
    }

    @Override
    public void removedService(ServiceReference<ServletContextHelper> reference, Object object) {
        unregister(_busServiceRegistration);
        unregister(_servletServiceRegistration);
    }

    private void unregister(ServiceRegistration<?> reg) {
        try {
            reg.unregister();
        }
        catch (Exception e) {
            _logger.warn("Unable to unregister CXF bus service registration " + reg);
        }
    }

    public void start() throws InvalidSyntaxException {
        String filterS = String.format("(&(objectClass=%s)(%s=%s))",
                                       ServletContextHelper.class.getName(),
                                       HTTP_WHITEBOARD_CONTEXT_NAME, 
                                       HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME);
        Filter filter = _bundleContext.createFilter(filterS);
        _serviceTracker = new ServiceTracker<>(_bundleContext, filter, this);
        _serviceTracker.open();
    }

    public void stop() {
        _serviceTracker.close();
    }

}
