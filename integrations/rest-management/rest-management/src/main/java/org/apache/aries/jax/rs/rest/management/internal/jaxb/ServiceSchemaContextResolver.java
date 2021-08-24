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
package org.apache.aries.jax.rs.rest.management.internal.jaxb;

import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_SERVICES_REPRESENTATIONS_JSON;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_SERVICES_REPRESENTATIONS_XML;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_SERVICE_JSON;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_SERVICE_XML;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.aries.jax.rs.rest.management.schema.ServiceSchema;
import org.apache.aries.jax.rs.rest.management.schema.ServiceSchemaListSchema;

@Provider
@Produces({
    APPLICATION_SERVICE_JSON,
    APPLICATION_SERVICE_XML,
    APPLICATION_SERVICES_REPRESENTATIONS_JSON,
    APPLICATION_SERVICES_REPRESENTATIONS_XML
})
public class ServiceSchemaContextResolver implements ContextResolver<JAXBContext> {

    private JAXBContext ctx;

    public ServiceSchemaContextResolver() {
        try {
            this.ctx = JAXBContext.newInstance(
                ArrayList.class,
                AtomicLong.class,
                Response.class,
                ServiceSchema.class,
                ServiceSchemaListSchema.class,
                String[].class
            );
        }
        catch (JAXBException ex) {
            throw new RuntimeException(ex);
        }
    }

    public JAXBContext getContext(Class<?> type) {
        return (
            type.equals(ServiceSchema.class) ||
            type.equals(ServiceSchemaListSchema.class)? ctx : null
        );
    }

}
