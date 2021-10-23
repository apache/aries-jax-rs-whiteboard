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

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.junit.jupiter.api.Test;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import java.util.Set;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CxfJaxrsServiceRegistratorTest {
    @Test
    public void unproxy() {
        final JAXRSServerFactoryBean bean = new CxfJaxrsServiceRegistrator(
                null, null, emptyMap(), null
        ).createEndpoint(new Application() {
            @Override
            public Set<Object> getSingletons() {
                return singleton(new MyResource$$Proxy());
            }
        }, JAXRSServerFactoryBean.class);
        bean.setStart(false);
        bean.create();
        assertEquals(singletonList(MyResource.class), bean.getResourceClasses());
        final ClassResourceInfo cri = bean.getServiceFactory().getClassResourceInfo().iterator().next();
        assertEquals(MyResource.class, cri.getServiceClass());
        assertEquals(MyResource.class, cri.getResourceClass());
        assertTrue(SingletonResourceProvider.class.isInstance(cri.getResourceProvider()));
    }

    @Test
    public void ignoreAppPAth() {
        final JAXRSServerFactoryBean bean = new CxfJaxrsServiceRegistrator(
                null, null, emptyMap(), null
        ).createEndpoint(new MyApp(), JAXRSServerFactoryBean.class);
        assertEquals("/", bean.getAddress());
    }

    @ApplicationPath("foo")
    public static class MyApp extends Application {}

    @Path("my")
    public static class MyResource {
        @GET
        public String get() {
            return "";
        }
    }

    public static class MyResource$$Proxy extends MyResource {
    }
}
