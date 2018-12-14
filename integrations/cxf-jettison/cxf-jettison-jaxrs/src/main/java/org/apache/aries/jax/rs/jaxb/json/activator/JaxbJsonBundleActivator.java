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

package org.apache.aries.jax.rs.jaxb.json.activator;

import org.apache.aries.component.dsl.OSGi;
import org.apache.aries.component.dsl.OSGiResult;
import org.apache.cxf.jaxrs.utils.schemas.SchemaHandler;
import org.codehaus.jettison.mapped.TypeConverter;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;

import static org.apache.aries.component.dsl.OSGi.all;
import static org.apache.aries.component.dsl.OSGi.coalesce;
import static org.apache.aries.component.dsl.OSGi.combine;
import static org.apache.aries.component.dsl.OSGi.configuration;
import static org.apache.aries.component.dsl.OSGi.configurations;
import static org.apache.aries.component.dsl.OSGi.just;
import static org.apache.aries.component.dsl.OSGi.register;
import static org.apache.aries.component.dsl.OSGi.service;
import static org.apache.aries.component.dsl.OSGi.serviceReferences;
import static org.apache.aries.component.dsl.Utils.highest;

public class JaxbJsonBundleActivator implements BundleActivator {

    public static final String CONFIG_PID = "org.apache.aries.jax.rs.jaxb.json";

    public static OSGi<Dictionary<String, ?>> CONFIGURATION =
        coalesce(
            all(
                configurations(CONFIG_PID),
                configuration(CONFIG_PID)
            ),
            just(Hashtable::new)
        );

    @Override
    public void start(BundleContext context) throws Exception {
        _result =
            CONFIGURATION.flatMap(properties ->
            createJsonFactory(properties).flatMap(jsonFactory ->
            register(
                new String[]{
                    MessageBodyReader.class.getName(),
                    MessageBodyWriter.class.getName()
                },
                () -> jsonFactory,
                () -> getRegistrationProperties(properties)
            ))).
            run(context);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        _result.close();
    }
    private OSGiResult _result;

    private static OSGi<JsonProviderPrototypeServiceFactory> createJsonFactory(
        Dictionary<String, ?> properties) {

        Object nameObject = properties.get("osgi.jaxrs.name");
        String name;

        if (nameObject == null) {
            name = "jaxb-json";
        }
        else {
            name = nameObject.toString();
        }

        return combine(
            JsonProviderPrototypeServiceFactory::new,
            just(properties),
            getStaticOptionalServices(name, TypeConverter.class),
            getStaticOptionalServices(name, Marshaller.Listener.class),
            getStaticOptionalServices(name, Unmarshaller.Listener.class),
            getStaticOptionalServices(name, SchemaHandler.class)
        );
    }

    private static <T> OSGi<Optional<T>> getStaticOptionalServices(
        String name, Class<T> clazz) {

        return coalesce(
            service(
                highest(
                    serviceReferences(
                        clazz, "(osgi.jaxrs.name=" + name + ")"))).
                map(Optional::of),
            just(Optional.empty()));
    }

    private Map<String, ?> getRegistrationProperties(
        Dictionary<String, ?> properties) {

        @SuppressWarnings("serial")
        Hashtable<String, Object> serviceProps =
            new Hashtable<String, Object>() {{
                put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, true);
                putIfAbsent(
                    JaxrsWhiteboardConstants.JAX_RS_NAME, "jaxb-json");
                put(Constants.SERVICE_RANKING, Integer.MIN_VALUE);
            }};

        Enumeration<String> keys = properties.keys();

        while (keys.hasMoreElements()) {
            String key = keys.nextElement();

            serviceProps.put(key, properties.get(key));
        }

        return serviceProps;
    }

}