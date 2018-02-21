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

import static java.util.stream.Collectors.toMap;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.canonicalize;
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

import javax.ws.rs.Priorities;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.aries.jax.rs.whiteboard.internal.Utils.ServiceReferenceResourceProvider;
import org.apache.aries.jax.rs.whiteboard.internal.Utils.ServiceTuple;
import org.apache.aries.osgi.functional.CachingServiceReference;
import org.apache.cxf.Bus;
import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.ext.ResourceComparator;
import org.apache.cxf.jaxrs.impl.ConfigurableImpl;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.model.ApplicationInfo;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.FilterProviderInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.provider.ProviderFactory.ProviderInfoClassComparator;
import org.apache.cxf.jaxrs.provider.ServerConfigurableFactory;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.message.Message;
import org.osgi.framework.ServiceReference;

public class CxfJaxrsServiceRegistrator {

    public CxfJaxrsServiceRegistrator(
        Bus bus, Application application, Map<String, Object> properties) {
        _bus = bus;
        _application = application;
        _properties = Collections.unmodifiableMap(new HashMap<>(properties));

        Comparator<ServiceTuple<?>> comparing = Comparator.comparing(
            ServiceTuple::getCachingServiceReference);

        _providers = new TreeSet<>(comparing);

        rewire();
    }

