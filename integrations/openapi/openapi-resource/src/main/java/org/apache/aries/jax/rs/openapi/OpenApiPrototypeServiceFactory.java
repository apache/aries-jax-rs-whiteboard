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

package org.apache.aries.jax.rs.openapi;

import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;

import org.osgi.framework.Bundle;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Carlos Sierra Andrés
 */
public class OpenApiPrototypeServiceFactory
    implements PrototypeServiceFactory<Object> {

    private final PropertyWrapper propertyWrapper;
    private final OpenAPI openAPI;

    public OpenApiPrototypeServiceFactory(
        PropertyWrapper propertyWrapper, OpenAPI openAPI) {

        this.propertyWrapper = propertyWrapper;
        this.openAPI = openAPI;
    }

    @Override
    public Object getService(
        Bundle bundle,
        ServiceRegistration<Object> serviceRegistration) {

        SwaggerConfiguration
                swaggerConfiguration = new SwaggerConfiguration().
                openAPI(openAPI);

        propertyWrapper.applyLong("cache.ttl", swaggerConfiguration::setCacheTTL);
        propertyWrapper.applyString("id", swaggerConfiguration::id);
        propertyWrapper.applyStringCollection("ignored.routes", swaggerConfiguration::setIgnoredRoutes);
        propertyWrapper.applyBoolean("pretty.print", swaggerConfiguration::setPrettyPrint);
        propertyWrapper.applyBoolean("read.all.resources", swaggerConfiguration::setReadAllResources);

        OpenApiResource openApiResource = new OpenApiResource();

        openApiResource.setOpenApiConfiguration(swaggerConfiguration);

        return openApiResource;
    }

    @Override
    public void ungetService(
        Bundle bundle,
        ServiceRegistration<Object> serviceRegistration,
        Object object) {

    }
}
