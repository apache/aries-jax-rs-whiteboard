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

package org.apache.aries.jax.rs.whiteboard.internal;

import org.apache.aries.jax.rs.whiteboard.internal.Utils.PropertyHolder;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jaxrs.runtime.JaxRSServiceRuntime;
import org.osgi.service.jaxrs.runtime.dto.ApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.DTOConstants;
import org.osgi.service.jaxrs.runtime.dto.ExtensionDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedExtensionDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedResourceDTO;
import org.osgi.service.jaxrs.runtime.dto.RequestInfoDTO;
import org.osgi.service.jaxrs.runtime.dto.ResourceDTO;
import org.osgi.service.jaxrs.runtime.dto.RuntimeDTO;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;

import javax.ws.rs.core.Application;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static org.apache.aries.jax.rs.whiteboard.internal.Utils.generateApplicationName;
import static org.apache.aries.jax.rs.whiteboard.internal.Whiteboard.DEFAULT_NAME;
import static org.apache.aries.jax.rs.whiteboard.internal.Whiteboard.getApplicationBase;
import static org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants.JAX_RS_NAME;

public class AriesJaxRSServiceRuntime implements JaxRSServiceRuntime {

    private static final long serialVersionUID = 1L;

    private ConcurrentHashMap<String, Map<String, Object>>
        _applications = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String, Collection<ServiceReference<?>>>
        _applicationEndpoints = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String, Collection<ServiceReference<?>>>
        _applicationExtensions = new ConcurrentHashMap<>();

    private Collection<ServiceReference<Application>>
        _ungettableApplications = new CopyOnWriteArrayList<>();

    private Collection<ServiceReference<Application>> _shadowedApplications =
        new CopyOnWriteArrayList<>();

    private Collection<ServiceReference<Application>> _erroredApplications =
        new CopyOnWriteArrayList<>();

    private Collection<ServiceReference<?>> _ungettableEndpoints =
        new CopyOnWriteArrayList<>();
    private Collection<ServiceReference<?>> _ungettableExtensions =
        new CopyOnWriteArrayList<>();

    private Set<ServiceReference<?>> _dependentServices =
        ConcurrentHashMap.newKeySet();

    private Collection<ServiceReference<?>> _invalidExtensions =
        new CopyOnWriteArrayList<>();

    private volatile Map<String, Object> _defaultApplicationProperties;

    public void addApplicationEndpoint(
        String applicationName, ServiceReference<?> endpointServiceReference) {

        _applicationEndpoints.compute(
            applicationName, merger(endpointServiceReference));
    }

    public void addApplicationExtension(
        String applicationName,
        ServiceReference<?> extensionServiceReference) {

        _applicationExtensions.compute(
            applicationName, merger(extensionServiceReference));
    }

    public void addDependentService(ServiceReference<?> serviceReference) {
        _dependentServices.add(serviceReference);
    }

    public void addErroredApplication(
        ServiceReference<Application> serviceReference) {

        _erroredApplications.add(serviceReference);
    }

    public void addInvalidExtension(ServiceReference<?> serviceReference) {
        _invalidExtensions.add(serviceReference);
    }

    public boolean addNotGettableApplication(
        ServiceReference<Application> serviceReference) {

        return _ungettableApplications.add(serviceReference);
    }

    public <T> boolean addNotGettableEndpoint(
        ServiceReference<T> serviceReference) {

        return _ungettableEndpoints.add(serviceReference);
    }

    public <T> void addNotGettableExtension(
        ServiceReference<T> serviceReference) {

        _ungettableExtensions.add(serviceReference);
    }

    public boolean addShadowedApplication(
        ServiceReference<Application> serviceReference) {

        return _shadowedApplications.add(serviceReference);
    }

    public void clearDefaultApplication() {
        _applications.compute(DEFAULT_NAME, (__, ___) -> {
            _defaultApplicationProperties = Collections.emptyMap();

            return null;
        });
    }

    public static String getApplicationName(PropertyHolder properties) {
        Object property = properties.get(JAX_RS_NAME);

        if (property == null) {
            return generateApplicationName(properties);
        }

        return property.toString();
    }

