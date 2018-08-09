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

package org.apache.aries.jax.rs.whiteboard.internal.cxf;

import static java.util.stream.Collectors.toMap;
import static org.apache.aries.jax.rs.whiteboard.internal.utils.Utils.canonicalize;
import static org.apache.aries.jax.rs.whiteboard.internal.Whiteboard.SUPPORTED_EXTENSION_INTERFACES;
import static org.apache.cxf.jaxrs.provider.ProviderFactory.DEFAULT_FILTER_NAME_BINDING;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.aries.jax.rs.whiteboard.internal.utils.ServiceReferenceResourceProvider;
import org.apache.aries.jax.rs.whiteboard.internal.utils.ServiceTuple;
import org.apache.aries.component.dsl.CachingServiceReference;
import org.apache.cxf.Bus;
import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.impl.ConfigurableImpl;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.model.ApplicationInfo;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.provider.ProviderFactory.ProviderInfoClassComparator;
import org.apache.cxf.jaxrs.provider.ServerConfigurableFactory;
import org.apache.cxf.jaxrs.sse.SseContextProvider;
import org.apache.cxf.jaxrs.sse.SseEventSinkContextProvider;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.osgi.framework.ServiceObjects;

public class CxfJaxrsServiceRegistrator {

    public CxfJaxrsServiceRegistrator(
        Bus bus, ServiceTuple<Application> applicationTuple,
        Map<String, ?> properties) {

        _bus = bus;
        _applicationTuple = applicationTuple;
        _properties = Collections.unmodifiableMap(new HashMap<>(properties));

        Comparator<ServiceTuple<?>> comparing = Comparator.comparing(
            ServiceTuple::getCachingServiceReference);

        _providers = new TreeSet<>(comparing);

        rewire();
    }

    public synchronized void add(ResourceProvider resourceProvider) {
        if (_closed) {
            return;
        }

        _services.add(resourceProvider);

        try {
            rewire();
        }
        catch (Exception e) {
            remove(resourceProvider);

            throw e;
        }
    }

    public synchronized void addProvider(ServiceTuple<?> tuple) {
        if (_closed) {
            return;
        }

        _providers.add(tuple);

        try {
            rewire();
        }
        catch (Exception e) {
            removeProvider(tuple);

            throw e;
        }

    }

    public void close() {
        if (_closed) {
            return;
        }

        _closed = true;

        if (_server != null) {
            _server.destroy();
        }

        if (_bus != null) {
            _bus.shutdown(false);
        }
    }

    public <T> T createEndpoint(Application app, Class<T> endpointType) {
        JAXRSServerFactoryBean bean =
            RuntimeDelegate.getInstance().createEndpoint(
                app, JAXRSServerFactoryBean.class);

        if (JAXRSServerFactoryBean.class.isAssignableFrom(endpointType)) {
            return endpointType.cast(bean);
        }
        bean.setStart(false);
        Server server = bean.create();
        return endpointType.cast(server);
    }

    public Bus getBus() {
        return _bus;
    }

    public Iterable<Class<?>> getStaticResourceClasses() {
        if (_jaxRsServerFactoryBean == null) {
            return Collections.emptyList();
        }

        JAXRSServiceFactoryBean serviceFactory =
            _jaxRsServerFactoryBean.getServiceFactory();

        List<ClassResourceInfo> classResourceInfo =
            serviceFactory.getClassResourceInfo();

        ArrayList<Class<?>> classes = new ArrayList<>();

        for (ClassResourceInfo resourceInfo : classResourceInfo) {
            if (!ServiceReferenceResourceProvider.class.isAssignableFrom(
                resourceInfo.getResourceProvider().getClass())) {

                classes.add(resourceInfo.getResourceClass());
            }
        }

        return classes;
    }

    public synchronized void remove(ResourceProvider resourceProvider) {
        if (_closed) {
            return;
        }

        _services.remove(resourceProvider);

        rewire();
    }

    public synchronized void removeProvider(ServiceTuple<?> tuple) {
        if (_closed) {
            return;
        }

        _providers.remove(tuple);

        rewire();
    }

