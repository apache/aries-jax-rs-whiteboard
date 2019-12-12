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

import static org.apache.aries.jax.rs.whiteboard.internal.utils.Utils.canonicalize;
import static org.apache.aries.jax.rs.whiteboard.internal.utils.Utils.generateApplicationName;
import static org.apache.aries.jax.rs.whiteboard.internal.Whiteboard.DEFAULT_NAME;
import static org.apache.aries.jax.rs.whiteboard.internal.Whiteboard.SUPPORTED_EXTENSION_INTERFACES;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

import org.apache.aries.jax.rs.whiteboard.AriesJaxrsWhiteboardConstants;
import org.apache.aries.jax.rs.whiteboard.internal.cxf.CxfJaxrsServiceRegistrator;
import org.apache.aries.jax.rs.whiteboard.internal.utils.PropertyHolder;
import org.apache.aries.jax.rs.whiteboard.internal.introspection.ClassIntrospector;
import org.apache.aries.component.dsl.CachingServiceReference;
import org.apache.aries.jax.rs.whiteboard.internal.utils.Utils;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.ServiceReferenceDTO;
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

    private static final Logger _log = LoggerFactory.getLogger(
        AriesJaxrsServiceRuntime.class);

    public AriesJaxrsServiceRuntime(Whiteboard whiteboard) {
        _whiteboard = whiteboard;
    }

    public static String getServiceName(PropertyHolder properties) {
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
        PropertyHolder registratorReference,
        CachingServiceReference<?> endpointImmutableServiceReference,
        Bus bus, Class<?> theClass) {

        _applicationEndpoints.compute(
            getServiceName(registratorReference),
            merger(
                new EndpointRuntimeInformation(
                    endpointImmutableServiceReference, bus, theClass)));

        if (_log.isDebugEnabled()) {
            _log.debug(
                "Resource service {} has been registered into application {}",
                endpointImmutableServiceReference,
                registratorReference.get("original.service.id"));
        }
    }

    public void addApplicationExtension(
        PropertyHolder registratorProperties,
        CachingServiceReference<?> extensionImmutableServiceReference,
        Class<?> theClass) {

        _applicationExtensions.compute(
            getServiceName(registratorProperties),
            merger(
                new ExtensionRuntimeInformation(
                    extensionImmutableServiceReference, theClass)));

        if (_log.isDebugEnabled()) {
            _log.debug(
                "Extension {} has been registered to application {}",
                extensionImmutableServiceReference,
                registratorProperties.get("original.service.id"));
        }
    }

    public void addClashingApplication(
        CachingServiceReference<?> serviceReference) {

        _clashingApplications.add(serviceReference);

        String serviceName = getServiceName(serviceReference::getProperty);

        if (_log.isDebugEnabled()) {
            _log.debug(
                "Application {} clashes with {} for name {}",
                serviceReference,
                _servicesForName.get(serviceName),
                serviceName);
        }
    }

    public void addClashingExtension(
        CachingServiceReference<?> serviceReference) {

        _clashingExtensions.add(serviceReference);


        String serviceName = getServiceName(serviceReference::getProperty);

        if (_log.isDebugEnabled()) {
            _log.debug(
                "Extension {} clashes with {} for name {}",
                serviceReference,
                _servicesForName.get(serviceName),
                serviceName);
        }
    }

    public void addClashingResource(
        CachingServiceReference<?> serviceReference) {

        _clashingResources.add(serviceReference);

        String serviceName = getServiceName(serviceReference::getProperty);

        if (_log.isDebugEnabled()) {
            _log.debug(
                "Resource {} clashes with {} for name {}",
                serviceReference,
                _servicesForName.get(serviceName),
                serviceName);
        }
    }

    public void addContextDependentApplication(
        CachingServiceReference<Application> serviceReference) {

        _contextDependentApplications.add(serviceReference);

        if (_log.isDebugEnabled()) {
            _log.debug(
                "Application {} depends on context filter {}",
                serviceReference,
                serviceReference.getProperty(HTTP_WHITEBOARD_CONTEXT_SELECT));
        }
    }

    public void addDependentApplication(
        CachingServiceReference<Application> applicationReference) {

        _dependentApplications.add(applicationReference);
    }

    public void addDependentExtensionInApplication(
        Map<String, ?> applicationReference,
        CachingServiceReference<?> cachingServiceReference) {

        _dependentExtensions.compute(
            getServiceName(applicationReference::get),
            merger(cachingServiceReference));
    }

    public void addDependentService(
        CachingServiceReference<?> serviceReference) {

        _dependentServices.add(serviceReference);
    }

    public void addErroredApplication(
        CachingServiceReference<Application> serviceReference) {

        _erroredApplications.add(serviceReference);

        if (_log.isWarnEnabled()) {
            _log.warn(
                "Application {} is registered with error",
                serviceReference);
        }
    }

    public <T> void addErroredEndpoint(
        CachingServiceReference<T> serviceReference) {

        if (_log.isWarnEnabled()) {
            _log.warn(
                "Resource {} is registered with error",
                serviceReference);
        }

        _erroredEndpoints.add(serviceReference);
    }

    public void addErroredExtension(
        CachingServiceReference<?> cachingServiceReference) {

        if (_log.isWarnEnabled()) {
            _log.warn(
                "Extension {} is registered with error",
                cachingServiceReference);
        }

        _erroredExtensions.add(cachingServiceReference);
    }

    public void addInvalidApplication(
        CachingServiceReference<?> serviceReference) {

        if (_log.isWarnEnabled()) {
            _log.warn(
                "Application {} is not valid", serviceReference);
        }

        _invalidApplications.add(serviceReference);
    }

    public void addInvalidExtension(
        CachingServiceReference<?> serviceReference) {

        if (_log.isWarnEnabled()) {
            _log.warn(
                "Extension {} is not valid", serviceReference);
        }

        _invalidExtensions.add(serviceReference);
    }

    public void addInvalidResource(
        CachingServiceReference<?> serviceReference) {

        if (_log.isWarnEnabled()) {
            _log.warn(
                "Resource {} is not valid", serviceReference);
        }

        _invalidResources.add(serviceReference);
    }

    public boolean addNotGettableApplication(
        CachingServiceReference<Application> serviceReference) {

        if (_log.isWarnEnabled()) {
            _log.warn(
                "Application from reference {} can't be got",
                serviceReference);
        }

        return _ungettableApplications.add(serviceReference);
    }

    public <T> boolean addNotGettableEndpoint(
        CachingServiceReference<T> serviceReference) {

        if (_log.isWarnEnabled()) {
            _log.warn(
                "Resource from reference {} can't be got",
                serviceReference);
        }

        return _ungettableEndpoints.add(serviceReference);
    }

    public <T> void addNotGettableExtension(
        CachingServiceReference<T> serviceReference) {

        if (_log.isWarnEnabled()) {
            _log.warn(
                "Extension from reference {} can't be got",
                serviceReference);
        }

        _ungettableExtensions.add(serviceReference);
    }

    public void addServiceForName(CachingServiceReference<?> serviceReference) {
        _servicesForName.put(
            getServiceName(serviceReference::getProperty), serviceReference);

        if (_log.isDebugEnabled()) {
            _log.debug(
                "Registered service {} for name {}",
                serviceReference,
                getServiceName(serviceReference::getProperty));
        }
    }

    public boolean addShadowedApplication(
        CachingServiceReference<Application> serviceReference,
        String actualBasePath) {

        if (_log.isDebugEnabled()) {
            ApplicationRuntimeInformation applicationRuntimeInformation =
                _applications.get(actualBasePath);

            if (applicationRuntimeInformation != null) {
                _log.debug(
                    "Application reference {} is shadowed by {}",
                    serviceReference,

                        applicationRuntimeInformation._cachingServiceReference
                );
            }
        }

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

        runtimeDTO.failedApplicationDTOs =
            Stream.concat(
                contextDependentApplicationsDTOStream(),
                Stream.concat(
                    invalidApplicationsDTOStream(),
                    Stream.concat(
                        shadowedApplicationsDTOStream(),
                        Stream.concat(
                            unreferenciableApplicationsDTOStream(),
                            Stream.concat(
                                clashingApplicationsDTOStream(),
                                Stream.concat(
                                    dependentApplicationsDTOStream(),
                                    erroredApplicationsDTOStream())))))
            ).toArray(
                FailedApplicationDTO[]::new
            );

        runtimeDTO.failedResourceDTOs =
            Stream.concat(
                invalidResourcesDTOStream(),
                Stream.concat(
                    clashingResourcesDTOStream(),
                    Stream.concat(
                        unreferenciableEndpointsDTOStream(),
                        Stream.concat(
                            dependentServiceStreamDTO(),
                            Stream.concat(
                                applicationDependentResourcesDTOStream(),
                                erroredEndpointsStreamDTO()))))
            ).toArray(
                FailedResourceDTO[]::new
            );

        runtimeDTO.failedExtensionDTOs =
            Stream.concat(
                clashingExtensionsDTOStream(),
                Stream.concat(
                    unreferenciableExtensionsDTOStream(),
                    Stream.concat(
                        applicationDependentExtensionsDTOStream(),
                        Stream.concat(
                            erroredExtensionsDTOStream(),
                            Stream.concat(dependentExtensionsStreamDTO(),
                                invalidExtensionsDTOStream()))))
            ).toArray(
                FailedExtensionDTO[]::new
            );

        ServiceReference<JaxrsServiceRuntime> serviceReference =
            _whiteboard.getServiceReference();

        ServiceReferenceDTO serviceDTO = new ServiceReferenceDTO();
        serviceDTO.bundle = serviceReference.getBundle().getBundleId();
        serviceDTO.id = (long)serviceReference.getProperty("service.id");
        serviceDTO.usingBundles = Arrays.stream(
            serviceReference.getUsingBundles()
        ).mapToLong(
            Bundle::getBundleId
        ).toArray();
        serviceDTO.properties = Utils.getProperties(serviceReference);

        runtimeDTO.serviceDTO = serviceDTO;

        return runtimeDTO;
    }

    public void removedServiceForName(
        CachingServiceReference<?> serviceReference) {

        _servicesForName.remove(getServiceName(serviceReference::getProperty));

        if (_log.isDebugEnabled()) {
            _log.debug(
                "Unregistered service {} for name {}",
                serviceReference,
                getServiceName(serviceReference::getProperty));
        }
    }

    public void unregisterApplicationExtensions(
        CachingServiceReference<?> applicationReference) {

        _dependentExtensions.remove(
            getServiceName(applicationReference::getProperty));
    }

    private ConcurrentHashMap<String, CachingServiceReference<?>>
        _servicesForName = new ConcurrentHashMap<>();
    private Whiteboard _whiteboard;

    private Stream<FailedApplicationDTO>
        contextDependentApplicationsDTOStream() {

        return _contextDependentApplications.stream().map(
            sr -> buildFailedApplicationDTO(
                AriesJaxrsWhiteboardConstants.
                    FAILURE_REASON_REQUIRED_CONTEXT_UNAVAILABLE,
                sr)
        );
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
        PropertyHolder registratorProperties,
        CachingServiceReference<?> cachingServiceReference) {

        _applicationEndpoints.compute(
            getServiceName(registratorProperties),
            remover(
                new EndpointRuntimeInformation(
                cachingServiceReference, null, null)));

        if (_log.isDebugEnabled()) {
            _log.debug(
                "Endpoint {} has been removed from application {}",
                cachingServiceReference,
                registratorProperties.get("original.service.id"));
        }
    }

    public void removeApplicationExtension(
        PropertyHolder registratorProperties,
        CachingServiceReference<?> extensionImmutableServiceReference) {

        _applicationExtensions.computeIfPresent(
            getServiceName(registratorProperties),
            remover(
                new ExtensionRuntimeInformation(
                extensionImmutableServiceReference, null)));

        if (_log.isDebugEnabled()) {
            _log.debug(
                "Extension {} has been removed from application {}",
                extensionImmutableServiceReference,
                registratorProperties.get("original.service.id"));
        }
    }

    public void removeClashingApplication(
        CachingServiceReference<?> serviceReference) {

        _clashingApplications.remove(serviceReference);

        if (_log.isDebugEnabled()) {
            _log.debug(
                "Application {} no longer clashes for name {}",
                serviceReference,
                getServiceName(serviceReference::getProperty));
        }
    }

    public void removeClashingExtension(
        CachingServiceReference<?> serviceReference) {

        _clashingExtensions.remove(serviceReference);

        if (_log.isDebugEnabled()) {
            _log.debug(
                "Extension {} no longer clashes for name {}",
                serviceReference,
                getServiceName(serviceReference::getProperty));
        }
    }

    public void removeClashingResource(
        CachingServiceReference<?> serviceReference) {

        _clashingResources.remove(serviceReference);

        if (_log.isDebugEnabled()) {
            _log.debug(
                "Resource {} no longer clashes for name {}",
                serviceReference,
                getServiceName(serviceReference::getProperty));
        }

    }

    public void removeContextDependentApplication(
        CachingServiceReference<Application> serviceReference) {

        _contextDependentApplications.remove(serviceReference);

        if (_log.isDebugEnabled()) {
            _log.debug(
                "Application {} no longer depends on context filter {}",
                serviceReference,
                serviceReference.getProperty(HTTP_WHITEBOARD_CONTEXT_SELECT));
        }

    }

    public void removeDependentApplication(
        CachingServiceReference<Application> applicationReference) {

        _dependentApplications.remove(applicationReference);
    }

    public void removeDependentExtensionFromApplication(
        Map<String, ?> properties,
        CachingServiceReference<?> cachingServiceReference) {

        _dependentExtensions.compute(
            getServiceName(properties::get),
            remover(cachingServiceReference));
    }

    public void removeDependentService(
        CachingServiceReference<?> serviceReference) {

        _dependentServices.remove(serviceReference);
    }

    public void removeErroredApplication(
        CachingServiceReference<Application> serviceReference) {

        _erroredApplications.remove(serviceReference);

        if (_log.isWarnEnabled()) {
            _log.warn(
                "Errored application {} is gone",
                serviceReference);
        }
    }

    public <T> void removeErroredEndpoint(
        CachingServiceReference<T> serviceReference) {

        _erroredEndpoints.remove(serviceReference);

        if (_log.isWarnEnabled()) {
            _log.warn(
                "Errored resource {} is gone", serviceReference);
        }
    }

    public void removeErroredExtension(
        CachingServiceReference<?> serviceReference) {

        _erroredExtensions.remove(serviceReference);

        if (_log.isWarnEnabled()) {
            _log.warn(
                "Errored extension {} is gone", serviceReference);
        }
    }

    public void removeInvalidApplication(
        CachingServiceReference<?> serviceReference) {

        _invalidApplications.remove(serviceReference);

        if (_log.isWarnEnabled()) {
            _log.warn(
                "Invalid application {} is gone",
                serviceReference);
        }
    }

    public void removeInvalidExtension(
        CachingServiceReference<?> serviceReference) {

        _invalidExtensions.remove(serviceReference);

        if (_log.isWarnEnabled()) {
            _log.warn(
                "Invalid extension {} is gone", serviceReference);
        }
    }

    public void removeInvalidResource(
        CachingServiceReference<?> serviceReference) {

        _invalidResources.remove(serviceReference);

        if (_log.isWarnEnabled()) {
            _log.warn(
                "Invalid resource {} is gone", serviceReference);
        }
    }

    public void removeNotGettableApplication(
        CachingServiceReference<Application> serviceReference) {

        _ungettableApplications.remove(serviceReference);

        if (_log.isWarnEnabled()) {
            _log.warn(
                "Ungettable application reference {} is gone",
                serviceReference);
        }
    }

    public <T> void removeNotGettableEndpoint(
        CachingServiceReference<T> serviceReference) {

        _ungettableEndpoints.remove(serviceReference);

        if (_log.isWarnEnabled()) {
            _log.warn(
                "Ungettable resource reference {} is gone",
                serviceReference);
        }
    }

    public <T> void removeNotGettableExtension(
        CachingServiceReference<T> serviceReference) {

        _ungettableExtensions.remove(serviceReference);

        if (_log.isWarnEnabled()) {
            _log.warn(
                "Ungettable extension reference {} is gone",
                serviceReference);
        }
    }

    public boolean removeShadowedApplication(
        CachingServiceReference<Application> serviceReference) {

        if (_log.isDebugEnabled()) {
            _log.debug(
                "Application {} is no longer shadowed",
                serviceReference);
        }

        return _shadowedApplications.remove(serviceReference);
    }

    public ApplicationRuntimeInformation setApplicationForPath(
        String path, CachingServiceReference<Application> serviceReference,
        CxfJaxrsServiceRegistrator cxfJaxRsServiceRegistrator) {

        ApplicationRuntimeInformation ari = new ApplicationRuntimeInformation(
            serviceReference, cxfJaxRsServiceRegistrator);

        return _applications.compute(
            path,
            (__, prop) -> {
                if (DEFAULT_NAME.equals(
                    getServiceName(
                        ari._cachingServiceReference::getProperty))) {

                    if (_log.isDebugEnabled()) {
                        _log.debug(
                            "Setting application {} as default",
                            serviceReference);
                    }

                    _defaultApplicationProperties = ari;
                }

                if (_log.isDebugEnabled()) {
                    _log.debug(
                        "Registering application {} for path {}",
                        serviceReference, path);
                }

                return ari;
            });
    }

    public ApplicationRuntimeInformation unsetApplicationForPath(String path) {
        return _applications.remove(path);
    }
    private Set<CachingServiceReference<?>> _applicationDependentExtensions =
        ConcurrentHashMap.newKeySet();
    private Set<CachingServiceReference<?>> _applicationDependentResources =
        ConcurrentHashMap.newKeySet();
    private ConcurrentHashMap<String, Collection<EndpointRuntimeInformation>>
        _applicationEndpoints = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Collection<ExtensionRuntimeInformation>>
        _applicationExtensions = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ApplicationRuntimeInformation>
        _applications = new ConcurrentHashMap<>();
    private Collection<CachingServiceReference<?>> _clashingApplications =
        new CopyOnWriteArrayList<>();
    private Collection<CachingServiceReference<?>> _clashingExtensions =
        new CopyOnWriteArrayList<>();
    private Collection<CachingServiceReference<?>> _clashingResources =
        new CopyOnWriteArrayList<>();
    private Set<CachingServiceReference<Application>>
        _contextDependentApplications = ConcurrentHashMap.newKeySet();
    private volatile ApplicationRuntimeInformation
        _defaultApplicationProperties;
    private Set<CachingServiceReference<Application>> _dependentApplications =
        ConcurrentHashMap.newKeySet();
    private ConcurrentHashMap<String, Collection<CachingServiceReference<?>>>
        _dependentExtensions = new ConcurrentHashMap<>();
    private Set<CachingServiceReference<?>> _dependentServices =
        ConcurrentHashMap.newKeySet();
    private Collection<CachingServiceReference<Application>>
        _erroredApplications = new CopyOnWriteArrayList<>();
    private Collection<CachingServiceReference<?>> _erroredEndpoints =
        new CopyOnWriteArrayList<>();
    private Collection<CachingServiceReference<?>> _erroredExtensions =
        new CopyOnWriteArrayList<>();
    private Collection<CachingServiceReference<?>> _invalidApplications =
        new CopyOnWriteArrayList<>();
    private Collection<CachingServiceReference<?>> _invalidExtensions =
        new CopyOnWriteArrayList<>();
    private Collection<CachingServiceReference<?>> _invalidResources =
        new CopyOnWriteArrayList<>();
    private Collection<CachingServiceReference<Application>>
        _shadowedApplications = new CopyOnWriteArrayList<>();
    private Collection<CachingServiceReference<Application>>
        _ungettableApplications = new CopyOnWriteArrayList<>();
    private Collection<CachingServiceReference<?>> _ungettableEndpoints =
        new CopyOnWriteArrayList<>();
    private Collection<CachingServiceReference<?>> _ungettableExtensions =
        new CopyOnWriteArrayList<>();

    private static FailedApplicationDTO buildFailedApplicationDTO(
        int reason, CachingServiceReference<?> serviceReference) {

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
                collection = Collections.newSetFromMap(new ConcurrentHashMap<>());
            }

            collection.add(t);

            return collection;
        };
    }

    private static <T extends BaseDTO> T populateBaseDTO(
            T baseDTO, CachingServiceReference<?> serviceReference) {

        baseDTO.name = getServiceName(serviceReference::getProperty);
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
                SUPPORTED_EXTENSION_INTERFACES::containsKey
            ).
            toArray(String[]::new);
    }

    private static ExtensionDTO populateExtensionDTO(
        ExtensionDTO extensionDTO, ExtensionRuntimeInformation eri) {

        populateBaseExtensionDTO(extensionDTO, eri._cachingServiceReference);

        Consumes consumes = AnnotationUtils.getClassAnnotation(
            eri._class, Consumes.class);
        Produces produces = AnnotationUtils.getClassAnnotation(
            eri._class, Produces.class);
        Set<String> nameBindings = AnnotationUtils.getNameBindings(
            eri._class.getAnnotations());

        if (nameBindings.isEmpty()) {
            nameBindings = null;
        }

        extensionDTO.consumes = consumes == null ? null :
            JAXRSUtils.getConsumeTypes(consumes).stream().
                map(
                    MediaType::toString
                ).toArray(
                    String[]::new
                );

        extensionDTO.produces = produces == null ? null :
            JAXRSUtils.getProduceTypes(produces).stream().
                map(
                    MediaType::toString
                ).toArray(
                    String[]::new
                );

        extensionDTO.nameBindings = nameBindings == null ? null :
            nameBindings.toArray(new String[0]);

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

    private Stream<FailedExtensionDTO>
        applicationDependentExtensionsDTOStream() {

        return _applicationDependentExtensions.stream().map(
            sr -> buildFailedExtensionDTO(
                DTOConstants.FAILURE_REASON_REQUIRED_APPLICATION_UNAVAILABLE,
                sr)
        );
    }

    private Stream<FailedResourceDTO> applicationDependentResourcesDTOStream() {
        return _applicationDependentResources.stream().map(
            sr -> buildFailedResourceDTO(
                DTOConstants.FAILURE_REASON_REQUIRED_APPLICATION_UNAVAILABLE,
                sr)
        );
    }

    private ApplicationDTO buildApplicationDTO(
        ApplicationRuntimeInformation ari) {

        ApplicationDTO applicationDTO = new ApplicationDTO(){};

        applicationDTO.name = getServiceName(
            ari._cachingServiceReference::getProperty);
        applicationDTO.base = _whiteboard.getApplicationBase(
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

        Map<String, Set<ExtensionDTO>> nameBoundExtensions =
            new HashMap<>();

        Map<ExtensionDTO, Set<ResourceDTO>> extensionResources =
            new HashMap<>();

        for (ExtensionDTO extensionDTO : applicationDTO.extensionDTOs) {
            if (extensionDTO.nameBindings == null) {
                continue;
            }

            for (String nameBinding : extensionDTO.nameBindings) {
                Set<ExtensionDTO> extensionDTOS =
                    nameBoundExtensions.computeIfAbsent(
                        nameBinding,
                        __ -> new HashSet<>()
                );

                extensionDTOS.add(extensionDTO);
            }
        }

        for (ResourceDTO resourceDTO : applicationDTO.resourceDTOs) {
            for (ResourceMethodInfoDTO resourceMethodInfo :
                resourceDTO.resourceMethods) {

                if (resourceMethodInfo.nameBindings == null) {
                    continue;
                }

                for (String nameBinding : resourceMethodInfo.nameBindings) {
                    Set<ExtensionDTO> extensionDTOS = nameBoundExtensions.get(
                        nameBinding);

                    if (extensionDTOS != null) {
                        for (ExtensionDTO extensionDTO : extensionDTOS) {
                            Set<ResourceDTO> resourceDTOS =
                                extensionResources.computeIfAbsent(
                                    extensionDTO, __ -> new HashSet<>());

                            resourceDTOS.add(resourceDTO);
                        }
                    }
                }
            }
        }

        extensionResources.forEach(
            (extensionDTO, resourceDTOS) ->
                extensionDTO.filteredByName = resourceDTOS.toArray(
                    new ResourceDTO[0])
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

    private static FailedResourceDTO buildFailedResourceDTO(
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

    private Stream<FailedExtensionDTO> clashingExtensionsDTOStream() {
        return _clashingExtensions.stream().map(
            sr -> buildFailedExtensionDTO(
                DTOConstants.FAILURE_REASON_DUPLICATE_NAME, sr));
    }

    private Stream<FailedResourceDTO> clashingResourcesDTOStream() {
        return _clashingResources.stream().map(
            sr -> buildFailedResourceDTO(
                DTOConstants.FAILURE_REASON_DUPLICATE_NAME, sr));
    }

    private Stream<FailedApplicationDTO> dependentApplicationsDTOStream() {
        return _dependentApplications.stream().map(
            sr -> buildFailedApplicationDTO(
                DTOConstants.FAILURE_REASON_REQUIRED_EXTENSIONS_UNAVAILABLE, sr)
        );
    }

    private Stream<FailedExtensionDTO> dependentExtensionsStreamDTO() {
        return _dependentExtensions.values().
            stream().flatMap(Collection::stream).map(
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

        return applicationEndpointStream.map(
            sr -> populateResourceDTO(new ResourceDTO(), sr)
        );
    }

    private Stream<ExtensionDTO> getApplicationExtensionsStream(String name) {
        Collection<ExtensionRuntimeInformation> extensionRuntimeInformations =
            _applicationExtensions.get(name);

        Stream<ExtensionRuntimeInformation> applicationExtensionStream =
            extensionRuntimeInformations != null ?
                extensionRuntimeInformations.stream() :
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

    private Stream<FailedApplicationDTO> invalidApplicationsDTOStream() {
        return _invalidApplications.stream().
            map(sr -> buildFailedApplicationDTO(
                DTOConstants.FAILURE_REASON_VALIDATION_FAILED, sr)
        );
    }

    private Stream<FailedResourceDTO> invalidResourcesDTOStream() {
        return _invalidResources.stream().
            map(sr -> buildFailedResourceDTO(
                DTOConstants.FAILURE_REASON_VALIDATION_FAILED, sr)
        );
    }

    private Stream<FailedApplicationDTO> shadowedApplicationsDTOStream() {
        return _shadowedApplications.stream().
            map(sr -> buildFailedApplicationDTO(
                DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE, sr)
        );
    }

    private Stream<FailedApplicationDTO>
        unreferenciableApplicationsDTOStream() {

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
            CachingServiceReference<?> cachingServiceReference, Bus bus,
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

        Bus _bus;
        CachingServiceReference<?> _cachingServiceReference;
        Class<?> _class;

    }

    private static class ExtensionRuntimeInformation {

        public ExtensionRuntimeInformation(
            CachingServiceReference<?> cachingServiceReference,
            Class<?> aClass) {

            _cachingServiceReference = cachingServiceReference;
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

            ExtensionRuntimeInformation that = (ExtensionRuntimeInformation) o;

            return _cachingServiceReference.equals(
                that._cachingServiceReference);
        }

        CachingServiceReference<?> _cachingServiceReference;
        Class<?> _class;

    }

    private static class ApplicationRuntimeInformation {

        public ApplicationRuntimeInformation(
            CachingServiceReference<?> cachingServiceReference,
            CxfJaxrsServiceRegistrator cxfJaxRsServiceRegistrator) {

            _cachingServiceReference = cachingServiceReference;
            _cxfJaxRsServiceRegistrator = cxfJaxRsServiceRegistrator;
        }

        CachingServiceReference<?> _cachingServiceReference;
        CxfJaxrsServiceRegistrator _cxfJaxRsServiceRegistrator;

        @Override
        public int hashCode() {
            return _cachingServiceReference.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ApplicationRuntimeInformation that =
                (ApplicationRuntimeInformation) o;

            return _cachingServiceReference.equals(
                that._cachingServiceReference);
        }

    }

}