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

package org.apache.aries.jax.rs.jaxb.json.activator;

import org.apache.cxf.jaxrs.provider.json.JSONProvider;
import org.apache.cxf.jaxrs.provider.jsrjsonp.JsrJsonpProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import java.util.Hashtable;

public class JaxbJsonBundleActivator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        _serviceRegistration = context.registerService(
            new String[]{
                MessageBodyReader.class.getName(),
                MessageBodyWriter.class.getName()
            },
            new PrototypeServiceFactory<JSONProvider<?>>() {

                @Override
                public JSONProvider<?> getService(
                    Bundle bundle,
                    ServiceRegistration<JSONProvider<?>> registration) {

                    return new JSONProvider<>();
                }

                @Override
                public void ungetService(
                    Bundle bundle,
                    ServiceRegistration<JSONProvider<?>> registration,
                    JSONProvider<?> service) {

                }
            },
            new Hashtable<String, Object>() {{
                put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, true);
                put(
                    JaxrsWhiteboardConstants.JAX_RS_NAME, "jaxb-json");
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
