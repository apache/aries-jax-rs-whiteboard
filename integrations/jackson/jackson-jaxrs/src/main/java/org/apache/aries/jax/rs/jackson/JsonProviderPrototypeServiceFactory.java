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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.osgi.annotation.bundle.Capability;
import org.osgi.framework.Bundle;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.service.jaxrs.whiteboard.annotations.RequireJaxrsWhiteboard;

import com.fasterxml.jackson.jaxrs.cfg.Annotations;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

@Capability(
    attribute = {
        "objectClass:List<String>='javax.ws.rs.ext.MessageBodyReader,javax.ws.rs.ext.MessageBodyWriter'",
        "osgi.jaxrs.media.type=application/json",
        "osgi.jaxrs.name=jackson"
    },
    namespace = ServiceNamespace.SERVICE_NAMESPACE
)
@RequireJaxrsWhiteboard
public class JsonProviderPrototypeServiceFactory
    implements PrototypeServiceFactory<JacksonJsonProvider> {

    JsonProviderPrototypeServiceFactory(Dictionary<String, ?> properties) {
        _properties = properties;
    }

    @Override
    public JacksonJsonProvider getService(
        Bundle bundle,
        ServiceRegistration<JacksonJsonProvider> registration) {

        return createJsonProvider(_properties);
    }

    @Override
    public void ungetService(
        Bundle bundle,
        ServiceRegistration<JacksonJsonProvider> registration,
        JacksonJsonProvider service) {

    }

    private Dictionary<String, ?> _properties;

    private JacksonJsonProvider createJsonProvider(
        Dictionary<String, ?> properties) {

        List<Annotations> list = new ArrayList<>();

        if(getBooleanProperty(properties, "jackson.annotations.enabled", true)) {
            list.add(Annotations.JACKSON);
        }

        if(getBooleanProperty(properties, "jaxb.annotations.enabled", true)) {
            list.add(Annotations.JAXB);
        }

        JacksonJsonProvider jsonProvider = new JacksonJsonProvider(list.toArray(new Annotations[list.size()]));

        // Do we want to enable any SerializationFeature, DeserializationFeature or JaxRSFeature?

        return jsonProvider;
    }

    private boolean getBooleanProperty(Dictionary<String, ?> properties, String key, boolean defaultValue) {
        Object object = properties.get(key);
        if(object == null) {
            return defaultValue;
        } else {
            return Boolean.parseBoolean(String.valueOf(object));
        }
    }
}
