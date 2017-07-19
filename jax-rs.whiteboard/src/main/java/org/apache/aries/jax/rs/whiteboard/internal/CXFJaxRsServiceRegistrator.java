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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.apache.aries.jax.rs.whiteboard.internal.Utils.ComparableResourceProvider;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.ext.ResourceComparator;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.osgi.framework.ServiceReference;

import static org.apache.aries.jax.rs.whiteboard.internal.Utils.safeToString;

public class CXFJaxRsServiceRegistrator {

    private volatile boolean _closed = false;
    private final Application _application;
    private final Bus _bus;
    private final Map<String, Object> _properties;
    private final Collection<Object> _providers = new ArrayList<>();
    private Server _server;
    private final Collection<ResourceProvider> _services = new ArrayList<>();

    private static final String CXF_ENDPOINT_ADDRESS = "CXF_ENDPOINT_ADDRESS";

    public CXFJaxRsServiceRegistrator(
        Bus bus, Application application, Map<String, Object> properties) {

        _bus = bus;
        _application = application;
        _properties = properties;

        rewire();
    }

    public static Map<String, Object> getProperties(
        ServiceReference<?> sref, String addressKey) {

        String[] propertyKeys = sref.getPropertyKeys();
        Map<String, Object> properties = new HashMap<>(propertyKeys.length);

        for (String key : propertyKeys) {
            properties.put(key, sref.getProperty(key));
        }

        properties.put(
            CXF_ENDPOINT_ADDRESS, sref.getProperty(addressKey).toString());
        return properties;
    }

    public void add(ResourceProvider resourceProvider) {
        if (_closed) {
            return;
        }

        _services.add(resourceProvider);

        rewire();
    }

    public void addProvider(Object provider) {
        if (_closed) {
            return;
        }

        _providers.add(provider);

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

    public void remove(ResourceProvider resourceProvider) {
        if (_closed) {
            return;
        }

        _services.remove(resourceProvider);

        rewire();
    }

    public void removeProvider(Object provider) {
        if (_closed) {
            return;
        }

        _providers.remove(provider);

        rewire();
    }

    private static class ComparableResourceComparator
        implements ResourceComparator {

        @Override
        public int compare(
            ClassResourceInfo cri1, ClassResourceInfo cri2, Message message) {

            ResourceProvider rp1 = cri1.getResourceProvider();
            ResourceProvider rp2 = cri2.getResourceProvider();

            if (rp1 instanceof ComparableResourceProvider &&
                rp2 instanceof ComparableResourceProvider) {

                return -((ComparableResourceProvider) rp1).compareTo(
                    (ComparableResourceProvider) rp2);
            }

            if (rp1 instanceof ComparableResourceProvider) {
                return 1;
            }

            if (rp2 instanceof ComparableResourceProvider) {
                return -1;
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
        jaxRsServerFactoryBean.setProperties(_properties);

        String address = safeToString(_properties.get(CXF_ENDPOINT_ADDRESS));

        DestinationFactoryManager dfm = _bus.getExtension(
            DestinationFactoryManager.class);
        DestinationFactory destinationFactory = dfm.getDestinationFactoryForUri(
            address);

        jaxRsServerFactoryBean.setDestinationFactory(
            new CXF7409DestinationFactory(destinationFactory));

        JSONProvider<Object> jsonProvider = new JSONProvider<>();

        jsonProvider.setDropCollectionWrapperElement(true);
        jsonProvider.setDropRootElement(true);
        jsonProvider.setSerializeAsArray(true);
        jsonProvider.setSupportUnwrapped(true);

        jaxRsServerFactoryBean.setProvider(jsonProvider);

        for (Object provider : _providers) {
            jaxRsServerFactoryBean.setProvider(provider);
        }

        for (ResourceProvider resourceProvider: _services) {
            jaxRsServerFactoryBean.setResourceProvider(resourceProvider);
        }

        jaxRsServerFactoryBean.setAddress(address);

        jaxRsServerFactoryBean.setResourceComparator(
            new ComparableResourceComparator());

        _server = jaxRsServerFactoryBean.create();

        _server.start();
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

    /**
     * This class exists as a workaround for
     * https://issues.apache.org/jira/browse/CXF-7409
     */
    private static class CXF7409DestinationFactory
        implements DestinationFactory {

        private final DestinationFactory _destinationFactory;

        public CXF7409DestinationFactory(
            DestinationFactory destinationFactory) {

            _destinationFactory = destinationFactory;
        }

        @Override
        public Destination getDestination(
            EndpointInfo endpointInfo, Bus bus) throws IOException {

            Destination destination = _destinationFactory.getDestination(
                endpointInfo, bus);

            if (destination.getMessageObserver() != null) {
                throw new RuntimeException(
                    "There is already an application running at " +
                        endpointInfo.getAddress());
            }

            return destination;
        }

        @Override
        public Set<String> getUriPrefixes() {
            return _destinationFactory.getUriPrefixes();
        }

        @Override
        public List<String> getTransportIds() {
            return _destinationFactory.getTransportIds();
        }

    }

}