    @Override
    public RuntimeDTO getRuntimeDTO() {
        RuntimeDTO runtimeDTO = new RuntimeDTO();

        runtimeDTO.defaultApplication = buildApplicationDTO(
            _defaultApplicationProperties);

        runtimeDTO.applicationDTOs = applicationDTOStream().
            toArray(
                ApplicationDTO[]::new
            );

        runtimeDTO.failedApplicationDTOs = Stream.concat(
            shadowedApplicationsDTOStream(),
            Stream.concat(
                unreferenciableApplicationsDTOStream(),
                erroredApplicationsDTOStream())
            ).toArray(
                FailedApplicationDTO[]::new
            );

        runtimeDTO.failedResourceDTOs =
            Stream.concat(
                unreferenciableEndpointsDTOStream(), dependentServiceStreamDTO()
            ).toArray(
                FailedResourceDTO[]::new
            );

        runtimeDTO.failedExtensionDTOs = Stream.concat(
                unreferenciableExtensionsDTOStream(),
                invalidExtensionsDTOStream()
            ).toArray(
                FailedExtensionDTO[]::new
            );

        return runtimeDTO;
    }

    @Override
    public RequestInfoDTO calculateRequestInfoDTO(String path) {
        return null;
    }

    public void removeApplicationEndpoint(
        String applicationName, ServiceReference<?> endpointServiceReference) {

        _applicationEndpoints.compute(
            applicationName, remover(endpointServiceReference));
    }

    public void removeApplicationExtension(
        String applicationName, ServiceReference<?> extensionServiceReference) {

        _applicationExtensions.computeIfPresent(
            applicationName, remover(extensionServiceReference));
    }

    public void removeDependentService(ServiceReference<?> serviceReference) {
        _dependentServices.remove(serviceReference);
    }

    public void removeErroredApplication(
        ServiceReference<Application> serviceReference) {

        _erroredApplications.remove(serviceReference);
    }

    public void removeInvalidExtension(ServiceReference<?> serviceReference) {
        _invalidExtensions.remove(serviceReference);
    }

    public boolean removeNotGettableApplication(
        ServiceReference<Application> serviceReference) {

        return _ungettableApplications.remove(serviceReference);
    }

    public <T> boolean removeNotGettableEndpoint(
        ServiceReference<T> serviceReference) {

        return _ungettableEndpoints.remove(serviceReference);
    }

    public <T> void removeNotGettableExtension(
        ServiceReference<T> serviceReference) {

        _ungettableExtensions.remove(serviceReference);
    }

    public boolean removeShadowedApplication(
        ServiceReference<Application> serviceReference) {

        return _shadowedApplications.remove(serviceReference);
    }

    public Map<String, Object> setApplicationForPath(
        String path, Map<String, Object> properties) {

        return _applications.put(path, properties);
    }

    public void setDefaultApplication(Map<String, Object> properties) {
        _applications.compute(DEFAULT_NAME, (__, ___) -> {
            _defaultApplicationProperties = properties;

            return properties;
        });
    }

    public Map<String, Object> unsetApplicationForPath(String path) {
        return _applications.remove(path);
    }

    private Stream<ApplicationDTO> applicationDTOStream() {
        return _applications.values().stream().
            filter(p -> !(".default".equals(p.get(JAX_RS_NAME)))).
            map(
                this::buildApplicationDTO
            );
    }

    private ApplicationDTO buildApplicationDTO(
        Map<String, Object> properties) {

        ApplicationDTO applicationDTO = new ApplicationDTO(){};

        applicationDTO.name = getApplicationName(properties::get);
        applicationDTO.base = getApplicationBase(properties::get);
        applicationDTO.serviceId = (Long)properties.get("service.id");

        applicationDTO.resourceDTOs = getApplicationEndpointsStream(
            applicationDTO.name).toArray(
                ResourceDTO[]::new
            );

        applicationDTO.extensionDTOs = getApplicationExtensionsStream(
            applicationDTO.name).toArray(
                ExtensionDTO[]::new
            );

        return applicationDTO;
    }

    private FailedExtensionDTO buildFailedExtensionDTO(
        int reason, ServiceReference<?> serviceReference) {

        FailedExtensionDTO failedExtensionDTO = new FailedExtensionDTO();

        populateExtensionDTO(failedExtensionDTO, serviceReference);

        failedExtensionDTO.failureReason = reason;

        return failedExtensionDTO;
    }

    private FailedResourceDTO buildFailedResourceDTO(
        int reason, ServiceReference<?> serviceReference) {

        FailedResourceDTO failedResourceDTO = new FailedResourceDTO();

        populateResourceDTO(failedResourceDTO, serviceReference);

        failedResourceDTO.failureReason = reason;

        return failedResourceDTO;
    }

