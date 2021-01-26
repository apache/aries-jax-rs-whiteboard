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

import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.OpenAPI;
import org.apache.aries.component.dsl.CachingServiceReference;
import org.apache.aries.component.dsl.OSGi;
import org.apache.aries.component.dsl.OSGiResult;
import org.osgi.annotation.bundle.Header;
import org.osgi.framework.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.apache.aries.component.dsl.OSGi.*;
import static org.apache.aries.component.dsl.Utils.accumulate;

/**
 * @author Carlos Sierra Andrés
 */
@Header(name = Constants.BUNDLE_ACTIVATOR, value = "${@class}")
public class OpenApiBundleActivator implements BundleActivator {

    private OSGiResult result;

    private static <T> OSGi<Set<T>> sequence(Set<OSGi<T>> list) {
        Set<T> objects = new HashSet<>();

        OSGi<Set<T>> result = just(() -> objects);

        for (OSGi<T> osgi : list) {
            result = osgi.effects(objects::add, objects::remove).then(result);
        }

        return result;
    }

    private static OpenAPIWithModelResolvers pairModelConverterWithOpenAPI(
        CachingServiceReference<OpenAPI> oasr, List<CachingServiceReference<ModelConverter>> mcsrs) {

        return new OpenAPIWithModelResolvers(
            oasr,
            mcsrs.stream().filter(
                mcsr -> filterModelConverter(oasr, mcsr)
            ).collect(
                Collectors.toSet()
            )
        );
    }

    private static boolean filterModelConverter(
        CachingServiceReference<OpenAPI> oasr,
        CachingServiceReference<ModelConverter> mcsr) {

        final String[] propertyValues = canonicalize(
            mcsr.getProperty("osgi.jaxrs.openapi.select"));

        for (String propertyValue : propertyValues) {
            if (propertyValue == null || propertyValue.isEmpty()) {
                continue;
            }
            try {
                Filter filter = FrameworkUtil.createFilter(propertyValue);

                if (filter.match(oasr.getServiceReference())) {
                    return true;
                }
            } catch (InvalidSyntaxException ise) {
                //log
                continue;
            }
        }

        return false;
    }

    private static String[] canonicalize(Object propertyValue) {
        if (propertyValue == null) {
            return new String[0];
        }
        if (propertyValue instanceof String[]) {
            return (String[]) propertyValue;
        }
        if (propertyValue instanceof Collection) {
            return
                ((Collection<?>) propertyValue).stream().
                    map(
                        Object::toString
                    ).toArray(
                    String[]::new
                );
        }

        return new String[]{propertyValue.toString()};
    }

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        final OSGi<?> modelConverters = services(
            ModelConverter.class
        ).effects(
            ModelConverters.getInstance()::addConverter,
            ModelConverters.getInstance()::removeConverter
        );

        OSGi<?> program = combine(
            (openAPI, __) -> openAPI,
            serviceReferences(OpenAPI.class),
            accumulate(modelConverters)
        ).
            flatMap(openAPICachingServiceReference ->
                service(openAPICachingServiceReference).flatMap(openAPI ->
                    register(
                        Object.class,
                        new OpenApiPrototypeServiceFactory(
                            new PropertyWrapper(openAPICachingServiceReference), openAPI),
                        () -> getProperties(openAPICachingServiceReference)
                    )
                )
            );

        result = program.run(bundleContext);
    }

    private Set<OSGi<ModelConverter>> lazilyGetModelConverters(
        OpenAPIWithModelResolvers openAPIWithModelConverters) {

        return openAPIWithModelConverters.
            getModelConverters().
            stream().
            map(OSGi::service).
            collect(Collectors.toSet());
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
