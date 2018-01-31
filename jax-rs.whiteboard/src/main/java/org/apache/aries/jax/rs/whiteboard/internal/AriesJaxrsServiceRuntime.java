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

import static org.apache.aries.jax.rs.whiteboard.internal.Utils.canonicalize;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.generateApplicationName;
import static org.apache.aries.jax.rs.whiteboard.internal.Whiteboard.DEFAULT_NAME;
import static org.apache.aries.jax.rs.whiteboard.internal.Whiteboard.SUPPORTED_EXTENSION_INTERFACES;
import static org.apache.aries.jax.rs.whiteboard.internal.Whiteboard.getApplicationBase;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import javax.ws.rs.core.Application;

import org.apache.aries.jax.rs.whiteboard.internal.Utils.PropertyHolder;
import org.apache.aries.jax.rs.whiteboard.internal.introspection.ClassIntrospector;
import org.apache.aries.osgi.functional.CachingServiceReference;
import org.apache.cxf.Bus;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;
import org.osgi.service.jaxrs.runtime.dto.ApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.BaseDTO;
import org.osgi.service.jaxrs.runtime.dto.BaseExtensionDTO;
import org.osgi.service.jaxrs.runtime.dto.DTOConstants;
import org.osgi.service.jaxrs.runtime.dto.ExtensionDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedExtensionDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedResourceDTO;
import org.osgi.service.jaxrs.runtime.dto.ResourceDTO;
import org.osgi.service.jaxrs.runtime.dto.ResourceMethodInfoDTO;
import org.osgi.service.jaxrs.runtime.dto.RuntimeDTO;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AriesJaxrsServiceRuntime implements JaxrsServiceRuntime {

    private static final long serialVersionUID = 1L;
    private static final Logger _LOGGER = LoggerFactory.getLogger(
        Whiteboard.class);

    public static String getApplicationName(PropertyHolder properties) {
        Object property = properties.get(JAX_RS_NAME);

        if (property == null) {
            return generateApplicationName(properties);
        }

        return property.toString();
    }

    public void addApplicationDependentExtension(
        CachingServiceReference<?> cachingServiceReference) {

        _applicationDependentExtensions.add(cachingServiceReference);
    }

    public void addApplicationDependentResource(
        CachingServiceReference<?> cachingServiceReference) {

        _applicationDependentResources.add(cachingServiceReference);
    }

    public void addApplicationEndpoint(
        String applicationName,
        CachingServiceReference<?> endpointImmutableServiceReference,
        Bus bus, Class<?> theClass) {

        _applicationEndpoints.compute(
            applicationName,
            merger(
                new EndpointRuntimeInformation(
                    endpointImmutableServiceReference, bus, theClass)));
    }

    public void addApplicationExtension(
        String applicationName,
        CachingServiceReference<?> extensionImmutableServiceReference) {

        _applicationExtensions.compute(
            applicationName, merger(extensionImmutableServiceReference));
    }

    public void addClashingApplication(
        CachingServiceReference<Application> serviceReference) {

        _clashingApplications.add(serviceReference);
    }

    public void addDependentApplication(
        CachingServiceReference<Application> applicationReference) {

        _dependentApplications.add(applicationReference);
    }

    public void addDependentExtension(
        CachingServiceReference<?> cachingServiceReference) {

        _dependentExtensions.add(cachingServiceReference);
    }

    public void addDependentService(CachingServiceReference<?> serviceReference) {
        _dependentServices.add(serviceReference);
    }

    public void addErroredApplication(
        CachingServiceReference<Application> serviceReference) {

        _erroredApplications.add(serviceReference);
    }

    public <T> void addErroredEndpoint(CachingServiceReference<T> serviceReference) {
        _erroredEndpoints.add(serviceReference);
    }

    public void addErroredExtension(
        CachingServiceReference<?> cachingServiceReference) {

        _erroredExtensions.add(cachingServiceReference);
    }

    public void addInvalidExtension(
        CachingServiceReference<?> serviceReference) {

        _invalidExtensions.add(serviceReference);
    }

    public boolean addNotGettableApplication(
        CachingServiceReference<Application> serviceReference) {

        if (_LOGGER.isWarnEnabled()) {
            _LOGGER.warn(
                "Application from reference " + serviceReference +
                    " can't be got");
        }

        return _ungettableApplications.add(serviceReference);
    }

    public <T> boolean addNotGettableEndpoint(
        CachingServiceReference<T> serviceReference) {

        if (_LOGGER.isWarnEnabled()) {
            _LOGGER.warn(
                "Resource from reference " + serviceReference +
                    " can't be got");
        }

        return _ungettableEndpoints.add(serviceReference);
    }

    public <T> void addNotGettableExtension(
        CachingServiceReference<T> serviceReference) {

        if (_LOGGER.isWarnEnabled()) {
            _LOGGER.warn(
                "Extension from reference " + serviceReference +
                    " can't be got");
        }

        _ungettableExtensions.add(serviceReference);
    }

    public boolean addShadowedApplication(
        CachingServiceReference<Application> serviceReference) {

        return _shadowedApplications.add(serviceReference);
    }

    @Override
    public RuntimeDTO getRuntimeDTO() {
        RuntimeDTO runtimeDTO = new RuntimeDTO();

        if (_defaultApplicationProperties != null) {
            runtimeDTO.defaultApplication = buildApplicationDTO(
                _defaultApplicationProperties);
        }

        runtimeDTO.applicationDTOs = applicationDTOStream().
            toArray(
                ApplicationDTO[]::new
            );

        runtimeDTO.failedApplicationDTOs = Stream.concat(
            shadowedApplicationsDTOStream(),
            Stream.concat(
                unreferenciableApplicationsDTOStream(),
                Stream.concat(
                    clashingApplicationsDTOStream(),
                    Stream.concat(
                        dependentApplicationsDTOStream(),
                        erroredApplicationsDTOStream())))
            ).toArray(
                FailedApplicationDTO[]::new
            );

        runtimeDTO.failedResourceDTOs =
            Stream.concat(
                unreferenciableEndpointsDTOStream(),
                Stream.concat(
                    dependentServiceStreamDTO(),
                    Stream.concat(
                        applicationDependentResourcesDTOStream(),
                        erroredEndpointsStreamDTO()))
            ).toArray(
                FailedResourceDTO[]::new
            );

        runtimeDTO.failedExtensionDTOs = Stream.concat(
                unreferenciableExtensionsDTOStream(),
                Stream.concat(
                    applicationDependentExtensionsDTOStream(),
                    Stream.concat(
                        erroredExtensionsDTOStream(),
                        Stream.concat(dependentExtensionsStreamDTO(),
                            invalidExtensionsDTOStream())))
            ).toArray(
                FailedExtensionDTO[]::new
            );

        return runtimeDTO;
    }

    public void removeApplicationDependentExtension(
        CachingServiceReference<?> cachingServiceReference) {

        _applicationDependentExtensions.remove(cachingServiceReference);
    }

    public void removeApplicationDependentResource(
        CachingServiceReference<?> cachingServiceReference) {

        _applicationDependentResources.remove(cachingServiceReference);
    }

    public void removeApplicationEndpoint(
        String applicationName,
        CachingServiceReference<?> cachingServiceReference) {

        _applicationEndpoints.compute(
            applicationName,
            remover(new EndpointRuntimeInformation(
                cachingServiceReference, null, null)));
    }

    public void removeApplicationExtension(
        String applicationName, CachingServiceReference<?> extensionImmutableServiceReference) {

        _applicationExtensions.computeIfPresent(
            applicationName, remover(extensionImmutableServiceReference));
    }

    public void removeClashingApplication(
        CachingServiceReference<Application> serviceReference) {

        _clashingApplications.remove(serviceReference);
    }

    public void removeDependentApplication(
        CachingServiceReference<Application> applicationReference) {

        _dependentApplications.remove(applicationReference);
    }

    public void removeDependentExtension(
        CachingServiceReference<?> cachingServiceReference) {

        _dependentExtensions.add(cachingServiceReference);
    }

    public void removeDependentService(CachingServiceReference<?> serviceReference) {
        _dependentServices.remove(serviceReference);
    }

    public void removeErroredApplication(
        CachingServiceReference<Application> serviceReference) {

        _erroredApplications.remove(serviceReference);
    }

    public <T> void removeErroredEndpoint(CachingServiceReference<T> serviceReference) {
        _erroredEndpoints.remove(serviceReference);
    }

    public void removeErroredExtension(
        CachingServiceReference<?> cachingServiceReference) {

        _erroredExtensions.remove(cachingServiceReference);
    }

    public void removeInvalidExtension(CachingServiceReference<?> serviceReference) {
        _invalidExtensions.remove(serviceReference);
    }

    public boolean removeNotGettableApplication(
        CachingServiceReference<Application> serviceReference) {

        return _ungettableApplications.remove(serviceReference);
    }

    public <T> boolean removeNotGettableEndpoint(
        CachingServiceReference<T> serviceReference) {

        return _ungettableEndpoints.remove(serviceReference);
    }

    public <T> void removeNotGettableExtension(
        CachingServiceReference<T> serviceReference) {

        _ungettableExtensions.remove(serviceReference);
    }

    public boolean removeShadowedApplication(
        CachingServiceReference<Application> serviceReference) {

        return _shadowedApplications.remove(serviceReference);
    }

    public ApplicationRuntimeInformation setApplicationForPath(
        String path,
        CachingServiceReference<Application> serviceReference,
        CxfJaxrsServiceRegistrator cxfJaxRsServiceRegistrator) {

        ApplicationRuntimeInformation ari = new ApplicationRuntimeInformation(
            serviceReference, cxfJaxRsServiceRegistrator);

        return _applications.compute(
            path,
            (__, prop) -> {
                if (DEFAULT_NAME.equals(
                    getApplicationName(
                        ari._cachingServiceReference::getProperty))) {

                    _defaultApplicationProperties = ari;
                }

                return ari;
            });
    }

    public ApplicationRuntimeInformation unsetApplicationForPath(String path) {
        return _applications.remove(path);
    }
    private ConcurrentHashMap<String, ApplicationRuntimeInformation>
        _applications = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Collection<EndpointRuntimeInformation>>
        _applicationEndpoints = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Collection<CachingServiceReference<?>>>
        _applicationExtensions = new ConcurrentHashMap<>();
    private Collection<CachingServiceReference<Application>>
        _ungettableApplications = new CopyOnWriteArrayList<>();
    private Collection<CachingServiceReference<Application>> _shadowedApplications =
        new CopyOnWriteArrayList<>();
    private Set<CachingServiceReference<?>> _applicationDependentExtensions =
        ConcurrentHashMap.newKeySet();
    private Set<CachingServiceReference<?>> _applicationDependentResources =
        ConcurrentHashMap.newKeySet();
    private Collection<CachingServiceReference<Application>> _clashingApplications =
        new CopyOnWriteArrayList<>();
    private Set<CachingServiceReference<Application>> _dependentApplications =
        ConcurrentHashMap.newKeySet();
    private Set<CachingServiceReference<?>> _dependentExtensions =
        ConcurrentHashMap.newKeySet();
    private Set<CachingServiceReference<?>> _dependentServices =
        ConcurrentHashMap.newKeySet();
    private Collection<CachingServiceReference<Application>> _erroredApplications =
        new CopyOnWriteArrayList<>();
    private Collection<CachingServiceReference<?>> _erroredEndpoints =
        new CopyOnWriteArrayList<>();
    private Collection<CachingServiceReference<?>> _erroredExtensions =
        new CopyOnWriteArrayList<>();
    private Collection<CachingServiceReference<?>> _ungettableEndpoints =
        new CopyOnWriteArrayList<>();
    private Collection<CachingServiceReference<?>> _ungettableExtensions =
        new CopyOnWriteArrayList<>();
    private Collection<CachingServiceReference<?>> _invalidExtensions =
        new CopyOnWriteArrayList<>();
    private volatile ApplicationRuntimeInformation _defaultApplicationProperties;

    private static FailedApplicationDTO buildFailedApplicationDTO(
        int reason, CachingServiceReference<Application> serviceReference) {

        FailedApplicationDTO failedApplicationDTO = new FailedApplicationDTO();

        Object nameProperty = serviceReference.getProperty(
            JaxrsWhiteboardConstants.JAX_RS_NAME);

        failedApplicationDTO.name = nameProperty == null ?
            generateApplicationName(serviceReference::getProperty) :
            nameProperty.toString();
        failedApplicationDTO.serviceId =
            (long)serviceReference.getProperty("service.id");

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

    private static <T extends BaseDTO> T populateBaseDTO(
    		T baseDTO, CachingServiceReference<?> serviceReference) {

        baseDTO.name = getApplicationName(serviceReference::getProperty);
        baseDTO.serviceId = (Long)serviceReference.getProperty(
            "service.id");
        
        return baseDTO;
    }

    private static void populateBaseExtensionDTO(
        BaseExtensionDTO extensionDTO,
        CachingServiceReference<?> serviceReference) {

        populateBaseDTO(extensionDTO, serviceReference);

        extensionDTO.extensionTypes =
            Arrays.stream(
                canonicalize(serviceReference.getProperty("objectClass"))).
            filter(
                SUPPORTED_EXTENSION_INTERFACES::contains
            ).
            toArray(String[]::new);
    }

    private static ExtensionDTO populateExtensionDTO(
        ExtensionDTO extensionDTO,
        CachingServiceReference<?> serviceReference) {

        populateBaseExtensionDTO(extensionDTO, serviceReference);

        return extensionDTO;
    }

    private static ResourceDTO populateResourceDTO(
        ResourceDTO resourceDTO,
        EndpointRuntimeInformation endpointRuntimeInformation) {

        populateBaseDTO(
            resourceDTO, endpointRuntimeInformation._cachingServiceReference);

        resourceDTO.resourceMethods = ClassIntrospector.getResourceMethodInfos(
            endpointRuntimeInformation._class,
            endpointRuntimeInformation._bus
        ).toArray(
            new ResourceMethodInfoDTO[0]
        );

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

    private Stream<ApplicationDTO> applicationDTOStream() {
        return _applications.values().stream().
            filter(p -> !(".default".equals(
                p._cachingServiceReference.getProperty(JAX_RS_NAME)))).
            map(
                this::buildApplicationDTO
            );
    }

    private Stream<FailedExtensionDTO> applicationDependentExtensionsDTOStream() {
        return _applicationDependentExtensions.stream().map(
            sr -> buildFailedExtensionDTO(
                DTOConstants.FAILURE_REASON_REQUIRED_APPLICATION_UNAVAILABLE, sr)
        );
    }

    private Stream<FailedResourceDTO> applicationDependentResourcesDTOStream() {
        return _applicationDependentResources.stream().map(
            sr -> buildFailedResourceDTO(
                DTOConstants.FAILURE_REASON_REQUIRED_APPLICATION_UNAVAILABLE, sr)
        );
    }

    private ApplicationDTO buildApplicationDTO(
        ApplicationRuntimeInformation ari) {

        ApplicationDTO applicationDTO = new ApplicationDTO(){};

        applicationDTO.name = getApplicationName(
            ari._cachingServiceReference::getProperty);
        applicationDTO.base = getApplicationBase(
            ari._cachingServiceReference::getProperty);
        applicationDTO.serviceId =
            (Long)ari._cachingServiceReference.getProperty("service.id");

        applicationDTO.resourceDTOs = getApplicationEndpointsStream(
            applicationDTO.name).toArray(
                ResourceDTO[]::new
            );

        applicationDTO.extensionDTOs = getApplicationExtensionsStream(
            applicationDTO.name).toArray(
                ExtensionDTO[]::new
            );

        CxfJaxrsServiceRegistrator cxfJaxRsServiceRegistrator =
            ari._cxfJaxRsServiceRegistrator;

        Bus bus = cxfJaxRsServiceRegistrator.getBus();
        Iterable<Class<?>> resourceClasses =
            cxfJaxRsServiceRegistrator.getStaticResourceClasses();

        ArrayList<ResourceMethodInfoDTO> resourceMethodInfoDTOS =
            new ArrayList<>();

        for (Class<?> resourceClass : resourceClasses) {
            resourceMethodInfoDTOS.addAll(
                ClassIntrospector.getResourceMethodInfos(resourceClass, bus));
        }

        applicationDTO.resourceMethods = resourceMethodInfoDTOS.toArray(
            new ResourceMethodInfoDTO[0]);

        return applicationDTO;
    }

    private FailedExtensionDTO buildFailedExtensionDTO(
        int reason, CachingServiceReference<?> serviceReference) {

        FailedExtensionDTO failedExtensionDTO = new FailedExtensionDTO();

        populateBaseExtensionDTO(failedExtensionDTO, serviceReference);

        failedExtensionDTO.failureReason = reason;

        return failedExtensionDTO;
    }

    private FailedResourceDTO buildFailedResourceDTO(
        int reason, CachingServiceReference<?> serviceReference) {

        FailedResourceDTO failedResourceDTO = new FailedResourceDTO();

        populateBaseDTO(failedResourceDTO, serviceReference);

        failedResourceDTO.failureReason = reason;

        return failedResourceDTO;
    }

    private Stream<FailedApplicationDTO> clashingApplicationsDTOStream() {
        return _clashingApplications.stream().map(
            sr -> buildFailedApplicationDTO(
                DTOConstants.FAILURE_REASON_DUPLICATE_NAME, sr)
        );
    }

    private Stream<FailedApplicationDTO> dependentApplicationsDTOStream() {
        return _dependentApplications.stream().map(
            sr -> buildFailedApplicationDTO(
                DTOConstants.FAILURE_REASON_REQUIRED_EXTENSIONS_UNAVAILABLE, sr)
        );
    }

    private Stream<FailedExtensionDTO> dependentExtensionsStreamDTO() {
        return _dependentExtensions.stream().map(
            sr -> buildFailedExtensionDTO(
                DTOConstants.FAILURE_REASON_REQUIRED_EXTENSIONS_UNAVAILABLE,
                sr));
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

    private Stream<FailedResourceDTO> erroredEndpointsStreamDTO() {
        return _erroredEndpoints.stream().map(
            sr -> buildFailedResourceDTO(
                DTOConstants.FAILURE_REASON_UNKNOWN, sr)
        );
    }

    private Stream<FailedExtensionDTO> erroredExtensionsDTOStream() {
        return _erroredExtensions.stream().map(
            sr -> buildFailedExtensionDTO(
                DTOConstants.FAILURE_REASON_UNKNOWN, sr)
        );
    }

    private Stream<ResourceDTO> getApplicationEndpointsStream(String name) {
        Collection<EndpointRuntimeInformation> endpointRuntimeInformations =
            _applicationEndpoints.get(name);

        Stream<EndpointRuntimeInformation> applicationEndpointStream =
            endpointRuntimeInformations != null ?
                endpointRuntimeInformations.stream() :
                Stream.empty();

        return
            applicationEndpointStream.map(
                sr -> populateResourceDTO(new ResourceDTO(), sr)
            );
    }

    private Stream<ExtensionDTO> getApplicationExtensionsStream(String name) {
        Collection<CachingServiceReference<?>> applicationExtensions =
            _applicationExtensions.get(name);

        Stream<CachingServiceReference<?>> applicationExtensionStream =
            applicationExtensions != null ?
                applicationExtensions.stream() :
                Stream.empty();

        return
            applicationExtensionStream.map(
                sr -> populateExtensionDTO(new ExtensionDTO(), sr)
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

    private static class EndpointRuntimeInformation {
        public EndpointRuntimeInformation(
            CachingServiceReference cachingServiceReference, Bus bus,
            Class<?> aClass) {

            _cachingServiceReference = cachingServiceReference;
            _bus = bus;
            _class = aClass;
        }

        @Override
        public int hashCode() {
            return _cachingServiceReference.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            EndpointRuntimeInformation that = (EndpointRuntimeInformation) o;

            return _cachingServiceReference.equals(
                that._cachingServiceReference);
        }
        CachingServiceReference _cachingServiceReference;
        Bus _bus;
        Class<?> _class;
    }

    private static class ApplicationRuntimeInformation {
        public ApplicationRuntimeInformation(
            CachingServiceReference cachingServiceReference,
            CxfJaxrsServiceRegistrator cxfJaxRsServiceRegistrator) {

            _cachingServiceReference = cachingServiceReference;
            _cxfJaxRsServiceRegistrator = cxfJaxRsServiceRegistrator;
        }

        @Override
        public int hashCode() {
            return _cachingServiceReference.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            EndpointRuntimeInformation that = (EndpointRuntimeInformation) o;

            return _cachingServiceReference.equals(
                that._cachingServiceReference);
        }

        CachingServiceReference _cachingServiceReference;
        CxfJaxrsServiceRegistrator _cxfJaxRsServiceRegistrator;

    }

}