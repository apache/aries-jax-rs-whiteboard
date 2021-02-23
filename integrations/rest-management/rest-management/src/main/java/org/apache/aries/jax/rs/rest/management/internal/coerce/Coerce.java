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

package org.apache.aries.jax.rs.rest.management.internal.coerce;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import javax.xml.stream.events.Attribute;

import org.osgi.framework.Constants;

import com.fasterxml.jackson.databind.JsonNode;

public class Coerce {

    private Coerce() {
    }

    public static List<String> elements(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node.isArray()) {
            for (Iterator<JsonNode> itr = node.elements(); itr.hasNext();) {
                list.add(itr.next().asText());
            }
        }
        else if (node.isValueNode()) {
            list.add(node.asText());
        }
        return list;
    }

    public static String type(String type, String name) {
        if (Constants.SERVICE_ID.equals(name) ||
            Constants.SERVICE_BUNDLEID.equals(name)) {
            return "Long";
        }
        else if (Constants.SERVICE_RANKING.equals(name)) {
            return "Integer";
        }
        else if (Constants.OBJECTCLASS.equals(name) ||
                 Constants.SERVICE_PID.equals(name) ||
                 Constants.SERVICE_SCOPE.equals(name)) {
            return "String";
        }

        if (type == null) {
            return "String";
        }
        return type;
    }

    public static String type(JsonNode node, String name) {
        if (Constants.SERVICE_ID.equals(name) ||
            Constants.SERVICE_BUNDLEID.equals(name)) {
            return "Long";
        }
        else if (Constants.SERVICE_RANKING.equals(name)) {
            return "Integer";
        }
        else if (Constants.OBJECTCLASS.equals(name) ||
                 Constants.SERVICE_PID.equals(name) ||
                 Constants.SERVICE_SCOPE.equals(name)) {
            return "String";
        }

        if (node.isArray()) {
            if (node.isEmpty()) {
                return "String";
            }

            node = node.get(0);
        }

        if (node.isBoolean()) {
            return "Boolean";
        }
        else if (node.isLong()) {
            return "Long";
        }
        else if (node.isInt()) {
            return "Integer";
        }
        else if (node.isNumber()) {
            String nodeValue = node.asText();
            try {
                Long.parseLong(nodeValue);
                return "Long";
            }
            catch (NumberFormatException nfe1) {
                try {
                    Double.parseDouble(nodeValue);
                    return "Double";
                }
                catch (NumberFormatException nfe2) {
                    throw new IllegalArgumentException(nfe2);
                }
            }
        }

        return "String";
    }

    public static Object from(Entry<String, JsonNode> entry) {
        JsonNode valueNode = entry.getValue();
        return from(
            Coerce.type(valueNode, entry.getKey()),
            valueNode.isArray(),
            Coerce.elements(valueNode)
        );
    }

    public static Object from(Attribute keyAT, Attribute typeAT, String txt, boolean array) {
        List<String> valueParts = Optional.ofNullable(txt).map(
            s -> s.trim().split("\\s*\\n\\s*")
        ).map(Arrays::asList).orElseGet(ArrayList::new);

        return from(type((typeAT == null ? null : typeAT.getValue()), keyAT.getValue()), array, valueParts);
    }

    public static Object from(
        String type, boolean array, List<String> valueParts) {

        Objects.requireNonNull(type);

        if (array) {
            if (type.equals("Long")) {
                return valueParts.stream().map(Long::parseLong).mapToLong(
                    Long::longValue).toArray();
            }
            else if (type.equals("Double")) {
                return valueParts.stream().map(Double::parseDouble).mapToDouble(
                    Double::doubleValue).toArray();
            }
            else if (type.equals("Float")) {
                float[] floats = new float[valueParts.size()];
                for (int i = 0; i < valueParts.size(); i++) {
                    floats[i] = Float.parseFloat(valueParts.get(i));
                }
                return floats;
            }
            else if (type.equals("Integer")) {
                return valueParts.stream().map(Integer::parseInt).mapToInt(
                    Integer::intValue).toArray();
            }
            else if (type.equals("Byte")) {
                byte[] bytes = new byte[valueParts.size()];
                for (int i = 0; i < valueParts.size(); i++) {
                    bytes[i] = Byte.parseByte(valueParts.get(i));
                }
                return bytes;
            }
            else if (type.equals("Character")) {
                return valueParts.stream().map(Integer::parseInt).mapToInt(
                    Integer::intValue).toArray();
            }
            else if (type.equals("Boolean")) {
                boolean[] booleans = new boolean[valueParts.size()];
                for (int i = 0; i < valueParts.size(); i++) {
                    booleans[i] = Boolean.parseBoolean(valueParts.get(i));
                }
                return booleans;
            }
            else if (type.equals("Short")) {
                short[] shorts = new short[valueParts.size()];
                for (int i = 0; i < valueParts.size(); i++) {
                    shorts[i] = Short.parseShort(valueParts.get(i));
                }
                return shorts;
            }
            else {
                return valueParts.stream().toArray(String[]::new);
            }
        }
        else {
            if (valueParts.isEmpty()) {
                return null;
            }

            if (type.equals("Long")) {
                return Long.parseLong(valueParts.get(0));
            }
            else if (type.equals("Double")) {
                return Double.parseDouble(valueParts.get(0));
            }
            else if (type.equals("Float")) {
                return Float.parseFloat(valueParts.get(0));
            }
            else if (type.equals("Integer")) {
                return Integer.parseInt(valueParts.get(0));
            }
            else if (type.equals("Byte")) {
                return Byte.parseByte(valueParts.get(0));
            }
            else if (type.equals("Character")) {
                return Integer.parseInt(valueParts.get(0));
            }
            else if (type.equals("Boolean")) {
                return Boolean.parseBoolean(valueParts.get(0));
            }
            else if (type.equals("Short")) {
                return Short.parseShort(valueParts.get(0));
            }
            else {
                return valueParts.get(0);
            }
        }
    }

}