    public void add(ResourceProvider resourceProvider) {
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

    public void addProvider(ServiceTuple<?> tuple) {
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

        if (_server != null) {
            _server.destroy();
        }

        _closed = true;
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

    public void remove(ResourceProvider resourceProvider) {
        if (_closed) {
            return;
        }

        _services.remove(resourceProvider);

        rewire();
    }

    public void removeProvider(ServiceTuple<?> tuple) {
        if (_closed) {
            return;
        }

        _providers.remove(tuple);

        rewire();
    }

    protected static Set<String> getFilterNameBindings(Bus bus, Object provider) {
        Class<?> pClass = ClassHelper.getRealClass(bus, provider);
        Set<String> names = AnnotationUtils.getNameBindings(pClass.getAnnotations());
        if (names.isEmpty()) {
            names = Collections.singleton(DEFAULT_FILTER_NAME_BINDING);
        }
        return names;
    }
    private final Application _application;
    private final Bus _bus;
    private final Collection<ServiceTuple<?>> _providers;
    private final Collection<ResourceProvider> _services = new ArrayList<>();
    private volatile boolean _closed = false;
    private JAXRSServerFactoryBean _jaxRsServerFactoryBean;
    private Map<String, Object> _properties;
    private Server _server;

    private static class ComparableResourceComparator
        implements ResourceComparator {

        static {
            comparator = Comparator.comparing(
                srrp -> srrp.getImmutableServiceReference());
        }

        @Override
        public int compare(
            ClassResourceInfo cri1, ClassResourceInfo cri2, Message message) {

            ResourceProvider rp1 = cri1.getResourceProvider();
            ResourceProvider rp2 = cri2.getResourceProvider();

            if (rp1 instanceof ServiceReferenceResourceProvider &&
                rp2 instanceof ServiceReferenceResourceProvider) {

                return comparator.compare(
                    (ServiceReferenceResourceProvider)rp2,
                    (ServiceReferenceResourceProvider)rp1);
            }

            if (rp1 instanceof ServiceReferenceResourceProvider) {
                return -1;
            }

            if (rp2 instanceof ServiceReferenceResourceProvider) {
                return 1;
            }

            return 0;
        }

        @Override
        public int compare(
            OperationResourceInfo oper1, OperationResourceInfo oper2,
            Message message) {

            return 0;
        }
        private static Comparator<ServiceReferenceResourceProvider> comparator;

    }

    private static class ProviderInfoComparator implements Comparator<ProviderInfo<?>> {
        public ProviderInfoComparator(
            ProviderInfoClassComparator providerInfoClassComparator) {

            _providerInfoClassComparator = providerInfoClassComparator;
        }

        @Override
        public int compare(ProviderInfo<?> pi1, ProviderInfo<?> pi2) {
            if (pi1 instanceof ServiceReferenceFilterProviderInfo<?>) {
                if (pi2 instanceof ServiceReferenceFilterProviderInfo<?>) {
                    ServiceReference serviceReference1 =
                        ((ServiceReferenceFilterProviderInfo) pi1).
                            getServiceReference();
                    ServiceReference serviceReference2 =
                        ((ServiceReferenceFilterProviderInfo) pi2).
                            getServiceReference();

                    return serviceReference1.compareTo(serviceReference2);
                }
                else {
                    return -1;
                }
            }
            else {
                if (pi2 instanceof ServiceReferenceFilterProviderInfo<?>) {
                    return 1;
                }
            }

            return _providerInfoClassComparator.compare(pi1, pi2);
        }

        private final ProviderInfoClassComparator
            _providerInfoClassComparator;
    }

    private static class ServiceReferenceFilterProviderInfo<T>
        extends FilterProviderInfo<T> {

        public ServiceReferenceFilterProviderInfo(
            ServiceReference<?> serviceReference,
            Class<?> resourceClass, Class<?> serviceClass, T provider, Bus bus,
            Set<String> nameBindings, boolean dynamic, Map<Class<?>, Integer>
                supportedContracts) {

            super(resourceClass, serviceClass, provider, bus, nameBindings,
                dynamic, supportedContracts);

            _serviceReference = serviceReference;
        }

        public ServiceReference<?> getServiceReference() {
            return _serviceReference;
        }

        private ServiceReference<?> _serviceReference;
    }

    protected synchronized void rewire() {
        if (_server != null) {
            _server.destroy();
        }

        if (_services.isEmpty() &&
            _application.getSingletons().isEmpty() &&
            _application.getClasses().isEmpty()) {

            return;
        }

        _jaxRsServerFactoryBean = createEndpoint(
            _application, JAXRSServerFactoryBean.class);

        _jaxRsServerFactoryBean.setBus(_bus);

        _bus.setExtension(
            context -> {
                ConfigurableImpl<FeatureContext> configurable =
                    new ConfigurableImpl<>(
                        context, RuntimeType.SERVER,
                        ServerConfigurableFactory.
                            SERVER_FILTER_INTERCEPTOR_CLASSES);

                configurable.property(
                    "osgi.jaxrs.application.serviceProperties", _properties);

                return configurable;
            },
            ServerConfigurableFactory.class);

        _jaxRsServerFactoryBean.setStart(false);

        for (ServiceTuple<?> provider : _providers) {
            CachingServiceReference<?> cachingServiceReference =
                provider.getCachingServiceReference();

            ServiceReference<?> serviceReference =
                cachingServiceReference.getServiceReference();

            Object service = provider.getService();

            if (service instanceof Feature) {
                _jaxRsServerFactoryBean.setProvider(service);

                continue;
            }

            Class<?> realClass = ClassHelper.getRealClass(getBus(), service);

            Class<?>[] interfaces = Arrays.stream(canonicalize(
                serviceReference.getProperty("objectClass")))
                .filter(SUPPORTED_EXTENSION_INTERFACES::containsKey)
                .map(SUPPORTED_EXTENSION_INTERFACES::get)
                .toArray(Class[]::new);

            Map<Class<?>, Integer> classesWithPriorities=
                Arrays.stream(interfaces).collect(
                    toMap(
                        c -> c,
                        __ -> AnnotationUtils.getBindingPriority(
                            realClass))
                );

            _jaxRsServerFactoryBean.setProvider(
                new ServiceReferenceFilterProviderInfo<>(
                    serviceReference, realClass, realClass, service, getBus(),
                    getFilterNameBindings(getBus(), service), false,
                    classesWithPriorities));
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
            new ProviderInfoComparator(providerInfoClassComparator)
        );

        _server = _jaxRsServerFactoryBean.create();

        ApplicationInfo applicationInfo = (ApplicationInfo)
            _server.getEndpoint().get(Application.class.getName());

        applicationInfo.setOverridingProps(new HashMap<String, Object>() {{
            put("osgi.jaxrs.application.serviceProperties", _properties);
        }});

        _server.start();
    }

    protected Iterable<Class<?>> getStaticResourceClasses() {
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

}
