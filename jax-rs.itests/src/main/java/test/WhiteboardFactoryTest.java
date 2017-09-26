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

import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.jaxrs.runtime.JaxRSServiceRuntime;
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

        try {
            configTracker.open();

            ServiceReference<JaxRSServiceRuntime> serviceReference =
                _runtimeTracker.getServiceReference();

            assertNotNull(serviceReference);

            assertEquals(1, _runtimeTracker.size());

            int trackingCount = _runtimeTracker.getTrackingCount();

            ConfigurationAdmin admin = configTracker.waitForService(5000);

            Configuration configuration = admin.createFactoryConfiguration(
                "org.apache.aries.jax.rs.whiteboard", "?");

            Dictionary<String, Object> properties = new Hashtable<>();

            properties.put(
                HTTP_WHITEBOARD_SERVLET_PATTERN,
                "/new-whiteboard");
            properties.put(Constants.SERVICE_RANKING, 1000);

            configuration.update(properties);

            do {
                Thread.sleep(50);

                if (!"/new-whiteboard".equals(
                        _runtimeTracker.getServiceReference().getProperty(
                            HTTP_WHITEBOARD_SERVLET_PATTERN))) {

                    trackingCount = _runtimeTracker.getTrackingCount();
                }
            }
            while (_runtimeTracker.getTrackingCount() <= trackingCount);

            assertEquals(2, _runtimeTracker.size());

            trackingCount = _runtimeTracker.getTrackingCount();

            configuration.delete();

            do {
                Thread.sleep(50);
            }
            while (_runtimeTracker.getTrackingCount() <= trackingCount);

            assertEquals(1, _runtimeTracker.size());
        }
        finally {
            configTracker.close();
        }
    }



    private BundleContext bundleContext =
        FrameworkUtil.getBundle(getClass()).getBundleContext();

}