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
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.aries.jax.rs.whiteboard.internal.CXFJaxRsServiceRegistrator;
import org.apache.aries.jax.rs.whiteboard.internal.ClientBuilderFactory;
import org.apache.aries.jax.rs.whiteboard.internal.DefaultApplication;
import org.apache.aries.jax.rs.whiteboard.internal.DefaultWeb;
import org.apache.aries.osgi.functional.OSGi;
import org.apache.aries.osgi.functional.OSGiResult;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.CXFBusFactory;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import static java.lang.String.format;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.*;
import static org.apache.aries.osgi.functional.OSGi.just;
import static org.apache.aries.osgi.functional.OSGi.serviceReferences;
import static org.apache.aries.osgi.functional.OSGi.services;
import static org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants.*;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN;
import static org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants.JAX_RS_APPLICATION_SELECT;

public class CXFJaxRsBundleActivator implements BundleActivator {

    private static final Logger _log = LoggerFactory.getLogger(CXFJaxRsBundleActivator.class);

    private OSGiResult<?> _applicationsResult;
    private OSGiResult<?> _applicationSingletonsResult;
    private BundleContext _bundleContext;
    private Bus _bus;
    private ServiceRegistration<ClientBuilder> _clientBuilder;
    private ServiceRegistration<DefaultWeb> _defaultWeb;
    private OSGiResult<?> _extensionsResult;
    private OSGiResult<?> _singletonsResult;

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        _bundleContext = bundleContext;

        if (_log.isDebugEnabled()) {
            _log.debug("Beginning initialization");
        }

        initRuntimeDelegate(bundleContext);

        // TODO make the context path of the JAX-RS Whiteboard configurable.
        _bus = BusFactory.newInstance(
            CXFBusFactory.class.getName()).createBus();
        registerCXFServletService(_bus);

        OSGi<?> applications =
            repeatInOrder(
                serviceReferences(Application.class, getApplicationFilter())).
            flatMap(ref ->
            just(
                CXFJaxRsServiceRegistrator.getProperties(
                    ref, JAX_RS_APPLICATION_BASE)).
                flatMap(properties ->
            service(ref).flatMap(application ->
            cxfRegistrator(_bus, application, properties)
        )));

        _applicationsResult = applications.run(bundleContext);

        OSGi<?> applicationSingletons =
            serviceReferences(format("(%s=*)", JAX_RS_APPLICATION_SELECT)).
                flatMap(ref ->
            just(ref.getProperty(JAX_RS_APPLICATION_SELECT).toString()).
                flatMap(applicationFilter ->
            services(CXFJaxRsServiceRegistrator.class, applicationFilter).
                flatMap(registrator ->
            safeRegisterGeneric(ref, registrator)
        )));

        _applicationSingletonsResult = applicationSingletons.run(bundleContext);

        Map<String, Object> properties = new HashMap<>();
        properties.put(JAX_RS_APPLICATION_BASE, "/");
        properties.put(JAX_RS_NAME, ".default");

        CXFJaxRsServiceRegistrator defaultServiceRegistrator =
            new CXFJaxRsServiceRegistrator(
                _bus, new DefaultApplication(), properties);

        OSGi<?> extensions =
            serviceReferences(getExtensionFilter()).flatMap(ref ->
            waitForExtensionDependencies(ref,
                safeRegisterExtension(ref, defaultServiceRegistrator)
            )
        );

        _extensionsResult = extensions.run(bundleContext);

        OSGi<?> singletons =
            serviceReferences(getSingletonsFilter()).
                flatMap(serviceReference ->
            waitForExtensionDependencies(serviceReference,
                safeRegisterEndpoint(
                    serviceReference, defaultServiceRegistrator)
            )
        );

        _singletonsResult = singletons.run(bundleContext);

        Dictionary<String, Object> dictionary = new Hashtable<>();
        dictionary.put(JAX_RS_APPLICATION_SELECT, "(osgi.jaxrs.name=.default)");
        dictionary.put(JAX_RS_RESOURCE, "true");
        dictionary.put(Constants.SERVICE_RANKING, -1);

        _defaultWeb =  _bundleContext.registerService(
            DefaultWeb.class, new DefaultWeb(), dictionary);
        _clientBuilder = _bundleContext.registerService(
            ClientBuilder.class, new ClientBuilderFactory(), null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        _clientBuilder.unregister();
        _defaultWeb.unregister();
        _applicationsResult.close();
        _applicationSingletonsResult.close();
        _extensionsResult.close();
        _singletonsResult.close();
    }

    private static String buildExtensionFilter(String filter) {
        return String.format("(&%s%s)", getExtensionFilter(), filter);
    }

    private static String[] canonicalize(Object propertyValue) {
        if (propertyValue == null) {
            return new String[0];
        }
        if (propertyValue instanceof String[]) {
            return (String[]) propertyValue;
        }
        return new String[]{propertyValue.toString()};
    }

    private static CXFNonSpringServlet createCXFServlet(Bus bus) {
        CXFNonSpringServlet cxfNonSpringServlet = new CXFNonSpringServlet();
        cxfNonSpringServlet.setBus(bus);
        return cxfNonSpringServlet;
    }

    private static String getApplicationFilter() {
        return format("(%s=*)", JAX_RS_APPLICATION_BASE);
    }

    private static String getExtensionFilter() {
        return format("(%s=*)", JAX_RS_EXTENSION);
    }

    private static String getSingletonsFilter() {
        return format("(%s=true)", JAX_RS_RESOURCE);
    }

    /**
     * Initialize instance so it is never looked up again
     * @param bundleContext
     */
    private void initRuntimeDelegate(BundleContext bundleContext) {
        RuntimeDelegate.setInstance(new org.apache.cxf.jaxrs.impl.RuntimeDelegateImpl());
    }

    private ServiceRegistration<Servlet> registerCXFServletService(Bus bus) {
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(HTTP_WHITEBOARD_CONTEXT_SELECT,
            format("(%s=%s)", HTTP_WHITEBOARD_CONTEXT_NAME,
                HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME));
        properties.put(HTTP_WHITEBOARD_SERVLET_PATTERN, "/*");
        properties.put(Constants.SERVICE_RANKING, -1);
        CXFNonSpringServlet cxfNonSpringServlet = createCXFServlet(bus);
        return _bundleContext.registerService(
            Servlet.class, cxfNonSpringServlet, properties);
    }

    private static OSGi<?> waitForExtensionDependencies(
        ServiceReference<?> serviceReference, OSGi<?> program) {

        String[] extensionDependencies = canonicalize(
            serviceReference.getProperty(JAX_RS_EXTENSION_SELECT));

        for (String extensionDependency : extensionDependencies) {
            program =
                serviceReferences(buildExtensionFilter(extensionDependency)).
                then(program);
        }

        return program;
    }

}