    private Stream<FailedResourceDTO> dependentServiceStreamDTO() {
        return _dependentServices.stream().map(
            sr -> buildFailedResourceDTO(
                DTOConstants.FAILURE_REASON_REQUIRED_EXTENSIONS_UNAVAILABLE,
                sr));
    }

    private Stream<FailedApplicationDTO> erroredApplicationsDTOStream() {
        return _erroredApplications.stream().map(
            sr -> buildFailedApplicationDTO(
                DTOConstants.FAILURE_REASON_UNKNOWN, sr)
        );
    }

    private Stream<ResourceDTO> getApplicationEndpointsStream(String name) {
        Collection<ServiceReference<?>> applicationEndpoints =
            _applicationEndpoints.get(name);

        Stream<ServiceReference<?>> applicationEndpointStream =
            applicationEndpoints != null ?
                applicationEndpoints.stream() :
                Stream.empty();

        return
            applicationEndpointStream.map(
                sr -> populateResourceDTO(new ResourceDTO(){}, sr)
            );
    }

    private Stream<ExtensionDTO> getApplicationExtensionsStream(String name) {
        Collection<ServiceReference<?>> applicationExtensions =
            _applicationExtensions.get(name);

        Stream<ServiceReference<?>> applicationExtensionStream =
            applicationExtensions != null ?
                applicationExtensions.stream() :
                Stream.empty();

        return
            applicationExtensionStream.map(
                sr -> populateExtensionDTO(new ExtensionDTO(){}, sr)
            );
    }

    private Stream<FailedExtensionDTO> invalidExtensionsDTOStream() {
        return _invalidExtensions.stream().map(
            sr -> buildFailedExtensionDTO(
                DTOConstants.FAILURE_REASON_NOT_AN_EXTENSION_TYPE, sr)
        );
    }

    private Stream<FailedApplicationDTO> shadowedApplicationsDTOStream() {
        return _shadowedApplications.stream().
            map(sr -> buildFailedApplicationDTO(
                DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE, sr)
        );
    }

    private Stream<FailedApplicationDTO> unreferenciableApplicationsDTOStream() {
        return _ungettableApplications.stream().
            map(
                sr -> buildFailedApplicationDTO(
                    DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE, sr)
        );
    }

    private Stream<FailedResourceDTO> unreferenciableEndpointsDTOStream() {
        return _ungettableEndpoints.stream().map(
            sr -> buildFailedResourceDTO(
                DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE, sr));
    }

    private Stream<FailedExtensionDTO> unreferenciableExtensionsDTOStream() {
        return _ungettableExtensions.stream().map(
            sr -> buildFailedExtensionDTO(
                DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE, sr)
        );
    }

    private static FailedApplicationDTO buildFailedApplicationDTO(
        int reason, ServiceReference<Application> serviceReference) {

        FailedApplicationDTO failedApplicationDTO = new FailedApplicationDTO();

        Object nameProperty = serviceReference.getProperty(
            JaxRSWhiteboardConstants.JAX_RS_NAME);

        failedApplicationDTO.name = nameProperty == null ?
            generateApplicationName(serviceReference::getProperty) :
            nameProperty.toString();

        failedApplicationDTO.failureReason = reason;

        return failedApplicationDTO;
    }

    private static <T> BiFunction<String, Collection<T>, Collection<T>> merger(
        T t) {

        return (__, collection) -> {
            if (collection == null) {
                collection = new ArrayList<>();
            }

            collection.add(t);

            return collection;
        };
    }

    private static ExtensionDTO populateExtensionDTO(
        ExtensionDTO extensionDTO, ServiceReference<?> serviceReference) {

        extensionDTO.name = serviceReference.getProperty(JAX_RS_NAME).
            toString();
        extensionDTO.serviceId = (Long)serviceReference.getProperty(
            "service.id");

        return extensionDTO;
    }

    private static ResourceDTO populateResourceDTO(
        ResourceDTO resourceDTO, ServiceReference<?> serviceReference) {

        resourceDTO.name = getApplicationName(serviceReference::getProperty);
        resourceDTO.serviceId = (Long)serviceReference.getProperty(
            "service.id");

        return resourceDTO;
    }

    private static <T> BiFunction<String, Collection<T>, Collection<T>> remover(
        T t) {

        return (__, collection) -> {
            if (collection != null) {
                collection.remove(t);

                if (collection.isEmpty()) {
                    return null;
                }
            }

            return collection;
        };
    }

}