    protected synchronized void rewire() {
        if (_server != null) {
            _server.destroy();

            _applicationTuple.refresh();

            for (ServiceTuple<?> provider : _providers) {
                provider.refresh();
            }
        }

        Application application = _applicationTuple.getService();

        if (_services.isEmpty() &&
            application.getSingletons().isEmpty() &&
            application.getClasses().isEmpty()) {

            return;
        }

        _jaxRsServerFactoryBean = createEndpoint(
            application, JAXRSServerFactoryBean.class);

        _jaxRsServerFactoryBean.setBus(_bus);

        _bus.setExtension(
            context -> {
                ConfigurableImpl<FeatureContext> configurable =
                    new ConfigurableImpl<>(context, RuntimeType.SERVER);

                configurable.property(
                    "osgi.jaxrs.application.serviceProperties", _properties);

                return configurable;
            },
            ServerConfigurableFactory.class);

        _jaxRsServerFactoryBean.setStart(false);

        List<org.apache.cxf.feature.Feature> features = new ArrayList<>();

        for (ServiceTuple<?> provider : _providers) {
            CachingServiceReference<?> cachingServiceReference =
                provider.getCachingServiceReference();

            Object service = provider.getService();

            if (service instanceof Feature || service instanceof DynamicFeature) {
                _jaxRsServerFactoryBean.setProvider(service);

                continue;
            }
            else if (service instanceof org.apache.cxf.feature.Feature) {
                features.add((org.apache.cxf.feature.Feature)service);

                continue;
            }

            Class<?> realClass = ClassHelper.getRealClass(getBus(), service);

            Class<?>[] interfaces = Arrays.stream(canonicalize(
                cachingServiceReference.getProperty("objectClass")))
                .filter(SUPPORTED_EXTENSION_INTERFACES::containsKey)
                .map(SUPPORTED_EXTENSION_INTERFACES::get)
                .toArray(Class[]::new);

            Map<Class<?>, Integer> classesWithPriorities=
                Arrays.stream(interfaces).collect(
                    toMap(
                        c -> c,
                        __ -> AnnotationUtils.getBindingPriority(realClass))
                );

            _jaxRsServerFactoryBean.setProvider(
                new ServiceReferenceFilterProviderInfo<>(
                    cachingServiceReference, realClass, realClass, service,
                    getBus(), getFilterNameBindings(getBus(), service), false,
                    classesWithPriorities));
        }

        _jaxRsServerFactoryBean.setProvider(new SseEventSinkContextProvider());
        _jaxRsServerFactoryBean.setProvider(new SseContextProvider());

        if (!features.isEmpty()) {
            features.addAll(_jaxRsServerFactoryBean.getFeatures());

            _jaxRsServerFactoryBean.setFeatures(features);
        }

        for (ResourceProvider resourceProvider: _services) {
            _jaxRsServerFactoryBean.setResourceProvider(resourceProvider);
        }

        if (_jaxRsServerFactoryBean.getResourceClasses().isEmpty()) {
            return;
        }

        _jaxRsServerFactoryBean.setResourceComparator(
            new ComparableResourceComparator());

        ProviderInfoClassComparator providerInfoClassComparator =
            new ProviderInfoClassComparator(Object.class);

        _jaxRsServerFactoryBean.setProviderComparator(
            new ServiceReferenceProviderInfoComparator(
                providerInfoClassComparator)
        );

        _server = _jaxRsServerFactoryBean.create();

        ApplicationInfo applicationInfo = (ApplicationInfo)
            _server.getEndpoint().get(Application.class.getName());

        applicationInfo.setOverridingProps(new HashMap<String, Object>() {{
            put("osgi.jaxrs.application.serviceProperties", _properties);
        }});

        _server.start();
    }

    private final ServiceTuple<Application> _applicationTuple;
    private final Bus _bus;
    private final Collection<ServiceTuple<?>> _providers;
    private final Collection<ResourceProvider> _services = new ArrayList<>();
    private volatile boolean _closed = false;
    private JAXRSServerFactoryBean _jaxRsServerFactoryBean;
    private Map<String, Object> _properties;
    private Server _server;

    private static Set<String> getFilterNameBindings(
        Bus bus, Object provider) {
        Class<?> pClass = ClassHelper.getRealClass(bus, provider);
        Set<String> names = AnnotationUtils.getNameBindings(
            pClass.getAnnotations());
        if (names.isEmpty()) {
            names = Collections.singleton(DEFAULT_FILTER_NAME_BINDING);
        }
        return names;
    }

}
