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

package org.apache.aries.jax.rs.rest.management.internal;

import static org.apache.aries.component.dsl.OSGi.all;
import static org.apache.aries.component.dsl.OSGi.ignore;
import static org.apache.aries.component.dsl.OSGi.just;
import static org.apache.aries.component.dsl.OSGi.register;
import static org.apache.aries.component.dsl.OSGi.service;
import static org.apache.aries.component.dsl.OSGi.serviceReferences;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.SPECIFICATION_VERSION;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_EXTENSION;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_NAME;

import java.util.HashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Application;

import org.apache.aries.component.dsl.OSGi;
import org.apache.aries.component.dsl.OSGiResult;
import org.apache.aries.jax.rs.rest.management.feature.RestManagementFeature;
import org.apache.aries.jax.rs.rest.management.internal.client.RestClientFactoryImpl;
import org.osgi.annotation.bundle.Header;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.service.rest.RestApiExtension;
import org.osgi.service.rest.client.RestClientFactory;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Header(name = Constants.BUNDLE_ACTIVATOR, value = "${@class}")
public class RestManagementActivator implements BundleActivator {

    public static final String RMS_BASE = "/rms";

    private OSGiResult result;

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        result = all(
            ignore(
                just(
                    new RestManagementApplication(bundleContext)
                ).flatMap(application ->
                    register(
                        Application.class,
                        () -> application,
                        () -> {
                            HashMap<String, Object> map = new HashMap<>();

                            map.put(JAX_RS_NAME, RestManagementApplication.class.getSimpleName());
                            map.put(
                                JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, RMS_BASE);

                            return map;
                        }
                    ).then(
                        dynamic(
                            serviceReferences(
                                RestApiExtension.class,
                                "(&(org.osgi.rest.name=*)(org.osgi.rest.uri.path=*))"
                            ),
                            application::addExtension,
                            application::removeExtension
                        )
                    )
                )
            ),
            ignore(
                service(
                    serviceReferences(ClientBuilder.class)
                ).flatMap(
                    clientBuilder -> register(
                        RestClientFactory.class,
                        new RestClientFactoryImpl(clientBuilder),
                        null
                    )
                )
            ),
            ignore(
                register(
                    javax.ws.rs.core.Feature.class,
                    new PrototypeWrapper<>(
                        (b, r) -> new RestManagementFeature()
                    ),
                    () -> {
                        HashMap<String, Object> map = new HashMap<>();

                        map.put(JAX_RS_NAME, RestManagementFeature.class.getSimpleName());
                        map.put(
                            JAX_RS_APPLICATION_SELECT,
                            String.format(
                                "(%s=%s)", JAX_RS_NAME,
                                RestManagementApplication.class.getSimpleName()));
                        map.put(JAX_RS_EXTENSION, true);

                        return map;
                    }
                )
            ),
            ignore(
                register(
                    OpenAPI.class,
                    () -> {
                        OpenAPI openAPI = new OpenAPI();

                        openAPI.info(
                            new Info().title(
                                "Apache Aries OSGi Rest Management Service"
                            ).description(
                                "A REST API to manage an OSGi Framework"
                            ).license(
                                new License().name(
                                    "Apache 2.0"
                                ).url(
                                    "https://www.apache.org/licenses/LICENSE-2.0.html"
                                )
                            ).version(
                                SPECIFICATION_VERSION
                            ).contact(
                                new Contact().email(
                                    "dev@aries.apache.org"
                                )
                            )
                        ).externalDocs(
                            new ExternalDocumentation().description(
                                "OSGi Compendium Chapter 137 - REST Management Service Specification"
                            ).url(
                                "https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.rest.html"
                            )
                        );

                        return openAPI;
                    },
                    () -> {
                        HashMap<String, Object> map = new HashMap<>();

                        map.put(
                            JAX_RS_APPLICATION_SELECT,
                            String.format(
                                "(%s=%s)", JAX_RS_NAME,
                                RestManagementApplication.class.getSimpleName()));

                        return map;
                    }
                )
            )
        ).run(bundleContext);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        result.close();
    }

    public static <T> OSGi<Void> dynamic(
        OSGi<T> program, Consumer<T> bind, Consumer<T> unbind) {

        return program.foreach(bind, unbind);
    }

    class PrototypeWrapper<S> implements PrototypeServiceFactory<S> {

        private final BiFunction<Bundle, ServiceRegistration<S>, S> function;

        public PrototypeWrapper(BiFunction<Bundle, ServiceRegistration<S>, S> function) {
            this.function = function;
        }

        @Override
        public S getService(Bundle bundle, ServiceRegistration<S> registration) {
            return function.apply(bundle, registration);
        }

        @Override
        public void ungetService(Bundle bundle, ServiceRegistration<S> registration, S service) {
        }

    }

}
