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

import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.OpenAPI;
import org.apache.aries.component.dsl.CachingServiceReference;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Carlos Sierra Andrés
 */
class OpenAPIWithModelResolvers {
    private final CachingServiceReference<OpenAPI> openAPI;
    private final Set<CachingServiceReference<ModelConverter>> modelConverters;

    OpenAPIWithModelResolvers(
        CachingServiceReference<OpenAPI> openAPI,
        Set<CachingServiceReference<ModelConverter>> modelConverters) {

        this.openAPI = openAPI;
        this.modelConverters = modelConverters;
    }

    public Set<CachingServiceReference<ModelConverter>> getModelConverters() {
        return modelConverters;
    }

    public CachingServiceReference<OpenAPI> getOpenAPIServiceReference() {
        return openAPI;
    }
}
