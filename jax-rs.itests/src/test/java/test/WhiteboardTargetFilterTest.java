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

import java.util.Dictionary;
import java.util.Hashtable;

import javax.ws.rs.client.WebTarget;

import org.junit.Test;
import org.osgi.framework.ServiceRegistration;

import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

import test.types.TestAddon;
import test.types.TestHelper;

public class WhiteboardTargetFilterTest extends TestHelper {

    @Test
    public void testInvalidTargetFilter() throws Exception {
        WebTarget webTarget = createDefaultTarget().path("extended");

        assertEquals(
            "This should return nothing", 404,
            webTarget.request().get().getStatus());

        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
        properties.put(JaxrsWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET, "//");

        ServiceRegistration<Object> serviceRegistration =
            bundleContext.registerService(
                Object.class, new TestAddon(), properties);

        try {
            assertEquals(
                "This should return nothing", 404,
                webTarget.request().get().getStatus());
        }
        finally {
            serviceRegistration.unregister();
        }
    }

    @Test
    public void testNonMatchingFilter() throws Exception {
        WebTarget webTarget = createDefaultTarget().path("extended");

        assertEquals(
            "This should return nothing", 404,
            webTarget.request().get().getStatus());

        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
        properties.put(
            JaxrsWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET,
            "(crazy=the joker)");

        ServiceRegistration<Object> serviceRegistration =
            bundleContext.registerService(
                Object.class, new TestAddon(), properties);

        try {
            assertEquals(
                "This should return nothing", 404,
                webTarget.request().get().getStatus());
        }
        finally {
            serviceRegistration.unregister();
        }
    }

    @Test
    public void testMatchingFilter() throws Exception {
        WebTarget webTarget = createDefaultTarget().path("extended");

        assertEquals(
            "This should return nothing", 404,
            webTarget.request().get().getStatus());

        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
        properties.put(
            JaxrsWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET,
            "(service.pid=org.apache.aries.jax.rs.whiteboard.default)");

        ServiceRegistration<Object> serviceRegistration =
            bundleContext.registerService(
                Object.class, new TestAddon(), properties);

        try {
            assertEquals(
                "This should say hello", "Hello extended",
                webTarget.request().get().readEntity(String.class));
        }
        finally {
            serviceRegistration.unregister();
        }
    }

}
