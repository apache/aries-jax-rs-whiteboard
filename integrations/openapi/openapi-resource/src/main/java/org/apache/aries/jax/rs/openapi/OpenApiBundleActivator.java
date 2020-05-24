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

import io.swagger.v3.oas.models.OpenAPI;
import org.apache.aries.component.dsl.CachingServiceReference;
import org.apache.aries.component.dsl.OSGi;
import org.apache.aries.component.dsl.OSGiResult;
import org.osgi.annotation.bundle.Header;
import org.osgi.framework.*;

import java.util.*;

import static org.apache.aries.component.dsl.OSGi.*;

/**
 * @author Carlos Sierra Andrés
 */
@Header(name = Constants.BUNDLE_ACTIVATOR, value = "${@class}")
public class OpenApiBundleActivator implements BundleActivator {

    private OSGiResult result;

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        OSGi<?> program =
            serviceReferences(OpenAPI.class).flatMap(sr ->
            service(sr).flatMap(openAPI ->
            just(
                new OpenApiPrototypeServiceFactory(
                    new PropertyWrapper(sr),
                    openAPI))).flatMap(factory ->
            register(
                Object.class,
                factory,
                () -> getProperties(sr))
            ));

        result = program.run(bundleContext);
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        result.close();
    }

    private Map<String, Object> getProperties(
        CachingServiceReference<OpenAPI> serviceReference) {

        HashMap<String, Object> map = new HashMap<>();

        for (String propertyKey : serviceReference.getPropertyKeys()) {
            map.put(propertyKey, serviceReference.getProperty(propertyKey));
        }

        map.put("org.apache.aries.jax.rs.whiteboard.application.scoped", true);
        map.put("osgi.jaxrs.resource", true);

        return map;
    }

}
