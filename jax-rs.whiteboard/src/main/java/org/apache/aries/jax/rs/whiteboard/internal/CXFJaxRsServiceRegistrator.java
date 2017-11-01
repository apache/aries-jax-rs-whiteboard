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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.stream.Stream;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Feature;

import org.apache.aries.jax.rs.whiteboard.internal.Utils.ServiceReferenceResourceProvider;
import org.apache.aries.jax.rs.whiteboard.internal.Utils.ServiceTuple;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.ext.ResourceComparator;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;
import org.apache.cxf.message.Message;
import org.osgi.framework.ServiceReference;

import static org.apache.aries.jax.rs.whiteboard.internal.Utils.canonicalize;

public class CXFJaxRsServiceRegistrator {

    private final Application _application;
    private final Bus _bus;
    private final Collection<ServiceTuple<?>> _providers = new TreeSet<>(
        Comparator.comparing(ServiceTuple::getCachingServiceReference));
    private final Collection<ResourceProvider> _services = new ArrayList<>();
    private volatile boolean _closed = false;
    private Server _server;

    public CXFJaxRsServiceRegistrator(Bus bus, Application application) {
        _bus = bus;
        _application = application;

        rewire();
    }

    public void add(ResourceProvider resourceProvider) {
        if (_closed) {
            return;
        }

        _services.add(resourceProvider);

        rewire();
    }

    public void addProvider(ServiceTuple<?> tuple) {
        if (_closed) {
            return;
        }

        _providers.add(tuple);

        rewire();
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
        JAXRSServerFactoryBean bean = ResourceUtils.createApplication(app, false);
        if (JAXRSServerFactoryBean.class.isAssignableFrom(endpointType)) {
            return endpointType.cast(bean);
        }
        bean.setStart(false);
        Server server = bean.create();
        return endpointType.cast(server);
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

    private static class ComparableResourceComparator
        implements ResourceComparator {

        private static Comparator<ServiceReferenceResourceProvider> comparator;

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

        JAXRSServerFactoryBean jaxRsServerFactoryBean = createEndpoint(
            _application, JAXRSServerFactoryBean.class);

        jaxRsServerFactoryBean.setBus(_bus);

        JSONProvider<Object> jsonProvider = new JSONProvider<>();

        jsonProvider.setDropCollectionWrapperElement(true);
        jsonProvider.setDropRootElement(true);
        jsonProvider.setSerializeAsArray(true);
        jsonProvider.setSupportUnwrapped(true);

        jaxRsServerFactoryBean.setProvider(jsonProvider);

        for (ServiceTuple<?> provider : _providers) {
            jaxRsServerFactoryBean.setProvider(
                (Feature) featureContext -> {
                    ServiceReference<?> serviceReference =
                        provider.getCachingServiceReference().
                            getServiceReference();

                    String[] interfaces = canonicalize(
                        serviceReference.getProperty("objectClass"));

                    Class[] classes = Arrays.stream(interfaces).flatMap(
                        className -> {
                            try {
                                return Stream.of(Class.forName(className));
                            }
                            catch (ClassNotFoundException e) {
                                return Stream.empty();
                            }
                        }
                    ).toArray(
                        Class[]::new
                    );

                    featureContext.register(provider.getService(), classes);

                    return true;
                });
        }

        for (ResourceProvider resourceProvider: _services) {
            jaxRsServerFactoryBean.setResourceProvider(resourceProvider);
        }

        jaxRsServerFactoryBean.setResourceComparator(
            new ComparableResourceComparator());

        _server = jaxRsServerFactoryBean.create();

        _server.start();
    }

}
