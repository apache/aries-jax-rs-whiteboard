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

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;
import org.osgi.util.tracker.ServiceTracker;
import test.types.TestHelper;

public class WhiteboardFactoryTest extends TestHelper {

    @Test
    public void testDefaultDefaultWhiteboardConfig() throws Exception {
        assertEquals(1, _runtimeTracker.size());
    }

    @Test
    public void testCreateNewInstance() throws Exception {
        ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> configTracker =
            new ServiceTracker<>(
                bundleContext, ConfigurationAdmin.class, null);

        CountDownLatch addedCountLatch = new CountDownLatch(1);
        CountDownLatch removedCountLatch = new CountDownLatch(1);

        ServiceTracker<?, ?> serviceTracker = new ServiceTracker
            <JaxrsServiceRuntime, JaxrsServiceRuntime>(
            bundleContext, JaxrsServiceRuntime.class, null) {

            @Override
            public JaxrsServiceRuntime addingService(
                ServiceReference<JaxrsServiceRuntime> reference) {

                if ("/new-whiteboard".equals(
                    reference.getProperty(
                        HTTP_WHITEBOARD_SERVLET_PATTERN))) {

                    addedCountLatch.countDown();

                    return super.addingService(reference);
                }

                return null;
            }

            @Override
            public void removedService(
                ServiceReference<JaxrsServiceRuntime> reference,
                JaxrsServiceRuntime service) {

                removedCountLatch.countDown();
            }
        };

        try {
            configTracker.open();

            serviceTracker.open();

            ServiceReference<JaxrsServiceRuntime> serviceReference =
                _runtimeTracker.getServiceReference();

            assertNotNull(serviceReference);

            assertEquals(1, _runtimeTracker.size());

            ConfigurationAdmin admin = configTracker.waitForService(5000);

            Configuration configuration = admin.createFactoryConfiguration(
                "org.apache.aries.jax.rs.whiteboard", "?");

            Dictionary<String, Object> properties = new Hashtable<>();

            properties.put(
                HTTP_WHITEBOARD_SERVLET_PATTERN,
                "/new-whiteboard");
            properties.put(Constants.SERVICE_RANKING, 1000);

            configuration.update(properties);

            addedCountLatch.await(1, TimeUnit.MINUTES);

            assertEquals(2, _runtimeTracker.size());

            configuration.delete();

            removedCountLatch.await(1, TimeUnit.MINUTES);

            assertEquals(1, _runtimeTracker.size());
        }
        finally {
            configTracker.close();

            serviceTracker.close();
        }
    }

    private BundleContext bundleContext =
        FrameworkUtil.getBundle(getClass()).getBundleContext();

}