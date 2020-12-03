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

@Capability(
    name = JAX_RS_WHITEBOARD_IMPLEMENTATION,
    namespace = ImplementationNamespace.IMPLEMENTATION_NAMESPACE,
    version = JAX_RS_WHITEBOARD_SPECIFICATION_VERSION,
    uses = {
        javax.ws.rs.Path.class,
        javax.ws.rs.core.MediaType.class,
        javax.ws.rs.ext.Provider.class,
        javax.ws.rs.client.Entity.class,
        javax.ws.rs.container.PreMatching.class,
        javax.ws.rs.sse.Sse.class,
        org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.class
    }
)
@Capability(
    attribute = "objectClass:List<String>='javax.ws.rs.client.ClientBuilder'",
    namespace = ServiceNamespace.SERVICE_NAMESPACE,
    uses = {
        javax.ws.rs.client.ClientBuilder.class,
        org.osgi.service.jaxrs.client.SseEventSourceFactory.class
    }
)
@Capability(
    attribute = "objectClass:List<String>='org.osgi.service.jaxrs.client.SseEventSourceFactory'",
    namespace = ServiceNamespace.SERVICE_NAMESPACE,
    uses = {
        org.osgi.service.jaxrs.client.SseEventSourceFactory.class
    }
)
@Capability(
    attribute = "objectClass:List<String>='org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime'",
    namespace = ServiceNamespace.SERVICE_NAMESPACE,
    uses = {
        org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime.class,
        org.osgi.service.jaxrs.runtime.dto.BaseDTO.class
    }
)
@Capability(
    name = "javax.ws.rs.ext.RuntimeDelegate",
    namespace = "osgi.serviceloader",
    attribute = {
        "register:=org.apache.cxf.jaxrs.impl.RuntimeDelegateImpl",
        "service.scope=prototype"
    }
)
@Capability(
    name = "javax.ws.rs.sse.SseEventSource.Builder",
    namespace = "osgi.serviceloader",
    attribute = {
        "register:=org.apache.cxf.jaxrs.sse.client.SseEventSourceBuilderImpl",
        "service.scope=prototype"
    }
)
@Export
@Version("1.0.0")
@Requirement(
    name = "osgi.http",
    namespace = ImplementationNamespace.IMPLEMENTATION_NAMESPACE,
    version = "1.0.0"
)
@Requirement(
    name = "osgi.serviceloader.registrar",
    namespace = ExtenderNamespace.EXTENDER_NAMESPACE,
    resolution = Resolution.OPTIONAL
)
package org.apache.aries.jax.rs.whiteboard;

import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.*;

import org.osgi.annotation.bundle.Capability;
import org.osgi.annotation.bundle.Export;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.annotation.bundle.Requirement.Resolution;
import org.osgi.annotation.versioning.Version;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.namespace.implementation.ImplementationNamespace;
import org.osgi.namespace.service.ServiceNamespace;
