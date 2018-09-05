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
package org.apache.aries.jax.rs.shiro.authc.impl;

import static java.lang.Boolean.TRUE;
import static org.apache.aries.component.dsl.OSGi.coalesce;
import static org.apache.aries.component.dsl.OSGi.configuration;
import static org.apache.aries.component.dsl.OSGi.just;
import static org.apache.aries.component.dsl.OSGi.register;
import static org.apache.aries.component.dsl.OSGi.service;
import static org.apache.aries.component.dsl.OSGi.serviceReferences;
import static org.apache.aries.component.dsl.Utils.accumulate;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_EXTENSION;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_NAME;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Feature;

import org.apache.aries.component.dsl.OSGi;
import org.apache.aries.component.dsl.OSGiResult;
import org.apache.shiro.realm.Realm;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShiroAuthenticationActivator implements BundleActivator {

    private static final Logger _LOG = LoggerFactory.getLogger(
            ShiroAuthenticationActivator.class);
    
    private OSGiResult registration;

    @Override
    public void start(BundleContext context) throws Exception {
        _LOG.debug("Starting the Shiro JAX-RS Authentication Feature");
        
        registration = coalesce(
                configuration("org.apache.aries.jax.rs.shiro.authentication"),
                just(() -> {
                    
                    _LOG.debug("Using the default configuration for the Shiro JAX-RS Authentication Feature");
                    
                    Dictionary<String, Object> properties =
                        new Hashtable<>();

                    properties.put(
                        Constants.SERVICE_PID,
                        "org.apache.aries.jax.rs.shiro.authentication");

                    return properties;
                })
        ).map(this::filter)
         .flatMap(this::setupRealms)
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
        serviceProps.putIfAbsent(JAX_RS_NAME, "aries.shiro.authc");
        
        _LOG.debug("Shiro JAX-RS Authentication Feature service properties are: {}", serviceProps);
        
        return serviceProps;
    }
    
    OSGi<?> setupRealms(Map<String, Object> properties) {
        
        Object filter = properties.get("realms.target");
        
        OSGi<List<Realm>> realms;
        if(filter == null) {
            _LOG.debug("The Shiro JAX-RS Authentication Feature is accepting all realms");
            realms = accumulate(service(serviceReferences(Realm.class)));
        } else {
            _LOG.debug("The Shiro JAX-RS Authentication Feature is filtering realms using the filter {}", filter);
            realms = accumulate(service(serviceReferences(Realm.class, String.valueOf(filter))));
        }
        
        return realms.map(ShiroAuthenticationFeatureProvider::new)
                .flatMap(f -> register(Feature.class, f,  properties)
                        .effects(x -> {}, x -> f.close()));
    }
    
    @Override
    public void stop(BundleContext context) throws Exception {
        _LOG.debug("Stopping the Shiro JAX-RS Authentication Feature");
       registration.close();
    }
}
