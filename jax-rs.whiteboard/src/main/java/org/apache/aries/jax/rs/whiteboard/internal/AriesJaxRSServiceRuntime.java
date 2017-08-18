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

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jaxrs.runtime.JaxRSServiceRuntime;
import org.osgi.service.jaxrs.runtime.dto.ApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.DTOConstants;
import org.osgi.service.jaxrs.runtime.dto.FailedApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.RequestInfoDTO;
import org.osgi.service.jaxrs.runtime.dto.RuntimeDTO;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;

import javax.ws.rs.core.Application;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants.JAX_RS_NAME;

public class AriesJaxRSServiceRuntime implements JaxRSServiceRuntime {

    private static final long serialVersionUID = 1L;

    private ConcurrentHashMap<String, ServiceReference<Application>>
        _applications = new ConcurrentHashMap<>();

    private TreeSet<ServiceReference<Application>>
        _ungettableApplications;

    private Set<ServiceReference<Application>> _shadowedApplications =
        new TreeSet<>();

    public AriesJaxRSServiceRuntime(BundleContext bundleContext) {
        _ungettableApplications = new TreeSet<>(Comparator.reverseOrder());
    }

    public boolean addNotGettable(
        ServiceReference<Application> serviceReference) {

        return _ungettableApplications.add(serviceReference);
    }

    public boolean removeNotGettable(
        ServiceReference<Application> serviceReference) {

        return _ungettableApplications.remove(serviceReference);
    }

    public ServiceReference<Application> setApplicationForPath(
        String path, ServiceReference<Application> serviceReference) {

        return _applications.put(path, serviceReference);
    }

    public ServiceReference<Application> unsetApplicationForPath(String path) {
        return _applications.remove(path);
    }

    public boolean addShadowedApplication(
        ServiceReference<Application> serviceReference) {

        return _shadowedApplications.add(serviceReference);
    }

    public boolean removeShadowedApplication(
        ServiceReference<Application> serviceReference) {

        return _shadowedApplications.remove(serviceReference);
    }

    @Override
    public RequestInfoDTO calculateRequestInfoDTO(String path) {
        return null;
    }

    @Override
    public RuntimeDTO getRuntimeDTO() {
        RuntimeDTO runtimeDTO = new RuntimeDTO();

        runtimeDTO.applicationDTOs = applicationDTOStream().
            toArray(
                ApplicationDTO[]::new
            );

        runtimeDTO.failedApplicationDTOs = Stream.concat(
            shadowedApplications(),
            unreferenciableApplications()
            ).toArray(
                FailedApplicationDTO[]::new
            );

        return runtimeDTO;
    }

    private Stream<ApplicationDTO> applicationDTOStream() {
        return _applications.values().stream().
            map(
                AriesJaxRSServiceRuntime::buildApplicationDTO
            );
    }

    private Stream<FailedApplicationDTO> unreferenciableApplications() {
        return _ungettableApplications.stream().
            map(
                sr -> buildFailedApplicationDTO(
                    DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE,
                    sr)
        );
    }

    private Stream<FailedApplicationDTO> shadowedApplications() {
        return _shadowedApplications.stream().
            map(sr -> buildFailedApplicationDTO(
                DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE,
                sr)
        );
    }

    private static ApplicationDTO buildApplicationDTO(
        ServiceReference<Application> serviceReference) {

        ApplicationDTO applicationDTO = new ApplicationDTO(){};

        applicationDTO.name = getApplicationName(serviceReference);

        return applicationDTO;
    }

    private static FailedApplicationDTO buildFailedApplicationDTO(
        int reason, ServiceReference<Application> serviceReference) {

        FailedApplicationDTO failedApplicationDTO = new FailedApplicationDTO();

        Object nameProperty = serviceReference.getProperty(
            JaxRSWhiteboardConstants.JAX_RS_NAME);

        failedApplicationDTO.name = nameProperty == null ?
            generateApplicationName(serviceReference) :
            nameProperty.toString();

        failedApplicationDTO.failureReason = reason;

        return failedApplicationDTO;
    }

    private static String getApplicationName(
        ServiceReference<Application> serviceReference) {

        Object property = serviceReference.getProperty(JAX_RS_NAME);

        if (property == null) {
            return generateApplicationName(serviceReference);
        }

        return property.toString();
    }

    private static String generateApplicationName(
        ServiceReference<Application> serviceReference) {

        return "jax-rs-application-" +
            serviceReference.getProperty("service.id").toString();
    }

}