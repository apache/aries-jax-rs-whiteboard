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
package org.apache.aries.jax.rs.shiro.authz.impl;

import static java.lang.Boolean.TRUE;
import static org.apache.aries.component.dsl.OSGi.coalesce;
import static org.apache.aries.component.dsl.OSGi.configuration;
import static org.apache.aries.component.dsl.OSGi.just;
import static org.apache.aries.component.dsl.OSGi.register;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_EXTENSION;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

import javax.ws.rs.core.Feature;

import org.apache.aries.component.dsl.OSGiResult;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShiroAuthorizationActivator implements BundleActivator {
    
    private static final Logger _LOG = LoggerFactory.getLogger(
            ShiroAuthorizationActivator.class);

    private OSGiResult _registration;

    @Override
    public void start(BundleContext context) throws Exception {
        
        _LOG.debug("Starting the Shiro JAX-RS Authorization Feature");
        
        _registration = coalesce(
                configuration("org.apache.aries.jax.rs.shiro.authorization"),
                just(() -> {
                    
                    _LOG.debug("Using the default configuration for the Shiro JAX-RS Authorization Feature");
                    
                    Dictionary<String, Object> properties =
                        new Hashtable<>();

                    properties.put(
                        Constants.SERVICE_PID,
                        "org.apache.aries.jax.rs.shiro.authorization");

                    return properties;
                })
        ).map(this::filter)
         .flatMap(p -> register(Feature.class, new ShiroAuthorizationFeature(), p))
         .run(context);
    }

    Map<String, Object> filter(Dictionary<String, ?> props) {
        Map<String, Object> serviceProps = new Hashtable<>();
        
        Enumeration<String> keys = props.keys();
        while(keys.hasMoreElements()) {
            String key = keys.nextElement();
            if(!key.startsWith(".")) {
                serviceProps.put(key, props.get(key));
            }
        }
        
        serviceProps.put(JAX_RS_EXTENSION, TRUE);

        _LOG.debug("Shiro JAX-RS Authorization Feature service properties are: {}", serviceProps);
        return serviceProps;
    }
    
    @Override
    public void stop(BundleContext context) throws Exception {
        _LOG.debug("Stopping the Shiro JAX-RS Authorization Feature");
       _registration.close();
    }
}
