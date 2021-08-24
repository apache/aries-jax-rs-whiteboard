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
package org.apache.aries.jax.rs.rest.management.schema;

import java.util.Map;

import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.osgi.framework.dto.ServiceReferenceDTO;

@XmlRootElement(name = "service")
@XmlJavaTypeAdapter(ServiceSchemaAdapter.class)
public class ServiceSchema extends ServiceReferenceDTO {

    public String bundle;
    public String[] usingBundles;

    public static ServiceSchema build(
        String bundle, long id, Map<String, Object> properties, String[] usingBundles) {
        final ServiceSchema serviceSchema = new ServiceSchema();
        serviceSchema.bundle = bundle;
        serviceSchema.id = id;
        serviceSchema.properties = properties;
        serviceSchema.usingBundles = usingBundles;
        return serviceSchema;
    }

    public static ServiceSchema build(UriInfo uriInfo, ServiceReferenceDTO serviceReferenceDTO) {
        final ServiceSchema serviceSchema = new ServiceSchema();
        serviceSchema.bundle = uriInfo.getBaseUriBuilder().path("framework").path("bundle").path("{id}").build(serviceReferenceDTO.bundle).toASCIIString();
        serviceSchema.id = serviceReferenceDTO.id;
        serviceSchema.properties = serviceReferenceDTO.properties;
        serviceSchema.usingBundles = new String[serviceReferenceDTO.usingBundles.length];
        for (int i = 0; i < serviceReferenceDTO.usingBundles.length; i++) {
            serviceSchema.usingBundles[i] = uriInfo.getBaseUriBuilder().path("framework").path("bundle").path("{id}").build(serviceReferenceDTO.usingBundles[i]).toASCIIString();
        }
        return serviceSchema;
    }

}
