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

@Export
@Version("1.0.0")
@Capability(
    name = SPECIFICATION_IMPLEMENTATION,
    namespace = IMPLEMENTATION_NAMESPACE,
    version = SPECIFICATION_VERSION,
    uses = org.osgi.service.rest.RestApiExtension.class
)
@org.osgi.annotation.bundle.Capability(
    attribute = "objectClass:List<String>='org.osgi.service.rest.client.RestClientFactory'",
    namespace = SERVICE_NAMESPACE,
    uses = org.osgi.service.rest.client.RestClientFactory.class
)
package org.apache.aries.jax.rs.rest.management;

import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.SPECIFICATION_IMPLEMENTATION;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.SPECIFICATION_VERSION;
import static org.osgi.namespace.implementation.ImplementationNamespace.IMPLEMENTATION_NAMESPACE;
import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import org.osgi.annotation.bundle.Capability;
import org.osgi.annotation.bundle.Export;
import org.osgi.annotation.versioning.Version;
