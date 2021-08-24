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
package org.apache.aries.jax.rs.rest.management.schema;

import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

public class ServiceSchemaAdapter extends XmlAdapter<ServiceSchemaAdapter.ServiceSchemaAdapted, ServiceSchema> {

    @XmlRootElement(name = "service")
    public static class ServiceSchemaAdapted {
        public long id;
        public String bundle;
        @XmlJavaTypeAdapter(PropertiesAdapter.class)
        public Map<String, Object> properties;
        public String[] usingBundles;
    }

    @Override
    public ServiceSchemaAdapted marshal(ServiceSchema serviceSchema) throws Exception {
        ServiceSchemaAdapted adapter = new ServiceSchemaAdapted();
        adapter.id = serviceSchema.id;
        adapter.bundle = serviceSchema.bundle;
        adapter.properties = serviceSchema.properties;
        adapter.usingBundles = serviceSchema.usingBundles;
        return adapter;
    }

    @Override
    public ServiceSchema unmarshal(ServiceSchemaAdapted adapter) throws Exception {
        ServiceSchema serviceSchema = new ServiceSchema();
        serviceSchema.id = adapter.id;
        serviceSchema.bundle = adapter.bundle;
        serviceSchema.properties = adapter.properties;
        serviceSchema.usingBundles = adapter.usingBundles;
        return serviceSchema;
    }

}
