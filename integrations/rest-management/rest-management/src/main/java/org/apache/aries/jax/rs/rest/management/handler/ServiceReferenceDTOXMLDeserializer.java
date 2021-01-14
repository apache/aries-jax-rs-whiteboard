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
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;

import org.osgi.framework.dto.ServiceReferenceDTO;

class ServiceReferenceDTOXMLDeserializer {

    private final XMLDeserializer deserializer;

    public ServiceReferenceDTOXMLDeserializer(XMLDeserializer deserializer) {
        this.deserializer = deserializer;
    }

    public ServiceReferenceDTO deserializeServiceReferenceDTO(InputStream entityStream) throws IOException {
        AtomicReference<ServiceReferenceDTO> service = new AtomicReference<>();
        deserialize(service::set, entityStream);
        return service.get();
    }

    public List<ServiceReferenceDTO> deserializeServiceReferenceDTOList(InputStream entityStream) throws IOException {
        List<ServiceReferenceDTO> services = new ArrayList<>();
        deserialize(services::add, entityStream);
        return services;
    }

    private void deserialize(
            Consumer<ServiceReferenceDTO> collector, InputStream entityStream)
        throws IOException {

        AtomicReference<ServiceReferenceDTO> service = new AtomicReference<>();
        AtomicBoolean inUsingBundles = new AtomicBoolean();
        deserializer.deserialize((start, next) -> {
            String el = start.getName().getLocalPart();
            XMLEvent event = start;
            if (el.equals("service")) {
                service.set(new ServiceReferenceDTO());
            }
            else if (el.equals("id")) {
                try {event = next.nextEvent();} catch (Exception e) {throw new RuntimeException(e);}
                service.get().id = Long.parseLong(event.asCharacters().getData());
            }
            else if (el.equals("properties")) {
                service.get().properties = new HashMap<>();
            }
            else if (el.equals("property")) {
                Attribute keyAT = start.getAttributeByName(new QName("name"));
                Attribute typeAT = start.getAttributeByName(new QName("type"));
                Attribute valueAT = start.getAttributeByName(new QName("value"));
                String valueTxt = null;
                boolean array = false;
                if (valueAT == null) {
                    try {event = next.nextEvent();} catch (Exception e) {throw new RuntimeException(e);}
                    if (!event.isEndElement()) {
                        valueTxt = event.asCharacters().getData();
                        array = true;
                    }
                }
                else {
                    valueTxt = valueAT.getValue();
                }

                service.get().properties.put(keyAT.getValue(), Coerce.from(keyAT, typeAT, valueTxt, array));
            }
            else if (el.equals("bundle") && inUsingBundles.get()) {
                try {event = next.nextEvent();} catch (Exception e) {throw new RuntimeException(e);}
                long[] newArray = new long[service.get().usingBundles.length + 1];
                System.arraycopy(service.get().usingBundles, 0, newArray, 0, service.get().usingBundles.length);
                newArray[newArray.length - 1] = bundleIdFromURI(event.asCharacters().getData());
                service.get().usingBundles = newArray;
            }
            else if (el.equals("bundle")) {
                try {event = next.nextEvent();} catch (Exception e) {throw new RuntimeException(e);}
                service.get().bundle = bundleIdFromURI(event.asCharacters().getData());
            }
            else if (el.equals("usingBundles")) {
                service.get().usingBundles = new long[0];
                inUsingBundles.set(true);
            }
            return event;
        }, end -> {
            String el = end.getName().getLocalPart();
            if (el.equals("service")) {
                collector.accept(service.get());
                service.set(null);
            }
            else if (el.equals("usingBundles")) {
                inUsingBundles.set(false);
            }
        }, entityStream);
    }

    long bundleIdFromURI(String bundleURI) {
        final URI uri = URI.create(bundleURI);
        final String uriPath = uri.getPath();
        final Path path = Paths.get(uriPath);
        final String bundleId = path.getFileName().toString();
        return Long.parseLong(bundleId);
    }

}
