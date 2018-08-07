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

import static javax.ws.rs.Priorities.AUTHORIZATION;
import static javax.ws.rs.Priorities.USER;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SERVICE_PROPERTIES;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_NAME;

import java.util.Map;

import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.apache.shiro.web.jaxrs.ExceptionMapper;
import org.apache.shiro.web.jaxrs.ShiroAnnotationFilterFeature;
import org.apache.shiro.web.jaxrs.ShiroFeature;
import org.apache.shiro.web.jaxrs.SubjectPrincipalRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This type mirrors {@link ShiroFeature}, by registering an {@link ExceptionMapper},
 * {@link SubjectPrincipalRequestFilter} and {@link ShiroAnnotationFilterFeature}.
 * 
 * <p>We cannot use the {@link ShiroFeature} directly because several of the extension 
 * types it registers are also used to enable authentication, and it is not allowed to 
 * register the same extension twice. Also the ShiroFeature does not make correct use
 * of priorities when registering. This Feature therefore:
 * 
 * <ul>
 *   <li>Avoids duplicate registrations with extension types that are also used in authentication.</li>
 *   <li>Uses priorities to indicate that these are authorization extensions</li>
 * </ul>
 *
 */
public class ShiroAuthorizationFeature implements Feature {

    private static final Logger _LOG = LoggerFactory.getLogger(
            ShiroAuthorizationFeature.class);
    
    @Override
    public boolean configure(FeatureContext fc) {

        Configuration configuration = fc.getConfiguration();
        
        if(_LOG.isInfoEnabled()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> applicationProps = (Map<String, Object>) configuration.getProperty(JAX_RS_APPLICATION_SERVICE_PROPERTIES);
            _LOG.info("Registering the Shiro Authorization feature with application {}", 
                    applicationProps.getOrDefault(JAX_RS_NAME, "<No Name found in application configuration>"));
        }
        
        Map<Class<?>, Integer> contracts = configuration.getContracts(ExceptionMapper.class);
        if(contracts.isEmpty()) {
            _LOG.debug("Registering the Shiro ExceptionMapper");
            // Only register the ExceptionMapper if it isn't already registered
            fc.register(ExceptionMapper.class, AUTHORIZATION);
        } else if(AUTHORIZATION < contracts.getOrDefault(javax.ws.rs.ext.ExceptionMapper.class, USER)) {
            _LOG.debug("Updating the priority of the Shiro ExceptionMapper from {} to {}",
                    contracts.getOrDefault(javax.ws.rs.ext.ExceptionMapper.class, USER),
                    AUTHORIZATION);
            // Update the priority if it's registered too low
            contracts.put(javax.ws.rs.ext.ExceptionMapper.class, AUTHORIZATION);
        }

        contracts = configuration.getContracts(SubjectPrincipalRequestFilter.class);
        if(contracts.isEmpty()) {
            _LOG.debug("Registering the Shiro SubjectPrincipalRequestFilter");
            // Only register the SubjectPrincipalRequestFilter if it isn't already registered
            fc.register(SubjectPrincipalRequestFilter.class, AUTHORIZATION);
        } else if(AUTHORIZATION < contracts.getOrDefault(ContainerRequestFilter.class, USER)) {
            _LOG.debug("Updating the priority of the Shiro SubjectPrincipalRequestFilter from {} to {}",
                    contracts.getOrDefault(ContainerRequestFilter.class, USER),
                    AUTHORIZATION);
            // Update the priority if it's registered too low
            contracts.put(ContainerRequestFilter.class, AUTHORIZATION);
        }
        
        _LOG.debug("Registering the Shiro ShiroAnnotationFilterFeature");
        fc.register(ShiroAnnotationFilterFeature.class, Priorities.AUTHORIZATION);
        return true;
    }
}