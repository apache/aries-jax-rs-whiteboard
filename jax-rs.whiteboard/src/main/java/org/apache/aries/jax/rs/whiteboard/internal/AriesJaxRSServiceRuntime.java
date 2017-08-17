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

import org.apache.aries.osgi.functional.Event;
import org.apache.aries.osgi.functional.OSGi;
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
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE;
import static org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants.JAX_RS_NAME;

public class AriesJaxRSServiceRuntime implements JaxRSServiceRuntime {

    private static final long serialVersionUID = 1L;

    private ConcurrentHashMap<String, TreeSet<Event<MutableTuple<Application>>>>
        _applications = new ConcurrentHashMap<>();

    private Collection<Event<MutableTuple<Application>>>
        _ungettableApplications;
    private Comparator<Event<MutableTuple<Application>>>
        _applicationComparator;
    private BundleContext _bundleContext;

    public AriesJaxRSServiceRuntime(BundleContext bundleContext) {
        _bundleContext = bundleContext;

        _applicationComparator = Comparator.comparing(
            Event::getContent);

        _applicationComparator = _applicationComparator.reversed();

        _ungettableApplications = new TreeSet<>(_applicationComparator);
    }

    public OSGi<MutableTuple<Application>> processApplications(
        OSGi<ServiceReference<Application>> source) {

        return
            source.map(
                sr -> new MutableTuple<>(sr, null)
            ).
            route(
                router -> {
                router.onIncoming(event -> {
                    MutableTuple<Application>
                        tuple = event.getContent();

                    ServiceReference<Application> serviceReference =
                        tuple.getServiceReference();

                    Application application = checkGettable(serviceReference);

                    if (application == null)  {
                        _ungettableApplications.add(event);

                        return;
                    }

                    tuple.setService(application);

                    String path = serviceReference.getProperty(JAX_RS_APPLICATION_BASE).
                        toString();

                    TreeSet<Event<MutableTuple<Application>>> applicationReferences =
                        _applications.computeIfAbsent(
                            path, __ -> new TreeSet<>(_applicationComparator));

                    Event<MutableTuple<Application>> first =
                        applicationReferences.size() > 0 ? applicationReferences.first() : null;

                    if (first == null || _applicationComparator.compare(event, first) < 0) {
                        if (first != null) {
                            router.signalLeave(first);
                        }

                        router.signalAdd(event);
                    }

                    applicationReferences.add(event);
                });
                router.onLeaving(event -> {
                    MutableTuple<Application> tuple = event.getContent();

                    ServiceReference<Application> serviceReference =
                        tuple.getServiceReference();

                    boolean ungettable = _ungettableApplications.removeIf(
                        t -> t.getContent().equals(tuple));

                    if (ungettable) {
                        return;
                    }

                    String path = serviceReference.getProperty(JAX_RS_APPLICATION_BASE).
                        toString();

                    TreeSet<Event<MutableTuple<Application>>>
                        applicationReferences = _applications.get(path);

                    Event<MutableTuple<Application>> first =
                        applicationReferences.first();

                    if (tuple.equals(first.getContent())) {
                        router.signalLeave(first);

                        Event<MutableTuple<Application>> second =
                            applicationReferences.higher(first);

                        if (second != null) {
                            router.signalAdd(second);
                        }
                    }

                    applicationReferences.removeIf(
                        t -> t.getContent().equals(tuple));
                });
            });
    }

    private Application checkGettable(
        ServiceReference<Application> serviceReference) {

        Application application = null;

        try {
            application = _bundleContext.getService(
                serviceReference);
        }
        catch (Exception e) {
        }

        if (application == null) {
            return null;
        }
        else {
            _bundleContext.ungetService(serviceReference);
        }

        return application;
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
            flatMap(
                tree -> tree.size() > 0 ? Stream.of(tree.first()) : Stream.empty()
            ).filter(
                Objects::nonNull
            ).map(
                Event::getContent
            ).map(
                MutableTuple::getServiceReference
            ).map(
                AriesJaxRSServiceRuntime::buildApplicationDTO
            );
    }

    private Stream<FailedApplicationDTO> unreferenciableApplications() {
        return _ungettableApplications.stream().
            map(
                Event::getContent
            ).map(
                MutableTuple::getServiceReference
            ).map(
                sr -> buildFailedApplicationDTO(
                    DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE,
                    sr)
        );
    }

    private Stream<FailedApplicationDTO> shadowedApplications() {
        return _applications.values().stream().flatMap(
            tree -> tree.size() > 0 ? tree.tailSet(tree.first(), false).stream() : Stream.empty()
        ).filter(
            Objects::nonNull
        ).map(
            Event::getContent
        ).map(
            MutableTuple::getServiceReference
        ).map(
            sr -> buildFailedApplicationDTO(
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

    public static class MutableTuple<T> implements Comparable<MutableTuple<T>> {

        private final ServiceReference<T> _serviceReference;
        private T _service;

        public MutableTuple(ServiceReference<T> a, T service) {
            _serviceReference = a;
            _service = service;
        }

        @Override
        public int compareTo(MutableTuple<T> o) {
            return _serviceReference.compareTo(o._serviceReference);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MutableTuple<?> that = (MutableTuple<?>) o;

            return _serviceReference.equals(that._serviceReference);
        }

        @Override
        public int hashCode() {
            return _serviceReference.hashCode();
        }

        public ServiceReference<T> getServiceReference() {
            return _serviceReference;
        }

        public T getSecond() {
            return _service;
        }

        public void setService(T service) {
            _service = service;
        }

    }

}