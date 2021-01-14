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

import javax.ws.rs.core.UriInfo;

import org.osgi.framework.dto.ServiceReferenceDTO;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

@SuppressWarnings("serial")
class ServiceReferenceDTOJsonSerializer extends StdSerializer<ServiceReferenceDTO> {

    private final UriInfo uriInfo;

    protected ServiceReferenceDTOJsonSerializer(UriInfo uriInfo) {
        this(uriInfo, null);
    }

    protected ServiceReferenceDTOJsonSerializer(UriInfo uriInfo, Class<ServiceReferenceDTO> t) {
        super(t);
        this.uriInfo = uriInfo;
    }

    @Override
    public void serialize(
            ServiceReferenceDTO value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {

        gen.writeStartObject();
        gen.writeNumberField("id", value.id);
        gen.writeObjectField("properties", value.properties);
        gen.writeStringField(
            "bundle",
            uriInfo.getBaseUriBuilder().path("framework").path("bundle").path("{id}").build(value.bundle).toASCIIString()
        );
        gen.writeArrayFieldStart("usingBundles");
        for (long usingBundle : value.usingBundles) {
            gen.writeString(
                uriInfo.getBaseUriBuilder().path("framework").path("bundle").path("{id}").build(usingBundle).toASCIIString()
            );
        }
        gen.writeEndArray();
        gen.writeEndObject();
    }

}