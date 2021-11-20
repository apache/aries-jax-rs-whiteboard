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

package org.apache.aries.jax.rs.jackson;

import static org.apache.aries.component.dsl.OSGi.all;
import static org.apache.aries.component.dsl.OSGi.coalesce;
import static org.apache.aries.component.dsl.OSGi.configuration;
import static org.apache.aries.component.dsl.OSGi.configurations;
import static org.apache.aries.component.dsl.OSGi.just;
import static org.apache.aries.component.dsl.OSGi.register;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.aries.component.dsl.OSGi;
import org.apache.aries.component.dsl.OSGiResult;
import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

@Header(name = Constants.BUNDLE_ACTIVATOR, value = "${@class}")
public class JaxbJsonBundleActivator implements BundleActivator {

    public static final String CONFIG_PID = "org.apache.aries.jax.rs.jackson";

    public static OSGi<Dictionary<String, ?>> CONFIGURATION =
        all(
            configurations(CONFIG_PID),
            coalesce(
                configuration(CONFIG_PID),
                just(Hashtable::new))
        ).filter(
            c -> !Objects.equals(c.get("enabled"), "false")
        );

    @Override
    public void start(BundleContext context) throws Exception {
        _result =
                CONFIGURATION.flatMap(properties ->
                register(
                    new String[]{
                        MessageBodyReader.class.getName(),
                        MessageBodyWriter.class.getName()
                    },
                    new JsonProviderPrototypeServiceFactory(properties),
                    getRegistrationProperties(properties)
                )).
                run(context);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        _result.close();
    }
    private OSGiResult _result;

    private Map<String, ?> getRegistrationProperties(
        Dictionary<String, ?> properties) {

        Hashtable<String, Object> serviceProps =
            new Hashtable<String, Object>() {{
                put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, true);
                put(JaxrsWhiteboardConstants.JAX_RS_MEDIA_TYPE, MediaType.APPLICATION_JSON);
                putIfAbsent(
                    JaxrsWhiteboardConstants.JAX_RS_NAME, "jaxb-json");
                put(Constants.SERVICE_RANKING, Integer.MIN_VALUE);
                // Note that these are useful information, and bind us to the Jackson JAXB API
                // which is otherwise only optionally required
                put("jackson.jaxb.version", new com.fasterxml.jackson.module.jaxb.PackageVersion().version().toString());
                put("jackson.jaxrs.json.version", new com.fasterxml.jackson.jaxrs.json.PackageVersion().version().toString());
            }};

        Enumeration<String> keys = properties.keys();

        while (keys.hasMoreElements()) {
            String key = keys.nextElement();

            if(!key.startsWith(".")) {
                serviceProps.put(key, properties.get(key));
            }
        }

        return serviceProps;
    }

}