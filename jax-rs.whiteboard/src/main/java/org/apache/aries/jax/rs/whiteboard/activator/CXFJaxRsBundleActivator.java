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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.Executors;

import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.aries.jax.rs.whiteboard.internal.WhiteboardServiceFactory;
import org.apache.cxf.jaxrs.impl.RuntimeDelegateImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CXFJaxRsBundleActivator implements BundleActivator {

    private static final Logger _log = LoggerFactory.getLogger(CXFJaxRsBundleActivator.class);

    static {
        RuntimeDelegate.setInstance(new RuntimeDelegateImpl());
    }

    private ServiceRegistration<ManagedServiceFactory> _serviceRegistration;
    private WhiteboardServiceFactory _whiteboardServiceFactory;
    private String _defaultName;

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        if (_log.isDebugEnabled()) {
            _log.debug("Starting the whiteboard factory");
        }

        _whiteboardServiceFactory = new WhiteboardServiceFactory(bundleContext);

        _defaultName = _whiteboardServiceFactory.getName() + ".default";

        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(Constants.SERVICE_PID, _whiteboardServiceFactory.getName());

        _serviceRegistration = bundleContext.registerService(
            ManagedServiceFactory.class, _whiteboardServiceFactory, properties);

        if (_log.isDebugEnabled()) {
            _log.debug("Whiteboard factory started");
        }

        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                Dictionary<String, Object> defaultConfiguration = new Hashtable<>();

                defaultConfiguration.put(Constants.SERVICE_PID, _defaultName);

                _whiteboardServiceFactory.updated(_defaultName, defaultConfiguration);
            } catch (ConfigurationException ce) {
                _log.error("Configuration error", ce);
            }
        });
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (_log.isDebugEnabled()) {
            _log.debug("Stopping whiteboard factory");
        }

        _serviceRegistration.unregister();

        _whiteboardServiceFactory.deleted(_defaultName);

        if (_log.isDebugEnabled()) {
            _log.debug("Stopped whiteboard factory");
        }
    }

}