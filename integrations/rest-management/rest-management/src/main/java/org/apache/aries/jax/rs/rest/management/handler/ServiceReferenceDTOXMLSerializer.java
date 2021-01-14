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

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;

import javax.ws.rs.core.UriInfo;

import org.osgi.framework.dto.ServiceReferenceDTO;
import org.w3c.dom.Element;

class ServiceReferenceDTOXMLSerializer {

    private final XMLSerializer serializer;

    public ServiceReferenceDTOXMLSerializer(XMLSerializer serializer) {
        this.serializer = serializer;
    }

    public void serialize(
        ServiceReferenceDTO service, UriInfo uriInfo, OutputStream entityStream) throws IOException {

        serialize(singleton(service), null, uriInfo, entityStream);
    }


    public void serialize(
        Collection<ServiceReferenceDTO> services, UriInfo uriInfo, OutputStream entityStream) throws IOException {

        serialize(services, "services", uriInfo, entityStream);
    }

    public void serialize(
        Collection<ServiceReferenceDTO> services, String rootElement, UriInfo uriInfo, OutputStream entityStream) throws IOException {

        try {
            serializer.serialize(
                rootElement,
                (dom, parentNode) -> services.stream().map(
                    serviceReferenceDTO -> {
                        Element serviceEl = dom.createElement("service");

                        Element idEl = dom.createElement("id");
                        idEl.setTextContent(String.valueOf(serviceReferenceDTO.id));
                        Element propertiesEl = dom.createElement("properties");
                        serviceReferenceDTO.properties.forEach((k, v) -> {
                            Element propertyEl = dom.createElement("property");
                            propertyEl.setAttribute("name", k);
                            boolean array = false;
                            if (Long.class.isInstance(v)) {
                                propertyEl.setAttribute("type", "Long");
                            }
                            else if (Long[].class.isInstance(v)) {
                                propertyEl.setAttribute("type", "Long");
                                array = true;
                            }
                            else if (Double.class.isInstance(v)) {
                                propertyEl.setAttribute("type", "Double");
                            }
                            else if (Double[].class.isInstance(v)) {
                                propertyEl.setAttribute("type", "Double");
                                array = true;
                            }
                            else if (Float.class.isInstance(v)) {
                                propertyEl.setAttribute("type", "Float");
                            }
                            else if (Float[].class.isInstance(v)) {
                                propertyEl.setAttribute("type", "Float");
                                array = true;
                            }
                            else if (Integer.class.isInstance(v)) {
                                propertyEl.setAttribute("type", "Integer");
                            }
                            else if (Integer[].class.isInstance(v)) {
                                propertyEl.setAttribute("type", "Integer");
                                array = true;
                            }
                            else if (Byte.class.isInstance(v)) {
                                propertyEl.setAttribute("type", "Byte");
                            }
                            else if (Byte[].class.isInstance(v)) {
                                propertyEl.setAttribute("type", "Byte");
                                array = true;
                            }
                            else if (Character.class.isInstance(v)) {
                                propertyEl.setAttribute("type", "Character");
                            }
                            else if (Character[].class.isInstance(v)) {
                                propertyEl.setAttribute("type", "Character");
                                array = true;
                            }
                            else if (Boolean.class.isInstance(v)) {
                                propertyEl.setAttribute("type", "Boolean");
                            }
                            else if (Boolean[].class.isInstance(v)) {
                                propertyEl.setAttribute("type", "Boolean");
                                array = true;
                            }
                            else if (Short.class.isInstance(v)) {
                                propertyEl.setAttribute("type", "Short");
                            }
                            else if (Short[].class.isInstance(v)) {
                                propertyEl.setAttribute("type", "Short");
                                array = true;
                            }
                            else if (String[].class.isInstance(v)) {
                                array = true;
                            }

                            if (!array) {
                                propertyEl.setAttribute("value", String.valueOf(v));
                            }
                            else {
                                propertyEl.setTextContent(
                                    Arrays.stream((Object[])v).map(
                                        String::valueOf
                                    ).collect(
                                        joining("\n")
                                    )
                                );
                            }

                            propertiesEl.appendChild(propertyEl);
                        });

                        Element bundleEl = dom.createElement("bundle");
                        bundleEl.setTextContent(
                            uriInfo.getBaseUriBuilder().path("framework").path("bundle").path("{id}").build(serviceReferenceDTO.bundle).toASCIIString()
                        );
                        Element usingBundlesEl = dom.createElement("usingBundles");
                        for (long usingBundle : serviceReferenceDTO.usingBundles) {
                            Element bEl = dom.createElement("bundle");
                            bEl.setTextContent(
                                uriInfo.getBaseUriBuilder().path("framework").path("bundle").path("{id}").build(usingBundle).toASCIIString()
                            );
                            usingBundlesEl.appendChild(bEl);
                        }

                        serviceEl.appendChild(idEl);
                        serviceEl.appendChild(propertiesEl);
                        serviceEl.appendChild(bundleEl);
                        serviceEl.appendChild(usingBundlesEl);

                        return serviceEl;
                    }
                ).forEach(
                    bundleEL -> parentNode.appendChild(bundleEL)
                ),
                entityStream
            );
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

}
