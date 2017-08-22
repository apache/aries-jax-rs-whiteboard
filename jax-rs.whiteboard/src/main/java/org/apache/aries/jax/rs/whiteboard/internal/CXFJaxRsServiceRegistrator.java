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

public class CXFJaxRsServiceRegistrator {

    private volatile boolean _closed = false;
    private final Application _application;
    private final Bus _bus;
    private final Collection<Object> _providers = new ArrayList<>();
    private Server _server;
    private final Collection<ResourceProvider> _services = new ArrayList<>();

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

    public void addProvider(Utils.ServiceTuple<?> tuple) {
        if (_closed) {
            return;
        }

        _providers.add(tuple.getService());

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

    public void removeProvider(Utils.ServiceTuple<?> tuple) {
        if (_closed) {
            return;
        }

        _providers.remove(tuple.getService());

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

}
