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

package test.types;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresGuest;
import org.apache.shiro.authz.annotation.RequiresRoles;

@Path("/test")
public class TestShiroAnnotations {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/guest")
    @RequiresGuest
    public String guest() {
        return "Welcome Guest";
    }
    
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/authenticated")
    @RequiresAuthentication
    public String authenticated() {
        return "Welcome " + SecurityUtils.getSubject().getPrincipal().toString();
    }
    

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/admin")
    @RequiresRoles("admin")
    public String admin() {
        return "Welcome Admin";
    }
    
}
