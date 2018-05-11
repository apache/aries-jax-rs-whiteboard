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

package org.apache.aries.jax.rs.jsonp.activator;

import org.apache.johnzon.jaxrs.JsrProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import java.util.Hashtable;

public class JsonpBundleActivator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        _serviceRegistration = context.registerService(
            new String[]{
                MessageBodyReader.class.getName(),
                MessageBodyWriter.class.getName()
            },
            new PrototypeServiceFactory<JsrProvider>() {
                @Override
                public JsrProvider getService(
                    Bundle bundle,
                    ServiceRegistration<JsrProvider> registration) {

                    Thread thread = Thread.currentThread();

                    ClassLoader contextClassLoader =
                        thread.getContextClassLoader();

                    thread.setContextClassLoader(
                        context.
                            getBundle().
                            adapt(BundleWiring.class).
                            getClassLoader()
                    );

                    try {
                        return new JsrProvider();
                    }
                    finally {
                        thread.setContextClassLoader(contextClassLoader);
                    }
                }

                @Override
                public void ungetService(
                    Bundle bundle,
                    ServiceRegistration<JsrProvider> registration,
                    JsrProvider service) {

                }
            },
            new Hashtable<String, Object>() {{
                put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, true);
                put(
                    JaxrsWhiteboardConstants.JAX_RS_NAME, "JSON-P");
                put(Constants.SERVICE_RANKING, Integer.MIN_VALUE);
            }});
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        try {
            _serviceRegistration.unregister();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ServiceRegistration<?> _serviceRegistration;
}
