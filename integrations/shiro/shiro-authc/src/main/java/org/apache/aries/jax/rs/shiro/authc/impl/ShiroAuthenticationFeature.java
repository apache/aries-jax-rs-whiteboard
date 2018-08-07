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

import static javax.ws.rs.Priorities.AUTHENTICATION;
import static javax.ws.rs.Priorities.AUTHORIZATION;
import static javax.ws.rs.Priorities.USER;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SERVICE_PROPERTIES;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_NAME;

import java.util.List;
import java.util.Map;

import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.web.jaxrs.ExceptionMapper;
import org.apache.shiro.web.jaxrs.ShiroFeature;
import org.apache.shiro.web.jaxrs.SubjectPrincipalRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This type adds support for establishing Shiro Authentication in a JAX-RS container
 * using native JAX-RS features, rather than relying on an external Servlet Container.
 * As a result some common features of the ShiroFilter are not supported, but native
 * JAX-RS containers (i.e. ones that don't support Servlets) are able to be used. 
 * 
 * <p> Where possible we reuse types from the {@link ShiroFeature}, and where these
 * overlap with types used in authorization we must register carefully, as it is not
 * allowed to register the same extension twice. Also the ShiroFeature does not make 
 * correct use of priorities when registering. This Feature therefore:
 * 
 * <ul>
 *   <li>Avoids duplicate registrations with extension types that are also used in authorization.</li>
 *   <li>Uses priorities to indicate that these are authentication extensions</li>
 * </ul>
 *
 */
public class ShiroAuthenticationFeature implements Feature {

    private static final Logger _LOG = LoggerFactory.getLogger(
            ShiroAuthenticationFeature.class);
    
    public static final String SESSION_COOKIE_NAME = "JAXRSSESSIONID";
    
    private final List<Realm> realms;
    
    private final DefaultSecurityManager manager;
    
    public ShiroAuthenticationFeature(List<Realm> realms) {
        this.realms = realms;
        this.manager = realms.isEmpty() ? new DefaultSecurityManager() : new DefaultSecurityManager(realms);
    }

    @Override
    public boolean configure(FeatureContext fc) {
        
        Configuration configuration = fc.getConfiguration();
        
        if(_LOG.isInfoEnabled()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> applicationProps = (Map<String, Object>) configuration.getProperty(JAX_RS_APPLICATION_SERVICE_PROPERTIES);
            _LOG.info("Registering the Shiro Authentication feature with application {}", 
                    applicationProps.getOrDefault(JAX_RS_NAME, "<No Name found in application configuration>"));
        }
        
        if(realms.isEmpty()) {
            _LOG.warn("There are no authentication realms available. Users may not be able to authenticate.");
        } else {
            _LOG.debug("Using the authentication realms {}.", realms);
        }

        _LOG.debug("Registering the Shiro SecurityManagerAssociatingFilter");
        fc.register(new SecurityManagerAssociatingFilter(manager), AUTHENTICATION);

        Map<Class<?>, Integer> contracts = configuration.getContracts(ExceptionMapper.class);
        if(contracts.isEmpty()) {
            _LOG.debug("Registering the Shiro ExceptionMapper");
            // Only register the ExceptionMapper if it isn't already registered
            fc.register(ExceptionMapper.class, AUTHENTICATION);
        } else if(AUTHENTICATION < contracts.getOrDefault(javax.ws.rs.ext.ExceptionMapper.class, USER)) {
            _LOG.debug("Updating the priority of the Shiro ExceptionMapper from {} to {}",
                    contracts.getOrDefault(javax.ws.rs.ext.ExceptionMapper.class, USER),
                    AUTHORIZATION);
            // Update the priority if it's registered too low
            contracts.put(javax.ws.rs.ext.ExceptionMapper.class, AUTHENTICATION);
        }

        contracts = configuration.getContracts(SubjectPrincipalRequestFilter.class);
        if(contracts.isEmpty()) {
            _LOG.debug("Registering the Shiro SubjectPrincipalRequestFilter");
            // Only register the SubjectPrincipalRequestFilter if it isn't already registered
            // and make sure it always comes after the SecurityManagerAssociatingFilter
            fc.register(SubjectPrincipalRequestFilter.class, AUTHENTICATION + 1);
        } else if(AUTHENTICATION < contracts.getOrDefault(ContainerRequestFilter.class, USER)) {
            _LOG.debug("Updating the priority of the Shiro SubjectPrincipalRequestFilter from {} to {}",
                    contracts.getOrDefault(ContainerRequestFilter.class, USER),
                    AUTHENTICATION + 1);
            // Update the priority if it's registered too low
            contracts.put(ContainerRequestFilter.class, AUTHENTICATION + 1);
        }
        
        return true;
    }

    public void close() {
        manager.destroy();
    }
}