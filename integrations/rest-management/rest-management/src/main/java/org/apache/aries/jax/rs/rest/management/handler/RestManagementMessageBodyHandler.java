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

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLEHEADER_JSON_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLEHEADER_XML_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLESTARTLEVEL_JSON_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLESTARTLEVEL_XML_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLESTATE_JSON_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLESTATE_XML_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLES_JSON_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLES_REPRESENTATIONS_JSON_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLES_REPRESENTATIONS_XML_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLES_XML_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLE_JSON_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLE_XML_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_FRAMEWORKSTARTLEVEL_JSON_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_FRAMEWORKSTARTLEVEL_XML_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_SERVICES_JSON_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_SERVICES_REPRESENTATIONS_JSON_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_SERVICES_REPRESENTATIONS_XML_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_SERVICES_XML_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_SERVICE_JSON_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_SERVICE_XML_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;

import org.apache.aries.jax.rs.rest.management.model.BundleStateDTO;
import org.apache.aries.jax.rs.rest.management.model.BundlesDTO;
import org.apache.aries.jax.rs.rest.management.model.ServiceReferenceDTOs;
import org.apache.aries.jax.rs.rest.management.model.ServicesDTO;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.FrameworkDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.framework.startlevel.dto.BundleStartLevelDTO;
import org.osgi.framework.startlevel.dto.FrameworkStartLevelDTO;
import org.w3c.dom.Element;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class RestManagementMessageBodyHandler
    implements MessageBodyReader<Object>,
        MessageBodyWriter<Object> {

    private static final TypeReference<Collection<BundleDTO>> BUNDLEDTO_COLLECTION =
        new TypeReference<Collection<BundleDTO>>() {};
    private static final TypeReference<Collection<ServiceReferenceDTO>> SERVICEDTO_COLLECTION =
        new TypeReference<Collection<ServiceReferenceDTO>>() {};
    private static final TypeReference<Dictionary<String, String>> BUNDLE_HEADER =
        new TypeReference<Dictionary<String, String>>() {};
    private final JAXBContext context;

    private final XMLSerializer serializer;
    private final XMLDeserializer deserializer;
    private final ServiceReferenceDTOXMLDeserializer serviceReferenceDTO_XMLDeserializer;
    private final ServiceReferenceDTOXMLSerializer serviceReferenceDTO_XMLSerializer;

    private final Function<UriInfo, ObjectMapper> jsonMapperFunction = uriInfo -> {
        ObjectMapper jsonMapper = new ObjectMapper().configure(
            INDENT_OUTPUT, true
        );

        SimpleModule module = new SimpleModule();
        module.addSerializer(Dictionary.class, new DictionaryJsonSerializer());
        module.addDeserializer(Dictionary.class, new DictionaryJsonDeserializer());
        module.addSerializer(ServiceReferenceDTO.class, new ServiceReferenceDTOJsonSerializer(uriInfo));
        module.addDeserializer(ServiceReferenceDTO.class, new ServiceReferenceDTOJsonDeserializer());

        return jsonMapper.registerModule(module);
    };

    @Context
    volatile UriInfo uriInfo;

    public RestManagementMessageBodyHandler() {

        serializer = new XMLSerializer();
        deserializer = new XMLDeserializer();
        serviceReferenceDTO_XMLDeserializer = new ServiceReferenceDTOXMLDeserializer(deserializer);
        serviceReferenceDTO_XMLSerializer = new ServiceReferenceDTOXMLSerializer(serializer);

        try {
            context = JAXBContext.newInstance(
                BundleDTO.class,
                BundleDTO[].class,
                BundlesDTO.class,
                BundleStartLevelDTO.class,
                BundleStateDTO.class,
                Dictionary.class,
                FrameworkStartLevelDTO.class,
                FrameworkDTO.class,
                ServicesDTO.class,
                ServiceReferenceDTO.class,
                ServiceReferenceDTOs.class
            );
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isReadable(
        Class<?> type, Type genericType, Annotation[] annotations,
        MediaType mediaType) {

        return
            (Collection.class.isAssignableFrom(type) &&
                nthTypeArgumentIs(0, genericType, BundleDTO.class) && (
                APPLICATION_BUNDLES_REPRESENTATIONS_JSON_TYPE.isCompatible(mediaType) ||
                APPLICATION_BUNDLES_REPRESENTATIONS_XML_TYPE.isCompatible(mediaType)))
                ||
            (Collection.class.isAssignableFrom(type) &&
                nthTypeArgumentIs(0, genericType, ServiceReferenceDTO.class) && (
                APPLICATION_SERVICES_REPRESENTATIONS_JSON_TYPE.isCompatible(mediaType) ||
                APPLICATION_SERVICES_REPRESENTATIONS_XML_TYPE.isCompatible(mediaType)))
                ||
            (Dictionary.class.isAssignableFrom(type) &&
                nthTypeArgumentIs(0, genericType, String.class) &&
                nthTypeArgumentIs(1, genericType, String.class) && (
                APPLICATION_BUNDLEHEADER_JSON_TYPE.isCompatible(mediaType) ||
                APPLICATION_BUNDLEHEADER_XML_TYPE.isCompatible(mediaType)))
                ||
            (type == BundleDTO.class && (
                APPLICATION_BUNDLE_JSON_TYPE.isCompatible(mediaType) ||
                APPLICATION_BUNDLE_XML_TYPE.isCompatible(mediaType)))
                ||
            (type == BundlesDTO.class && (
                APPLICATION_BUNDLES_JSON_TYPE.isCompatible(mediaType) ||
                APPLICATION_BUNDLES_XML_TYPE.isCompatible(mediaType)))
                ||
            (type == BundleStateDTO.class && (
                APPLICATION_BUNDLESTATE_JSON_TYPE.isCompatible(mediaType) ||
                APPLICATION_BUNDLESTATE_XML_TYPE.isCompatible(mediaType)))
                ||
            (type == BundleStartLevelDTO.class && (
                APPLICATION_BUNDLESTARTLEVEL_JSON_TYPE.isCompatible(mediaType) ||
                APPLICATION_BUNDLESTARTLEVEL_XML_TYPE.isCompatible(mediaType)))
                ||
            (type == FrameworkStartLevelDTO.class && (
                APPLICATION_FRAMEWORKSTARTLEVEL_JSON_TYPE.isCompatible(mediaType) ||
                APPLICATION_FRAMEWORKSTARTLEVEL_XML_TYPE.isCompatible(mediaType)))
                ||
            (type == FrameworkDTO.class && (
                APPLICATION_JSON_TYPE.isCompatible(mediaType) ||
                APPLICATION_XML_TYPE.isCompatible(mediaType)))
                ||
            (type == ServicesDTO.class && (
                APPLICATION_SERVICES_JSON_TYPE.isCompatible(mediaType) ||
                APPLICATION_SERVICES_XML_TYPE.isCompatible(mediaType)))
                ||
            (type == ServiceReferenceDTO.class && (
                APPLICATION_SERVICE_JSON_TYPE.isCompatible(mediaType) ||
                APPLICATION_SERVICE_XML_TYPE.isCompatible(mediaType)))
                ||
            (type == ServiceReferenceDTOs.class && (
                APPLICATION_SERVICES_REPRESENTATIONS_JSON_TYPE.isCompatible(mediaType) ||
                APPLICATION_SERVICES_REPRESENTATIONS_XML_TYPE.isCompatible(mediaType)))
            ;
    }

    @Override
    public boolean isWriteable(
        Class<?> type, Type genericType, Annotation[] annotations,
        MediaType mediaType) {

        return isReadable(type, genericType, annotations, mediaType);
    }

    BundleDTO readBundleDTOFrom(
            Class<? extends BundleDTO> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders,
            InputStream entityStream)
        throws IOException, WebApplicationException {

        if (APPLICATION_BUNDLE_JSON_TYPE.isCompatible(mediaType)) {
            return jsonMapperFunction.apply(uriInfo).readerFor(type).readValue(entityStream);
        }
        else if (APPLICATION_BUNDLE_XML_TYPE.isCompatible(mediaType)) {
            try {
                return (BundleDTO)context.createUnmarshaller().unmarshal(
                    entityStream);
            } catch (JAXBException e) {
                throw new IOException(e);
            }
        }

        return null;
    }

    Collection<BundleDTO> readBundleDTOListFrom(
            @SuppressWarnings("rawtypes") Class<? extends Collection> type,
            Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
        throws IOException, WebApplicationException {

        if (APPLICATION_BUNDLES_REPRESENTATIONS_JSON_TYPE.isCompatible(mediaType)) {
            return jsonMapperFunction.apply(uriInfo).readerFor(BUNDLEDTO_COLLECTION).readValue(entityStream);
        }
        else if (APPLICATION_BUNDLES_REPRESENTATIONS_XML_TYPE.isCompatible(mediaType)) {
            List<BundleDTO> bundles = new ArrayList<>();
            AtomicReference<BundleDTO> bundle = new AtomicReference<>();
            deserializer.deserialize((start, next) -> {
                String el = start.getName().getLocalPart();
                XMLEvent event = start;
                if (el.equals("bundle")) {
                    bundle.set(new BundleDTO());
                }
                else if (el.equals("id")) {
                    try {event = next.nextEvent();} catch (Exception e) {throw new RuntimeException(e);}
                    bundle.get().id = Long.parseLong(event.asCharacters().getData());
                }
                else if (el.equals("lastModified")) {
                    try {event = next.nextEvent();} catch (Exception e) {throw new RuntimeException(e);}
                    bundle.get().lastModified = Long.parseLong(event.asCharacters().getData());
                }
                else if (el.equals("state")) {
                    try {event = next.nextEvent();} catch (Exception e) {throw new RuntimeException(e);}
                    bundle.get().state = Integer.parseInt(event.asCharacters().getData());
                }
                else if (el.equals("symbolicName")) {
                    try {event = next.nextEvent();} catch (Exception e) {throw new RuntimeException(e);}
                    bundle.get().symbolicName = event.asCharacters().getData();
                }
                else if (el.equals("version")) {
                    try {event = next.nextEvent();} catch (Exception e) {throw new RuntimeException(e);}
                    bundle.get().version = event.asCharacters().getData();
                }
                return event;
            }, end -> {
                String el = end.getName().getLocalPart();
                if (el.equals("bundle")) {
                    bundles.add(bundle.get());
                    bundle.set(null);
                }
            }, entityStream);

            return bundles;
        }

        return null;
    }

    Dictionary<String, String> readBundleHeadersFrom(
            @SuppressWarnings("rawtypes") Class<? extends Dictionary> asSubclass,
            Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
        throws IOException, WebApplicationException{

        if (APPLICATION_BUNDLEHEADER_JSON_TYPE.isCompatible(mediaType)) {
            return jsonMapperFunction.apply(uriInfo).readerFor(BUNDLE_HEADER).readValue(entityStream);
        }
        else if (APPLICATION_BUNDLEHEADER_XML_TYPE.isCompatible(mediaType)) {
            Dictionary<String, String> header = new Hashtable<>();

            deserializer.deserialize((start, next) -> {
                String el = start.getName().getLocalPart();
                if (el.equals("entry")) {
                    Attribute keyAT = start.getAttributeByName(new QName("key"));
                    Attribute valueAT = start.getAttributeByName(new QName("value"));
                    header.put(keyAT.getValue(), valueAT.getValue());
                }
                return start;
            }, end -> {}, entityStream);

            return header;
        }

        return null;
    }

    BundlesDTO readBundlesDTOFrom(
            Class<? extends BundlesDTO> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders,
            InputStream entityStream)
        throws IOException, WebApplicationException {

        if (APPLICATION_BUNDLES_JSON_TYPE.isCompatible(mediaType)) {
            return jsonMapperFunction.apply(uriInfo).readerFor(type).readValue(entityStream);
        }
        else if (APPLICATION_BUNDLES_XML_TYPE.isCompatible(mediaType)) {
            BundlesDTO bundlesDTO = new BundlesDTO();
            bundlesDTO.bundles = new ArrayList<>();

            deserializer.deserialize((start, next) -> {
                String el = start.getName().getLocalPart();
                XMLEvent event = start;
                if (el.equals("uri")) {
                    try {event = next.nextEvent();} catch (Exception e) {throw new RuntimeException(e);}
                    bundlesDTO.bundles.add(event.asCharacters().getData());
                }
                return start;
            }, end -> {}, entityStream);

            return bundlesDTO;
        }

        return null;
    }

    BundleStartLevelDTO readBundleStartLevelDTOFrom(
            Class<? extends BundleStartLevelDTO> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
        throws IOException, WebApplicationException {

        if (APPLICATION_BUNDLESTARTLEVEL_JSON_TYPE.isCompatible(mediaType)) {
            return jsonMapperFunction.apply(uriInfo).readerFor(type).readValue(entityStream);
        }
        else if (APPLICATION_BUNDLESTARTLEVEL_XML_TYPE.isCompatible(mediaType)) {
            try {
                return (BundleStartLevelDTO)context.createUnmarshaller().unmarshal(
                    entityStream);
            } catch (JAXBException e) {
                throw new IOException(e);
            }
        }

        return null;
    }

    BundleStateDTO readBundleStateDTOFrom(
            Class<? extends BundleStateDTO> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders,
            InputStream entityStream)
        throws IOException, WebApplicationException {

        if (APPLICATION_BUNDLESTATE_JSON_TYPE.isCompatible(mediaType)) {
            return jsonMapperFunction.apply(uriInfo).readerFor(type).readValue(entityStream);
        }
        else if (APPLICATION_BUNDLESTATE_XML_TYPE.isCompatible(mediaType)) {
            try {
                return (BundleStateDTO)context.createUnmarshaller().unmarshal(
                    entityStream);
            } catch (JAXBException e) {
                throw new IOException(e);
            }
        }

        return null;
    }

    FrameworkDTO readFrameworkDTOFrom(
            Class<? extends FrameworkDTO> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders,
            InputStream entityStream)
        throws IOException, WebApplicationException {

        if (APPLICATION_JSON_TYPE.isCompatible(mediaType)) {
            return jsonMapperFunction.apply(uriInfo).readerFor(type).readValue(entityStream);
        }
        else if (APPLICATION_XML_TYPE.isCompatible(mediaType)) {
            try {
                return (FrameworkDTO)context.createUnmarshaller().unmarshal(
                    entityStream);
            } catch (JAXBException e) {
                throw new IOException(e);
            }
        }

        return null;
    }

    FrameworkStartLevelDTO readFrameworkStartLevelDTOFrom(
            Class<? extends FrameworkStartLevelDTO> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders,
            InputStream entityStream)
        throws IOException, WebApplicationException {

        if (APPLICATION_FRAMEWORKSTARTLEVEL_JSON_TYPE.isCompatible(mediaType)) {
            return jsonMapperFunction.apply(uriInfo).readerFor(type).readValue(entityStream);
        }
        else if (APPLICATION_FRAMEWORKSTARTLEVEL_XML_TYPE.isCompatible(mediaType)) {
            try {
                return (FrameworkStartLevelDTO)context.createUnmarshaller().unmarshal(
                    entityStream);
            } catch (JAXBException e) {
                throw new IOException(e);
            }
        }

        return null;
    }

    @Override
    public Object readFrom(
            Class<Object> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders,
            InputStream entityStream)
        throws IOException, WebApplicationException {

        if (Collection.class.isAssignableFrom(type) &&
            nthTypeArgumentIs(0, genericType, BundleDTO.class)) {

            return readBundleDTOListFrom(
                type.asSubclass(Collection.class), genericType, annotations,
                mediaType, httpHeaders, entityStream);
        }
        else if (Collection.class.isAssignableFrom(type) &&
            nthTypeArgumentIs(0, genericType, ServiceReferenceDTO.class)) {

            return readServiceReferenceDTOListFrom(
                type.asSubclass(Collection.class), genericType, annotations,
                mediaType, httpHeaders, entityStream);
        }
        else if (Dictionary.class.isAssignableFrom(type) &&
            nthTypeArgumentIs(0, genericType, String.class) &&
            nthTypeArgumentIs(1, genericType, String.class)) {

            return readBundleHeadersFrom(
                type.asSubclass(Dictionary.class), genericType, annotations,
                mediaType, httpHeaders, entityStream);
        }
        else if (BundleDTO.class.equals(type)) {
            return readBundleDTOFrom(
                type.asSubclass(BundleDTO.class), genericType, annotations,
                mediaType, httpHeaders, entityStream);
        }
        else if (BundlesDTO.class.equals(type)) {
            return readBundlesDTOFrom(
                type.asSubclass(BundlesDTO.class), genericType, annotations,
                mediaType, httpHeaders, entityStream);
        }
        else if (BundleStateDTO.class.equals(type)) {
            return readBundleStateDTOFrom(
                type.asSubclass(BundleStateDTO.class), genericType, annotations,
                mediaType, httpHeaders, entityStream);
        }
        else if (BundleStartLevelDTO.class.equals(type)) {
            return readBundleStartLevelDTOFrom(
                type.asSubclass(BundleStartLevelDTO.class), genericType,
                annotations, mediaType, httpHeaders, entityStream);
        }
        else if (FrameworkStartLevelDTO.class.equals(type)) {
            return readFrameworkStartLevelDTOFrom(
                type.asSubclass(FrameworkStartLevelDTO.class), genericType,
                annotations, mediaType, httpHeaders, entityStream);
        }
        else if (FrameworkDTO.class.equals(type)) {
            return readFrameworkDTOFrom(
                type.asSubclass(FrameworkDTO.class), genericType, annotations,
                mediaType, httpHeaders, entityStream);
        }
        else if (ServicesDTO.class.equals(type)) {
            return readServicesDTOFrom(
                type.asSubclass(ServicesDTO.class), genericType, annotations,
                mediaType, httpHeaders, entityStream);
        }
        else if (ServiceReferenceDTO.class.equals(type)) {
            return readServiceReferenceDTOFrom(
                type.asSubclass(ServiceReferenceDTO.class), genericType, annotations,
                mediaType, httpHeaders, entityStream);
        }
        else if (ServiceReferenceDTOs.class.equals(type)) {
            return readServiceReferenceDTOsFrom(
                type.asSubclass(ServiceReferenceDTOs.class), genericType, annotations,
                mediaType, httpHeaders, entityStream);
        }

        return null;
    }

    ServiceReferenceDTO readServiceReferenceDTOFrom(
            Class<? extends ServiceReferenceDTO> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
        throws IOException, WebApplicationException {

        if (APPLICATION_SERVICE_JSON_TYPE.isCompatible(mediaType)) {
            return jsonMapperFunction.apply(uriInfo).readerFor(type).readValue(entityStream);
        }
        else if (APPLICATION_SERVICE_XML_TYPE.isCompatible(mediaType)) {
            return serviceReferenceDTO_XMLDeserializer.deserializeServiceReferenceDTO(entityStream);
        }

        return null;
    }

    Collection<ServiceReferenceDTO> readServiceReferenceDTOListFrom(
            @SuppressWarnings("rawtypes") Class<? extends Collection> type,
            Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
        throws IOException, WebApplicationException {

        if (APPLICATION_BUNDLES_REPRESENTATIONS_JSON_TYPE.isCompatible(mediaType)) {
            return jsonMapperFunction.apply(uriInfo).readerFor(SERVICEDTO_COLLECTION).readValue(entityStream);
        }
        else if (APPLICATION_BUNDLES_REPRESENTATIONS_XML_TYPE.isCompatible(mediaType)) {
            return serviceReferenceDTO_XMLDeserializer.deserializeServiceReferenceDTOList(entityStream);
        }

        return null;
    }

    ServiceReferenceDTOs readServiceReferenceDTOsFrom(
            Class<? extends ServiceReferenceDTOs> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
        throws IOException, WebApplicationException {

        if (APPLICATION_SERVICES_REPRESENTATIONS_JSON_TYPE.isCompatible(mediaType)) {
            return jsonMapperFunction.apply(uriInfo).readerFor(type).readValue(entityStream);
        }
        else if (APPLICATION_SERVICES_REPRESENTATIONS_XML_TYPE.isCompatible(mediaType)) {
            ServiceReferenceDTOs serviceReferenceDTOs = new ServiceReferenceDTOs();
            serviceReferenceDTOs.services = serviceReferenceDTO_XMLDeserializer.deserializeServiceReferenceDTOList(entityStream);
            return serviceReferenceDTOs;
        }

        return null;
    }

    ServicesDTO readServicesDTOFrom(
            Class<? extends ServicesDTO> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
        throws IOException, WebApplicationException{

        if (APPLICATION_SERVICES_JSON_TYPE.isCompatible(mediaType)) {
            return jsonMapperFunction.apply(uriInfo).readerFor(type).readValue(entityStream);
        }
        else if (APPLICATION_SERVICES_XML_TYPE.isCompatible(mediaType)) {
            ServicesDTO servicesDTO = new ServicesDTO();
            servicesDTO.services = new ArrayList<>();

            deserializer.deserialize((start, next) -> {
                String el = start.getName().getLocalPart();
                XMLEvent event = start;
                if (el.equals("uri")) {
                    try {event = next.nextEvent();} catch (Exception e) {throw new RuntimeException(e);}
                    servicesDTO.services.add(event.asCharacters().getData());
                }
                return start;
            }, end -> {}, entityStream);

            return servicesDTO;
        }

        return null;
    }

    void writeBundleDTOListTo(
            Collection<BundleDTO> list, Class<?> type, Type genericType,
            Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream)
        throws IOException, WebApplicationException {

        if (APPLICATION_BUNDLES_REPRESENTATIONS_JSON_TYPE.isCompatible(mediaType)) {
            jsonMapperFunction.apply(uriInfo).writeValue(entityStream, list);
        }
        else if (APPLICATION_BUNDLES_REPRESENTATIONS_XML_TYPE.isCompatible(mediaType)) {
            try {
                serializer.serialize(
                    "bundles",
                    (dom, rootEl) -> list.stream().map(
                        bundleDTO -> {
                            Element bundleEl = dom.createElement("bundle");

                            Element idEl = dom.createElement("id");
                            idEl.setTextContent(String.valueOf(bundleDTO.id));
                            Element lastModifiedEl = dom.createElement("lastModified");
                            lastModifiedEl.setTextContent(String.valueOf(bundleDTO.lastModified));
                            Element stateEl = dom.createElement("state");
                            stateEl.setTextContent(String.valueOf(bundleDTO.state));
                            Element symbolicNameEl = dom.createElement("symbolicName");
                            symbolicNameEl.setTextContent(bundleDTO.symbolicName);
                            Element versionEl = dom.createElement("version");
                            versionEl.setTextContent(bundleDTO.version);

                            bundleEl.appendChild(idEl);
                            bundleEl.appendChild(lastModifiedEl);
                            bundleEl.appendChild(stateEl);
                            bundleEl.appendChild(symbolicNameEl);
                            bundleEl.appendChild(versionEl);

                            return bundleEl;
                        }
                    ).forEach(
                        bundleEL -> rootEl.appendChild(bundleEL)
                    ),
                    entityStream
                );
            }
            catch (Exception e) {
                throw new IOException(e);
            }
        }
    }

    void writeBundleDTOTo(
            BundleDTO bundleDTO,
            Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream)
        throws IOException, WebApplicationException {

        if (APPLICATION_BUNDLE_JSON_TYPE.isCompatible(mediaType)) {
            jsonMapperFunction.apply(uriInfo).writeValue(entityStream, bundleDTO);
        }
        else if (APPLICATION_BUNDLE_XML_TYPE.isCompatible(mediaType)) {
            try {
                Marshaller marshaller = context.createMarshaller();
                marshaller.setProperty(JAXB_FORMATTED_OUTPUT, true);
                marshaller.marshal(
                    new JAXBElement<>(
                        new QName("bundle"),
                        BundleDTO.class,
                        bundleDTO),
                    entityStream);
            } catch (JAXBException e) {
                throw new IOException(e);
            }
        }
    }

    void writeBundleHeadersTo(
            Dictionary<String, String> headers, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream)
        throws IOException, WebApplicationException{

        if (APPLICATION_BUNDLEHEADER_JSON_TYPE.isCompatible(mediaType)) {
            jsonMapperFunction.apply(uriInfo).writeValue(entityStream, headers);
        }
        else if (APPLICATION_BUNDLEHEADER_XML_TYPE.isCompatible(mediaType)) {
            try {
                serializer.serialize(
                    "bundleHeader",
                    (dom, rootEl) -> Collections.list(
                        headers.keys()
                    ).stream().forEach(
                        key -> {
                            Element entryEl = dom.createElement("entry");
                            entryEl.setAttribute("key", key);
                            entryEl.setAttribute("value", headers.get(key));
                            rootEl.appendChild(entryEl);
                        }
                    ),
                    entityStream
                );
            }
            catch (Exception e) {
                throw new IOException(e);
            }
        }
    }

    void writeBundlesDTOTo(
            BundlesDTO bundlesDTO,
            Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream)
        throws IOException, WebApplicationException {

        if (APPLICATION_BUNDLES_JSON_TYPE.isCompatible(mediaType)) {
            jsonMapperFunction.apply(uriInfo).writeValue(entityStream, bundlesDTO);
        }
        else if (APPLICATION_BUNDLES_XML_TYPE.isCompatible(mediaType)) {
            try {
                serializer.serialize(
                    "bundles",
                    (dom, rootEl) -> bundlesDTO.bundles.stream().forEach(
                        key -> {
                            Element entryEl = dom.createElement("uri");
                            entryEl.setTextContent(key);
                            rootEl.appendChild(entryEl);
                        }
                    ),
                    entityStream
                );
            }
            catch (Exception e) {
                throw new IOException(e);
            }
        }
    }

    void writeBundleStartLevelDTOTo(
            BundleStartLevelDTO bundleStartLevelDTO, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
        throws IOException, WebApplicationException {

        if (APPLICATION_BUNDLESTARTLEVEL_JSON_TYPE.isCompatible(mediaType)) {
            jsonMapperFunction.apply(uriInfo).writeValue(entityStream, bundleStartLevelDTO);
        }
        else if (APPLICATION_BUNDLESTARTLEVEL_XML_TYPE.isCompatible(mediaType)) {
            try {
                Marshaller marshaller = context.createMarshaller();
                marshaller.setProperty(JAXB_FORMATTED_OUTPUT, true);
                marshaller.marshal(
                    new JAXBElement<>(
                        new QName("bundleStartLevel"),
                        BundleStartLevelDTO.class,
                        bundleStartLevelDTO),
                    entityStream);
            } catch (JAXBException e) {
                throw new IOException(e);
            }
        }
    }

    void writeBundleStateDTOTo(
            BundleStateDTO bundleStateDTO,
            Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream)
        throws IOException, WebApplicationException {

        if (APPLICATION_BUNDLESTATE_JSON_TYPE.isCompatible(mediaType)) {
            jsonMapperFunction.apply(uriInfo).writeValue(entityStream, bundleStateDTO);
        }
        else if (APPLICATION_BUNDLESTATE_XML_TYPE.isCompatible(mediaType)) {
            try {
                Marshaller marshaller = context.createMarshaller();
                marshaller.setProperty(JAXB_FORMATTED_OUTPUT, true);
                marshaller.marshal(
                    new JAXBElement<>(
                        new QName("bundleState"),
                        BundleStateDTO.class,
                        bundleStateDTO),
                    entityStream);
            } catch (JAXBException e) {
                throw new IOException(e);
            }
        }
    }

    void writeFrameworkDTOTo(
            FrameworkDTO frameworkDTO,
            Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream)
        throws IOException, WebApplicationException {

        if (APPLICATION_JSON_TYPE.isCompatible(mediaType)) {
            jsonMapperFunction.apply(uriInfo).writeValue(entityStream, frameworkDTO);
        }
        else if (APPLICATION_XML_TYPE.isCompatible(mediaType)) {
            try {
                Marshaller marshaller = context.createMarshaller();
                marshaller.setProperty(JAXB_FORMATTED_OUTPUT, true);
                marshaller.marshal(
                    new JAXBElement<>(
                        new QName("framework"),
                        FrameworkDTO.class,
                        frameworkDTO),
                    entityStream);
            } catch (JAXBException e) {
                throw new IOException(e);
            }
        }
    }

    void writeFrameworkStartLevelDTOTo(
            FrameworkStartLevelDTO frameworkStartLevelDTO,
            Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream)
        throws IOException, WebApplicationException {

        if (APPLICATION_FRAMEWORKSTARTLEVEL_JSON_TYPE.isCompatible(mediaType)) {
            jsonMapperFunction.apply(uriInfo).writeValue(entityStream, frameworkStartLevelDTO);
        }
        else if (APPLICATION_FRAMEWORKSTARTLEVEL_XML_TYPE.isCompatible(mediaType)) {
            try {
                Marshaller marshaller = context.createMarshaller();
                marshaller.setProperty(JAXB_FORMATTED_OUTPUT, true);
                marshaller.marshal(
                    new JAXBElement<>(
                        new QName("frameworkStartLevel"),
                        FrameworkStartLevelDTO.class,
                        frameworkStartLevelDTO),
                    entityStream);
            } catch (JAXBException e) {
                throw new IOException(e);
            }
        }
    }

    void writeServiceReferenceDTOTo(
            ServiceReferenceDTO serviceReferenceDTO, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream)
        throws IOException, WebApplicationException {

        if (APPLICATION_SERVICE_JSON_TYPE.isCompatible(mediaType)) {
            jsonMapperFunction.apply(uriInfo).writeValue(entityStream, serviceReferenceDTO);
        }
        else if (APPLICATION_SERVICE_XML_TYPE.isCompatible(mediaType)) {
            serviceReferenceDTO_XMLSerializer.serialize(serviceReferenceDTO, uriInfo, entityStream);
        }
    }

    void writeServiceReferenceDTOListTo(
            Collection<ServiceReferenceDTO> list, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream)
        throws IOException, WebApplicationException {

        if (APPLICATION_SERVICES_REPRESENTATIONS_JSON_TYPE.isCompatible(mediaType)) {
            jsonMapperFunction.apply(uriInfo).writeValue(entityStream, list);
        }
        else if (APPLICATION_SERVICES_REPRESENTATIONS_XML_TYPE.isCompatible(mediaType)) {
            serviceReferenceDTO_XMLSerializer.serialize(list, uriInfo, entityStream);
        }
    }

    void writeServiceReferenceDTOsTo(
            ServiceReferenceDTOs serviceReferenceDTOs, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
        throws IOException, WebApplicationException {

        if (APPLICATION_SERVICES_REPRESENTATIONS_JSON_TYPE.isCompatible(mediaType)) {
            jsonMapperFunction.apply(uriInfo).writeValue(entityStream, serviceReferenceDTOs);
        }
        else if (APPLICATION_SERVICES_REPRESENTATIONS_XML_TYPE.isCompatible(mediaType)) {
            serviceReferenceDTO_XMLSerializer.serialize(
                serviceReferenceDTOs.services, uriInfo, entityStream);
        }
    }

    void writeServicesDTOTo(
            ServicesDTO servicesDTO, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
        throws IOException, WebApplicationException {

        if (APPLICATION_SERVICES_JSON_TYPE.isCompatible(mediaType)) {
            jsonMapperFunction.apply(uriInfo).writeValue(entityStream, servicesDTO);
        }
        else if (APPLICATION_SERVICES_XML_TYPE.isCompatible(mediaType)) {
            try {
                serializer.serialize(
                    "services",
                    (dom, rootEl) -> servicesDTO.services.stream().forEach(
                        key -> {
                            Element entryEl = dom.createElement("uri");
                            entryEl.setTextContent(key);
                            rootEl.appendChild(entryEl);
                        }
                    ),
                    entityStream
                );
            }
            catch (Exception e) {
                throw new IOException(e);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void writeTo(
            Object entity,
            Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream)
        throws IOException, WebApplicationException {

        if (Collection.class.isAssignableFrom(type) &&
            nthTypeArgumentIs(0, genericType, BundleDTO.class)) {

            writeBundleDTOListTo(
                Collection.class.cast(entity), type, genericType, annotations,
                mediaType, httpHeaders, entityStream);
        }
        else if (Collection.class.isAssignableFrom(type) &&
            nthTypeArgumentIs(0, genericType, ServiceReferenceDTO.class)) {

            writeServiceReferenceDTOListTo(
                Collection.class.cast(entity), type, genericType, annotations,
                mediaType, httpHeaders, entityStream);
        }
        else if (Dictionary.class.isAssignableFrom(type) &&
            nthTypeArgumentIs(0, genericType, String.class) &&
            nthTypeArgumentIs(1, genericType, String.class)) {

            writeBundleHeadersTo(
                Dictionary.class.cast(entity), type, genericType, annotations,
                mediaType, httpHeaders, entityStream);
        }
        else if (type == BundleDTO.class) {
            writeBundleDTOTo(
                BundleDTO.class.cast(entity), type, genericType, annotations,
                mediaType, httpHeaders, entityStream);
        }
        else if (BundlesDTO.class.equals(type)) {
            writeBundlesDTOTo(
                BundlesDTO.class.cast(entity), type, genericType, annotations,
                mediaType, httpHeaders, entityStream);
        }
        else if (BundleStateDTO.class.equals(type)) {
            writeBundleStateDTOTo(
                BundleStateDTO.class.cast(entity), type, genericType,
                annotations, mediaType, httpHeaders, entityStream);
        }
        else if (BundleStartLevelDTO.class.equals(type)) {
            writeBundleStartLevelDTOTo(
                BundleStartLevelDTO.class.cast(entity), type, genericType,
                annotations, mediaType, httpHeaders, entityStream);
        }
        else if (FrameworkStartLevelDTO.class.equals(type)) {
            writeFrameworkStartLevelDTOTo(
                FrameworkStartLevelDTO.class.cast(entity), type, genericType,
                annotations, mediaType, httpHeaders, entityStream);
        }
        else if (FrameworkDTO.class.equals(type)) {
            writeFrameworkDTOTo(
                FrameworkDTO.class.cast(entity), type, genericType, annotations,
                mediaType, httpHeaders, entityStream);
        }
        else if (ServiceReferenceDTO.class.equals(type)) {
            writeServiceReferenceDTOTo(
                ServiceReferenceDTO.class.cast(entity), type, genericType, annotations,
                mediaType, httpHeaders, entityStream);
        }
        else if (ServicesDTO.class.equals(type)) {
            writeServicesDTOTo(
                ServicesDTO.class.cast(entity), type, genericType, annotations,
                mediaType, httpHeaders, entityStream);
        }
        else if (ServiceReferenceDTOs.class.equals(type)) {
            writeServiceReferenceDTOsTo(
                ServiceReferenceDTOs.class.cast(entity), type, genericType, annotations,
                mediaType, httpHeaders, entityStream);
        }
    }

    private boolean nthTypeArgumentIs(int n, Type type, Class<?> clazz) {
        return ((ParameterizedType)type).getActualTypeArguments()[n].equals(clazz);
    }

}
