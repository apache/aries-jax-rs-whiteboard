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

import static javax.ws.rs.core.HttpHeaders.SET_COOKIE;
import static org.apache.aries.jax.rs.shiro.authc.impl.ShiroAuthenticationFeature.SESSION_COOKIE_NAME;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Filter is used to:
 * 
 * <ul>
 *   <li>Associate a SecurityManager with the request</li>
 *   <li>Set up the current user if a cookie is detected</li>
 *   <li>Remove the user cookie after the request if the user is logged out</li>
 * </ul>
 */
@PreMatching
public class SecurityManagerAssociatingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger _LOG = LoggerFactory.getLogger(
            SecurityManagerAssociatingFilter.class);
    
    private final SecurityManager manager;
    
    public SecurityManagerAssociatingFilter(SecurityManager manager) {
        this.manager = manager;
    }

    /**
     * Set up the incoming request context
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        
        _LOG.debug("Establishing Shiro Security Context");
        
        // Bind the security manager
        ThreadContext.bind(manager);
        
        Cookie cookie = requestContext.getCookies().get(SESSION_COOKIE_NAME);
        
        // If we have a session cookie then use it to prime the session value
        if(cookie != null) {
            _LOG.debug("Found a Shiro Security Context cookie: {}. Establishing user context", cookie);
            
            _LOG.debug("Establishing user context:");
            Subject subject = new Subject.Builder(manager).sessionId(cookie.getValue()).buildSubject();
            ThreadContext.bind(subject);
            if(_LOG.isDebugEnabled()) {
                _LOG.debug("Established user context for: {}", subject.getPrincipal());
            }
        }
        
        UriInfo info = requestContext.getUriInfo();
        
        if("security/authenticate".equals(info.getPath())) {
            requestContext.abortWith(authenticate(info, requestContext.getHeaderString("user"), requestContext.getHeaderString("password")));
        } else if("security/logout".equals(info.getPath())) {
            logout();
        }
    }
    
    /**
     * Clean up after the request
     */
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        _LOG.debug("Cleaning up the Shiro Security Context");
        Subject subject = ThreadContext.getSubject();
        ThreadContext.unbindSecurityManager();
        ThreadContext.unbindSubject();
        
        if(subject != null && !subject.isAuthenticated()) {
            // Not authenticated. Check for incoming session cookie
            Cookie cookie = requestContext.getCookies().get(SESSION_COOKIE_NAME);
            
            // If we have a session cookie then it should be deleted
            if(cookie != null) {
                _LOG.debug("The subject associated with this request is not authenticated, removing the session cookie");
                responseContext.getHeaders().add(SET_COOKIE, getDeletionCookie(requestContext));
            }
        }
        
    }

    private NewCookie getDeletionCookie(ContainerRequestContext requestContext) {
        return new NewCookie(SESSION_COOKIE_NAME, "deleteMe", 
                        requestContext.getUriInfo().getBaseUri().getPath(), null, -1, null, -1, null, false, true);
    }
    
    private Response authenticate(UriInfo info, String user, String password) {
        
        _LOG.debug("Received a login request for user {}", user);
        
        Subject currentUser = SecurityUtils.getSubject();
        
        ResponseBuilder rb;
        
        if (!currentUser.isAuthenticated()) {
            _LOG.debug("Authenticating user {}", user);
            UsernamePasswordToken token = new UsernamePasswordToken(user, password);
            token.setRememberMe(true);
            currentUser.login(token);
            
            rb = Response.ok()
                    .cookie(new NewCookie(SESSION_COOKIE_NAME, currentUser.getSession().getId().toString(), 
                            info.getBaseUri().getPath(), null, -1, null, -1, null, false, true));
        } else {
            _LOG.debug("The login request for user {} was already authenticated as user {}", user, currentUser.getPrincipal());
            rb = Response.status(Status.CONFLICT);
        }
        return rb.build();
    }

    private void logout() {
        _LOG.debug("Received a logout request");
        Subject currentUser = SecurityUtils.getSubject();
        
        if (currentUser.isAuthenticated()) {
            _LOG.debug("Logging out user {}", currentUser.getPrincipal());
            currentUser.logout();
        } 
    }
}
