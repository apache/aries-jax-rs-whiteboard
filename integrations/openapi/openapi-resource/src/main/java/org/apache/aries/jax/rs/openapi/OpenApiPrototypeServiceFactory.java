/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 * <p>
 * The contents of this file are subject to the terms of the Liferay Enterprise
 * Subscription License ("License"). You may not use this file except in
 * compliance with the License. You can obtain a copy of the License by
 * contacting Liferay, Inc. See the License for the specific language governing
 * permissions and limitations under the License, including but not limited to
 * distribution rights of the Software.
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
