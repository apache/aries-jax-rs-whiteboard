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

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.apache.aries.jax.rs.rest.management.internal.coerce.Coerce;

public class PropertiesAdapter extends XmlAdapter<PropertiesAdapter.PropertiesAdapted, Map<String, Object>> {

    public static class PropertiesAdapted {
        public List<Property> property = new ArrayList<>();
    }

    public static class Property {
        @XmlAttribute(name = "name", required = true)
        public String key;
        @XmlAttribute(name = "type")
        public String type;
        @XmlAttribute(name = "value")
        public String attributeValue;
        @XmlValue
        public String elementValue;
    }

    @Override
    public PropertiesAdapted marshal(Map<String, Object> map) throws Exception {
        PropertiesAdapted adapter = new PropertiesAdapted();
        for(Map.Entry<String, Object> entry : map.entrySet()) {
            Property property = new Property();
            property.key = entry.getKey();
            Object v = entry.getValue();

            boolean array = false;
            if (Long.class.isInstance(v)) {
                property.type = "Long";
            }
            else if (Long[].class.isInstance(v)) {
                property.type = "Long";
                array = true;
            }
            else if (Double.class.isInstance(v)) {
                property.type = "Double";
            }
            else if (Double[].class.isInstance(v)) {
                property.type = "Double";
                array = true;
            }
            else if (Float.class.isInstance(v)) {
                property.type = "Float";
            }
            else if (Float[].class.isInstance(v)) {
                property.type = "Float";
                array = true;
            }
            else if (Integer.class.isInstance(v)) {
                property.type = "Integer";
            }
            else if (Integer[].class.isInstance(v)) {
                property.type = "Integer";
                array = true;
            }
            else if (Byte.class.isInstance(v)) {
                property.type = "Byte";
            }
            else if (Byte[].class.isInstance(v)) {
                property.type = "Byte";
                array = true;
            }
            else if (Character.class.isInstance(v)) {
                property.type = "Character";
            }
            else if (Character[].class.isInstance(v)) {
                property.type = "Character";
                array = true;
            }
            else if (Boolean.class.isInstance(v)) {
                property.type = "Boolean";
            }
            else if (Boolean[].class.isInstance(v)) {
                property.type = "Boolean";
                array = true;
            }
            else if (Short.class.isInstance(v)) {
                property.type = "Short";
            }
            else if (Short[].class.isInstance(v)) {
                property.type = "Short";
                array = true;
            }
            else if (String[].class.isInstance(v)) {
                array = true;
            }

            if (!array) {
                property.attributeValue = String.valueOf(v);
            }
            else {
                property.elementValue = Arrays.stream(
                    (Object[])v
                ).map(
                    String::valueOf
                ).collect(
                    joining("\n")
                );
            }

            adapter.property.add(property);
        }
        return adapter;
    }

    @Override
    public Map<String, Object> unmarshal(PropertiesAdapted adapter) throws Exception {
        Map<String, Object> map = new HashMap<>();
        for(Property property : adapter.property) {
            String valueTxt = property.attributeValue;
            boolean array = false;
            if (property.elementValue != null) {
                valueTxt = property.elementValue;
                array = true;
            }
            map.put(
                property.key,
                Coerce.from(
                    Coerce.type(property.type, property.key),
                    array,
                    Optional.ofNullable(
                        valueTxt
                    ).map(
                        s -> s.trim().split("\\s*\\n\\s*")
                    ).map(Arrays::asList).orElseGet(ArrayList::new)
                )
            );
        }

        return map;
    }

}
