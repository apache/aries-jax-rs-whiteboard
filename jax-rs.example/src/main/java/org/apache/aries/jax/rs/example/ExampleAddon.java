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

package org.apache.aries.jax.rs.example;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

@Component(
    property = {
        JaxRSWhiteboardConstants.JAX_RS_APPLICATION_SELECT + "=(osgi.jaxrs.name=.default)",
        JaxRSWhiteboardConstants.JAX_RS_RESOURCE + "=true"
    },
    service = ExampleAddon.class
)
public class ExampleAddon {

    @GET
    @Path("/{name}")
    public String sayHello(@PathParam("name") String name) {
        if (_log.isDebugEnabled()) {
            _log.debug("URI: " + _uriInfo.getAbsolutePath());
        }

        return "Hello " + name;
    }

    @Context
    UriInfo _uriInfo;

    private static final Logger _log = LoggerFactory.getLogger(
        ExampleAddon.class);

}