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
import static org.apache.aries.jax.rs.whiteboard.internal.Whiteboard.SUPPORTED_EXTENSION_INTERFACES;
import static org.apache.aries.jax.rs.whiteboard.internal.utils.Utils.canonicalize;
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
import java.util.stream.Collectors;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.aries.component.dsl.CachingServiceReference;
import org.apache.aries.component.dsl.OSGi;
import org.apache.aries.jax.rs.whiteboard.internal.AriesJaxrsServiceRuntime;
import org.apache.aries.jax.rs.whiteboard.internal.ServiceReferenceRegistry;
import org.apache.aries.jax.rs.whiteboard.internal.utils.ServiceTuple;
import org.apache.cxf.Bus;
import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.ext.ResourceContextProvider;
import org.apache.cxf.jaxrs.impl.ConfigurableImpl;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.model.ApplicationInfo;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.provider.ProviderFactory.ProviderInfoClassComparator;
import org.apache.cxf.jaxrs.provider.ServerConfigurableFactory;
import org.apache.cxf.jaxrs.sse.SseContextProvider;
import org.apache.cxf.jaxrs.sse.SseEventSinkContextProvider;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;

public class CxfJaxrsServiceRegistrator {

    public Map<String, ?> getProperties() {
        return _properties;
    }

    public CxfJaxrsServiceRegistrator(
        Bus bus, ServiceTuple<Application> applicationTuple,
        Map<String, ?> properties,
        AriesJaxrsServiceRuntime ariesJaxrsServiceRuntime) {

        _bus = bus;
        _applicationTuple = applicationTuple;
        _properties = Collections.unmodifiableMap(new HashMap<>(properties));
        _ariesJaxrsServiceRuntime = ariesJaxrsServiceRuntime;

        Comparator<ServiceTuple<?>> comparing = Comparator.comparing(
            ServiceTuple::getCachingServiceReference);

        _providers = new TreeSet<>(comparing);
        _erroredProviders = new ArrayList<>();
        _erroredServices = new ArrayList<>();
        _serviceReferenceRegistry = new ServiceReferenceRegistry();
    }

    public synchronized void add(ResourceProvider resourceProvider) {
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
        _providers.add(tuple);

        try {
            rewire();
        }
        catch (Exception e) {
            removeProvider(tuple);

            throw e;
        }

    }

    public void enable() {
        _enabled = true;

        try {
            rewire();
        }
        catch (Exception e) {
            ArrayList<ServiceTuple<?>> providers = new ArrayList<>();
            ArrayList<ResourceProvider> services = new ArrayList<>();

            for (ServiceTuple<?> provider : _providers) {
                providers.add(provider);

                try {
                    doRewire(providers, services);
                }
                catch (Exception ex) {
                    providers.remove(provider);
                    _erroredProviders.add(provider);
                }
            }
            for (ResourceProvider service : _services) {
                services.add(service);

                try {
                    doRewire(providers, services);
                }
                catch (Exception ex) {
                    services.remove(service);
                    _erroredServices.add(service);
                }
            }

            _enabled = false;

            for (ServiceTuple<?> erroredProvider : _erroredProviders) {
                CachingServiceReference<?> cachingServiceReference =
                    erroredProvider.getCachingServiceReference();
                _providers.remove(erroredProvider);
                _ariesJaxrsServiceRuntime.addErroredExtension(
                    cachingServiceReference);
                _serviceReferenceRegistry.unregister(cachingServiceReference);
            }
            for (ResourceProvider erroredService : _erroredServices) {
                _services.remove(erroredService);
                _ariesJaxrsServiceRuntime.addErroredEndpoint(
                    ((ServiceReferenceResourceProvider)erroredService).
                        getImmutableServiceReference());
            }

            _enabled = true;

            rewire();
        }
    }

    public void close() {
        if (!_enabled) {
            return;
        }

        _enabled = false;

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
            if (resourceInfo == null) {
                continue;
            }

            ResourceProvider resourceProvider =
                resourceInfo.getResourceProvider();

            if (resourceProvider == null ||
                !ServiceReferenceResourceProvider.class.isAssignableFrom(
                    resourceProvider.getClass())) {

                classes.add(resourceInfo.getResourceClass());
            }
        }

