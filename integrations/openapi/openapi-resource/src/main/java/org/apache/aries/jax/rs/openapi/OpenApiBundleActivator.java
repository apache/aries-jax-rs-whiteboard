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

import org.apache.aries.component.dsl.OSGi;
import org.apache.aries.component.dsl.OSGiResult;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxrs.openapi.OpenApiCustomizer;
import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import java.util.*;

import static org.apache.aries.component.dsl.OSGi.*;
import static org.apache.aries.component.dsl.Utils.highest;

/**
 * @author Carlos Sierra Andrés
 */
@Header(name = Constants.BUNDLE_ACTIVATOR, value = "${@class}")
public class OpenApiBundleActivator implements BundleActivator {

    public static final String CONFIG_PID = "org.apache.aries.jax.rs.openapi";

    public static OSGi<Dictionary<String, ?>> CONFIGURATION =
            all(
                configurations(CONFIG_PID),
                coalesce(
                    configuration(CONFIG_PID),
                    just(Hashtable::new)
                )
            ).filter(
                c -> !Objects.equals(c.get("enabled"), "false")
            );

    private OSGiResult result;

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        OSGi<?> program =
            CONFIGURATION.flatMap(configuration ->
            just(new OpenApiPrototypeServiceFactory(configuration)).flatMap(factory ->
            processOpenApiCustomizer(configuration, factory).then(
            processSecurityDefinitions(configuration, factory).then(
            register(
                Feature.class,
                factory,
                getProperties(configuration))
            ))));

        result = program.run(bundleContext);
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        result.close();
    }

    private OSGi<?> processOpenApiCustomizer(
            Dictionary<String, ?> properties,
            OpenApiPrototypeServiceFactory openApiPrototypeServiceFactory) {

        Object openApiCustomizerSelect = properties.get("open.api.customizer.select");

        if (openApiCustomizerSelect instanceof String) {
            return service(
                    highest(
                        serviceReferences(
                            OpenApiCustomizer.class,
                            openApiCustomizerSelect.toString()
                        )
                    )
            ).effects(
                openApiPrototypeServiceFactory::setOpenApiCustomizer,
                __ -> openApiPrototypeServiceFactory.setOpenApiCustomizer(null)
            );
        }
        else {
            return just(0);
        }
    }

    private OSGi<?> processSecurityDefinitions(
            Dictionary<String, ?> properties,
            OpenApiPrototypeServiceFactory openApiPrototypeServiceFactory) {

        Object openApisecurityDefinitionsSelect = properties.get("open.api.security.definitions.select");

        if (openApisecurityDefinitionsSelect instanceof String) {
            return service(
                    highest(
                        serviceReferences(
                            Map.class,
                            openApisecurityDefinitionsSelect.toString()
                        )
                    )
            ).effects(
                openApiPrototypeServiceFactory::setSecurityDefinitions,
                __ -> openApiPrototypeServiceFactory.setSecurityDefinitions(null)
            );
        }
        else {
            return just(0);
        }
    }

    private Map<String, Object> getProperties(Dictionary<String, ?> configuration) {
        HashMap<String, Object> map = new HashMap<>();

        Enumeration<String> keys = configuration.keys();

        while (keys.hasMoreElements()) {
            String key = keys.nextElement();

            map.put(key, configuration.get(key));
        }

        map.put("osgi.jaxrs.extension", true);

        return map;
    }

}
