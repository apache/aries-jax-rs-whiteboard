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

import io.swagger.v3.oas.models.security.SecurityScheme;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxrs.openapi.OpenApiCustomizer;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;
import org.osgi.framework.Bundle;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceRegistration;

import java.util.Dictionary;
import java.util.Map;

/**
 * @author Carlos Sierra Andrés
 */
public class OpenApiPrototypeServiceFactory
    implements PrototypeServiceFactory<Feature> {

    private final PropertyWrapper propertyWrapper;
    private OpenApiCustomizer openApiCustomizer;
    private Map<String, SecurityScheme> securitySchemeMap;

    public OpenApiPrototypeServiceFactory(Dictionary<String, ?> properties) {
        propertyWrapper = new PropertyWrapper(properties);
    }

    public void setOpenApiCustomizer(OpenApiCustomizer openApiCustomizer) {
        this.openApiCustomizer = openApiCustomizer;
    }

    public void setSecurityDefinitions(Map<String, SecurityScheme> securitySchemeMap) {
        this.securitySchemeMap = securitySchemeMap;
    }

    @Override
    public OpenApiFeature getService(
        Bundle bundle,
        ServiceRegistration<Feature> serviceRegistration) {

        OpenApiFeature openApiFeature = new OpenApiFeature();
        propertyWrapper.applyString("contact.email", openApiFeature::setContactEmail);
        propertyWrapper.applyString("contact.name", openApiFeature::setContactName);
        propertyWrapper.applyString("contact.url", openApiFeature::setContactUrl);
        propertyWrapper.applyString("description", openApiFeature::setDescription);
        propertyWrapper.applyStringCollection("ignored.routes", openApiFeature::setIgnoredRoutes);
        propertyWrapper.applyString("license", openApiFeature::setLicense);
        propertyWrapper.applyString("license.url", openApiFeature::setLicenseUrl);
        propertyWrapper.applyBoolean("pretty.print", openApiFeature::setPrettyPrint);
        propertyWrapper.applyBoolean("read.all.resources", openApiFeature::setReadAllResources);
        propertyWrapper.applyStringSet("resource.classes", openApiFeature::setResourceClasses);
        propertyWrapper.applyStringSet("resource.packages", openApiFeature::setResourcePackages);
        propertyWrapper.applyBoolean("run.as.filter", openApiFeature::setRunAsFilter);
        propertyWrapper.applyBoolean("scan", openApiFeature::setScan);
        propertyWrapper.applyBoolean("scan.known.config.locations", openApiFeature::setScanKnownConfigLocations);
        propertyWrapper.applyBoolean("support.swagger.ui", openApiFeature::setSupportSwaggerUi);


        if (openApiCustomizer != null) {
            openApiFeature.setCustomizer(openApiCustomizer);
        }
        if (securitySchemeMap != null) {
            openApiFeature.setSecurityDefinitions(securitySchemeMap);
        }

        return openApiFeature;
    }

    @Override
    public void ungetService(
        Bundle bundle,
        ServiceRegistration<Feature> serviceRegistration,
        Feature feature) {

    }
}
