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
        OSGi<?> program = serviceReferences(OpenAPI.class).flatMap(
            sr -> service(sr).flatMap(
                openAPI -> just(
                    new OpenApiPrototypeServiceFactory(
                        new PropertyWrapper(sr),
                        openAPI)
                )
            ).flatMap(
                factory -> register(
                    Object.class,
                    factory,
                    () -> getProperties(sr)
                ).then(
                    services(SchemaProcessor.class).foreach(
                        factory::addModelConverter,
                        factory::removeModelConverter
                    )
                )
            )
        );

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
