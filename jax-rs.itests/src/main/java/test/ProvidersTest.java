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

package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.ext.RuntimeDelegate;
import javax.ws.rs.sse.SseEventSource;
import javax.ws.rs.sse.SseEventSource.Builder;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.junit4.service.ServiceRule;

public class ProvidersTest {

    @Rule
    public ServiceRule sr = new ServiceRule();

    @Test
    public void testClientBuilderFromAPI() {
        ClientBuilder clientBuilder = ClientBuilder.newBuilder();

        assertNotNull(clientBuilder);

        assertEquals("org.apache.aries.jax.rs.whiteboard.internal.client.ClientBuilderImpl", clientBuilder.getClass().getName());
    }

    @Test
    public void testRuntimeDelegateFromAPI() {
        RuntimeDelegate runtimeDelegate = RuntimeDelegate.getInstance();

        assertNotNull(runtimeDelegate);

        assertEquals("org.apache.cxf.jaxrs.impl.RuntimeDelegateImpl", runtimeDelegate.getClass().getName());
    }

    @InjectService
    public ClientBuilder clientBuilder;

    @Test
    public void testSseEventSourceBuilderFromAPI() {
        assertNotNull(clientBuilder);

        Builder builder = SseEventSource.target(clientBuilder.build().target("/foo"));

        assertNotNull(builder);

        assertEquals("org.apache.cxf.jaxrs.sse.client.SseEventSourceBuilderImpl", builder.getClass().getName());
    }

}
