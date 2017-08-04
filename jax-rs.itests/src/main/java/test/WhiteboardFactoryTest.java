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
import static org.junit.Assert.assertTrue;
import static org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE;
import static org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants.JAX_RS_RESOURCE;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.jaxrs.runtime.JaxRSServiceRuntime;
import org.osgi.util.tracker.ServiceTracker;
import test.types.TestApplication;

import javax.ws.rs.core.Application;

public class WhiteboardFactoryTest {

    @Test
    public void testDefaultDefaultWhiteboardConfig() throws Exception {
        ServiceTracker<JaxRSServiceRuntime, JaxRSServiceRuntime> runtimeTracker =
            new ServiceTracker<>(
                bundleContext, JaxRSServiceRuntime.class, null);

        try {
            runtimeTracker.open();

            JaxRSServiceRuntime runtime = runtimeTracker.waitForService(5000);

            assertNotNull(runtime);

            ServiceReference<JaxRSServiceRuntime> serviceReference = runtimeTracker.getServiceReference();

            assertNotNull(serviceReference);

            assertEquals(1, runtimeTracker.size());
        }
        finally {
            runtimeTracker.close();
        }
    }

    @Test
    public void testCreateNewInstance() throws Exception {
        ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> configTracker =
            new ServiceTracker<>(
                bundleContext, ConfigurationAdmin.class, null);

        ServiceTracker<JaxRSServiceRuntime, JaxRSServiceRuntime> runtimeTracker =
            new ServiceTracker<>(
                bundleContext, JaxRSServiceRuntime.class, null);

        try {
            configTracker.open();
            runtimeTracker.open();

            JaxRSServiceRuntime runtime = runtimeTracker.waitForService(5000);

            assertNotNull(runtime);

            ServiceReference<JaxRSServiceRuntime> serviceReference = runtimeTracker.getServiceReference();

            assertNotNull(serviceReference);

            assertEquals(1, runtimeTracker.size());

            int trackingCount = runtimeTracker.getTrackingCount();

            ConfigurationAdmin admin = configTracker.waitForService(5000);

            Configuration configuration = admin.createFactoryConfiguration(
                "org.apache.aries.jax.rs.whiteboard", "?");

            configuration.update(new Hashtable<>());

            do {
                Thread.sleep(50);
            }
            while (runtimeTracker.getTrackingCount() <= trackingCount);

            assertEquals(2, runtimeTracker.size());

            configuration.delete();

            do {
                Thread.sleep(50);
            }
            while (runtimeTracker.getTrackingCount() <= trackingCount);

            assertEquals(1, runtimeTracker.size());
        }
        finally {
            runtimeTracker.close();
            configTracker.close();
        }
    }

    @Test
    public void testChangeCount() throws Exception {
        ServiceTracker<JaxRSServiceRuntime, JaxRSServiceRuntime> runtimeTracker =
            new ServiceTracker<>(
                bundleContext, JaxRSServiceRuntime.class, null);

        try {
            runtimeTracker.open();

            JaxRSServiceRuntime runtime = runtimeTracker.waitForService(5000);

            assertNotNull(runtime);

            ServiceReference<JaxRSServiceRuntime> serviceReference = runtimeTracker.getServiceReference();

            Long changeCount = (Long)serviceReference.getProperty("service.changecount");

            Dictionary<String, Object> properties = new Hashtable<>();

            properties.put(JAX_RS_APPLICATION_BASE, "/test-counter");

            ServiceRegistration<?> serviceRegistration =
                bundleContext.registerService(
                    Application.class, new TestApplication(), properties);

            Long newCount = (Long)serviceReference.getProperty("service.changecount");

            assertTrue(changeCount < newCount);

            changeCount = newCount;

            serviceRegistration.unregister();

            newCount = (Long)serviceReference.getProperty("service.changecount");

            assertTrue(changeCount < newCount);
        }
        finally {
            runtimeTracker.close();
        }
    }

    private BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();

}