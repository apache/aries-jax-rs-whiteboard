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

package org.apache.aries.jax.rs.rest.management.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.osgi.framework.dto.ServiceReferenceDTO;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;

@SuppressWarnings("serial")
class ServiceReferenceDTOJsonDeserializer extends StdDeserializer<ServiceReferenceDTO> {

    protected ServiceReferenceDTOJsonDeserializer() {
        this(null);
    }

    protected ServiceReferenceDTOJsonDeserializer(Class<ServiceReferenceDTO> t) {
        super(t);
    }

    @Override
    public ServiceReferenceDTO deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException {

        ServiceReferenceDTO referenceDTO = new ServiceReferenceDTO();
        JsonNode node = jp.getCodec().readTree(jp);
        referenceDTO.id = node.get("id").asLong();
        referenceDTO.properties = new HashMap<>();
        node.get("properties").fields().forEachRemaining(
            entry -> {
                referenceDTO.properties.put(entry.getKey(), Coerce.from(entry));
            }
        );

        String bundleURL = node.get("bundle").asText();
        referenceDTO.bundle = Long.parseLong(bundleURL.substring(bundleURL.lastIndexOf('/') + 1));

        List<Long> usingBundles = new ArrayList<>();
        for (JsonNode arrayNode : (ArrayNode)node.get("usingBundles")) {
            bundleURL = arrayNode.asText();
            usingBundles.add(Long.parseLong(bundleURL.substring(bundleURL.lastIndexOf('/') + 1)));
        }

        referenceDTO.usingBundles = usingBundles.stream().mapToLong(Long::longValue).toArray();
        return referenceDTO;
    }

}