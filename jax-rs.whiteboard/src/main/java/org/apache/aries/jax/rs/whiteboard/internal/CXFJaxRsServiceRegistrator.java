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
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Application;
import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;
import org.osgi.framework.ServiceReference;

import static org.apache.aries.jax.rs.whiteboard.internal.Utils.safeToString;
import static org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants.*;

public class CXFJaxRsServiceRegistrator {

    private volatile boolean _closed = false;
    private final Application _application;
    private final Bus _bus;
    private final Map<String, Object> _properties;
    private final Collection<Object> _providers = new ArrayList<>();
    private Server _server;
    private final Collection<ResourceInformation> _services = new ArrayList<>();

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
            if (key.equals(JAX_RS_RESOURCE_BASE)) {
                continue;
            }
            properties.put(key, sref.getProperty(key));
        }

        properties.put(
            CXF_ENDPOINT_ADDRESS, sref.getProperty(addressKey).toString());
        return properties;
    }

    public void add(ResourceInformation resourceInformation) {
        if (_closed) {
            return;
        }

        _services.add(resourceInformation);

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

    public void remove(ResourceInformation resourceInformation) {
        if (_closed) {
            return;
        }

        _services.remove(resourceInformation);

        rewire();
    }

    public void removeProvider(Object provider) {
        if (_closed) {
            return;
        }

        _providers.remove(provider);

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

        for (ResourceInformation resourceInformation : _services) {

            ResourceProvider resourceProvider =
                resourceInformation.getResourceProvider();

            jaxRsServerFactoryBean.setResourceProvider(resourceProvider);

            //FIXME: this is hack to programmatically prefix only resource path
            List<ClassResourceInfo> classResourceInfo =
                serviceFactory.getClassResourceInfo();

            for (ClassResourceInfo resourceInfo : classResourceInfo) {
                if (resourceInfo.getServiceClass() ==
                        resourceProvider.getResourceClass()) {
                    URITemplate uriTemplate = resourceInfo.getURITemplate();
                    resourceInfo.setURITemplate(
                        new URITemplate(
                            resourceInformation.getPrefixPath() +
                                uriTemplate.getValue()));
                }
            }
        }

        String address = safeToString(_properties.get(CXF_ENDPOINT_ADDRESS));

        if (address != null) {
            jaxRsServerFactoryBean.setAddress(address);
        }

        _server = jaxRsServerFactoryBean.create();

        _server.start();
    }

    public static class ResourceInformation {
        private final String prefixPath;
        private final ResourceProvider _resourceProvider;

        public ResourceInformation(
            String prefixPath, ResourceProvider resourceProvider) {

            this.prefixPath = prefixPath;
            this._resourceProvider = resourceProvider;
        }

        public String getPrefixPath() {
            return prefixPath;
        }

        public ResourceProvider getResourceProvider() {
            return _resourceProvider;
        }

    }

}
