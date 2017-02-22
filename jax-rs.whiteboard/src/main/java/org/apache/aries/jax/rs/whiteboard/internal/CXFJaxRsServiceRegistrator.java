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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;
import org.osgi.framework.ServiceReference;

public class CXFJaxRsServiceRegistrator {
    private volatile boolean _closed = false;
    private final Application _application;
    private final Bus _bus;
    private final Map<String, Object> _properties;
    private final Collection<Object> _providers = new ArrayList<>();
    private Server _server;
    private final Collection<ServiceInformation> _services = new ArrayList<>();

    public CXFJaxRsServiceRegistrator(
        Bus bus, Application application, Map<String, Object> properties) {

        _bus = bus;
        _application = application;
        _properties = properties;

        rewire();
    }
    
    public static Map<String, Object> getProperties(ServiceReference<?> sref, String addressKey) {
        String[] propertyKeys = sref.getPropertyKeys();
        Map<String, Object> properties = new HashMap<String, Object>(propertyKeys.length);

        for (String key : propertyKeys) {
            if (key.equals("osgi.jaxrs.resource.base")) {
                continue;
            }
            properties.put(key, sref.getProperty(key));
        }

        properties.put("CXF_ENDPOINT_ADDRESS", sref.getProperty(addressKey).toString());
        return properties;
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

    public void add(ServiceInformation serviceInformation) {
        if (_closed) {
            return;
        }

        Object object = serviceInformation.getService();

        if (object.getClass().isAnnotationPresent(Provider.class)) {
            _providers.add(object);
        } else {
            _services.add(serviceInformation);
        }
        rewire();
    }

    public void remove(Object object) {
        if (_closed) {
            return;
        }

        if (object.getClass().isAnnotationPresent(Provider.class)) {
            _providers.remove(object);
        }
        else {
            Iterator<ServiceInformation> iterator = _services.iterator();
            while (iterator.hasNext()) {
                ServiceInformation next = iterator.next();

                if (next.getService() == object) {
                    iterator.remove();
                }
            }
        }

        rewire();
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

        RuntimeDelegate runtimeDelegate = RuntimeDelegate.getInstance();

        JAXRSServerFactoryBean jaxRsServerFactoryBean =
            runtimeDelegate.createEndpoint(
                _application, JAXRSServerFactoryBean.class);

        jaxRsServerFactoryBean.setBus(_bus);
        jaxRsServerFactoryBean.setProperties(_properties);

        JSONProvider<Object> jsonProvider = new JSONProvider<>();

        jsonProvider.setDropCollectionWrapperElement(true);
        jsonProvider.setDropRootElement(true);
        jsonProvider.setSerializeAsArray(true);
        jsonProvider.setSupportUnwrapped(true);

        jaxRsServerFactoryBean.setProvider(jsonProvider);

        for (Object provider : _providers) {
            jaxRsServerFactoryBean.setProvider(provider);
        }

        JAXRSServiceFactoryBean serviceFactory =
            jaxRsServerFactoryBean.getServiceFactory();

        for (ServiceInformation serviceInformation : _services) {
            Object service = serviceInformation.getService();

            SingletonResourceProvider rp = new SingletonResourceProvider(
                service, true);

            jaxRsServerFactoryBean.setResourceProvider(rp);

            List<ClassResourceInfo> classResourceInfo =
                serviceFactory.getClassResourceInfo();

            for (ClassResourceInfo resourceInfo : classResourceInfo) {
                if (resourceInfo.getServiceClass() == service.getClass()) {
                    URITemplate uriTemplate = resourceInfo.getURITemplate();
                    resourceInfo.setURITemplate(
                        new URITemplate(
                            serviceInformation.getPrefixPath() +
                                uriTemplate.getValue()));
                }
            }
        }

        Object cxfEndpointAddressObject = _properties.get(
            "CXF_ENDPOINT_ADDRESS");

        String address;

        if (cxfEndpointAddressObject == null) {
            address = "";
        }
        else {
            address = cxfEndpointAddressObject.toString();
        }

        if (address != null) {
            jaxRsServerFactoryBean.setAddress(address);
        }

        _server = jaxRsServerFactoryBean.create();

        _server.start();
    }

    public static class ServiceInformation {
        private final String prefixPath;
        private final String scope;
        private final Object service;

        public ServiceInformation(
            String prefixPath, String scope, Object service) {

            this.prefixPath = prefixPath;
            this.scope = scope;
            this.service = service;
        }

        public String getPrefixPath() {
            return prefixPath;
        }

        public String getScope() {
            return scope;
        }

        public Object getService() {
            return service;
        }

    }

}