        return classes;
    }

    public void registerExtension(
        CachingServiceReference<?> serviceReference) {

        _serviceReferenceRegistry.register(serviceReference);
    }

    public synchronized void remove(ResourceProvider resourceProvider) {
        if (_erroredServices.remove(resourceProvider)) {
            _ariesJaxrsServiceRuntime.removeErroredEndpoint(
                ((ServiceReferenceResourceProvider)resourceProvider).
                    getImmutableServiceReference());
        }

        _services.remove(resourceProvider);

        rewire();
    }

    public synchronized void removeProvider(ServiceTuple<?> tuple) {
        if (_erroredProviders.remove(tuple)) {
            _ariesJaxrsServiceRuntime.removeErroredExtension(
                tuple.getCachingServiceReference());
        }

        _providers.remove(tuple);

        rewire();
    }

    public synchronized void rewire() {
        doRewire(_providers, _services);
    }

    public void unregisterExtension(
        CachingServiceReference<?> serviceReference) {

        _serviceReferenceRegistry.unregister(serviceReference);
    }

    public OSGi<CachingServiceReference<?>> waitForExtension(
        String extensionDependency) {

        return _serviceReferenceRegistry.waitFor(extensionDependency);
    }

    private ArrayList<ServiceTuple<?>> _erroredProviders;
    private ArrayList<ResourceProvider> _erroredServices;

    @SuppressWarnings("serial")
    private synchronized void doRewire(
        Collection<ServiceTuple<?>> providers,
        Collection<ResourceProvider> services) {

        if (!_enabled) {
            return;
        }

        if (!_applicationTuple.isAvailable()) {
            _applicationTuple.dispose();

            return;
        }

        if (_server != null) {
            _server.destroy();

            _applicationTuple.refresh();

            for (ServiceTuple<?> provider : providers) {
                provider.refresh();
            }
        }

        Application application = _applicationTuple.getService();

        if (application == null) {
            return;
        }

        if (_services.isEmpty() &&
            application.getSingletons().isEmpty() &&
            application.getClasses().isEmpty()) {

            return;
        }

        _jaxRsServerFactoryBean = createEndpoint(
            application, JAXRSServerFactoryBean.class);

        _jaxRsServerFactoryBean.setInvoker(new PromiseAwareJAXRSInvoker());
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

        for (ServiceTuple<?> provider : providers) {
            CachingServiceReference<?> cachingServiceReference =
                provider.getCachingServiceReference();

            if (!provider.isAvailable()) {
                continue;
            }

            Object service = provider.getService();

            if (service == null) {
                continue;
            }

            if (service instanceof Feature ||
                service instanceof DynamicFeature) {

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

        for (ResourceProvider resourceProvider: services) {
            if (resourceProvider instanceof
                PrototypeServiceReferenceResourceProvider) {

                PrototypeServiceReferenceResourceProvider provider =
                    (PrototypeServiceReferenceResourceProvider)resourceProvider;
                if (!provider.isAvailable()) {
                    continue;
                }
            }

            _jaxRsServerFactoryBean.setResourceProvider(resourceProvider);
        }

        if (_jaxRsServerFactoryBean.getResourceClasses().isEmpty()) {
            return;
        }

        ComparableResourceComparator comparableResourceComparator =
            new ComparableResourceComparator();

        _jaxRsServerFactoryBean.setResourceComparator(
            comparableResourceComparator);

        ProviderInfoClassComparator providerInfoClassComparator =
            new ProviderInfoClassComparator(Object.class);

        _jaxRsServerFactoryBean.setProviderComparator(
            new ServiceReferenceProviderInfoComparator(
                providerInfoClassComparator)
        );

        _server = _jaxRsServerFactoryBean.create();

        Endpoint endpoint = _server.getEndpoint();

        ApplicationInfo applicationInfo = (ApplicationInfo)endpoint.get(
            Application.class.getName());

        applicationInfo.setOverridingProps(new HashMap<String, Object>() {{
            put("osgi.jaxrs.application.serviceProperties", _properties);
        }});

        endpoint.put(
            "org.apache.cxf.jaxrs.resource.context.provider",
            createResourceContextProvider(
                _jaxRsServerFactoryBean.getServiceFactory()));

        _server.start();
    }

    private ResourceContextProvider createResourceContextProvider(
        JAXRSServiceFactoryBean jaxrsServiceFactoryBean) {

        ComparableResourceComparator comparableResourceComparator =
            new ComparableResourceComparator();

        List<ClassResourceInfo> classResourceInfos =
            jaxrsServiceFactoryBean.getClassResourceInfo().stream().sorted(
                (cri1, cri2) -> comparableResourceComparator.compare(
                    cri1, cri2, null)
            ).collect(
                Collectors.toList()
            );

        HashMap<Class, ResourceProvider> map = new HashMap<>();

        for (ClassResourceInfo classResourceInfo : classResourceInfos) {
            map.put(
                classResourceInfo.getResourceClass(),
                classResourceInfo.getResourceProvider());
        }

        return map::get;
    }

    private final ServiceTuple<Application> _applicationTuple;
    private final Bus _bus;
    private final Collection<ServiceTuple<?>> _providers;
    private final Collection<ResourceProvider> _services = new ArrayList<>();
    private volatile boolean _enabled = false;
    private JAXRSServerFactoryBean _jaxRsServerFactoryBean;
    private Map<String, Object> _properties;
    private AriesJaxrsServiceRuntime _ariesJaxrsServiceRuntime;
    private ServiceReferenceRegistry _serviceReferenceRegistry;
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
