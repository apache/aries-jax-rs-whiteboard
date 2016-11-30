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

import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import test.types.TestAddon;

public class JaxrsTest {

    static BundleContext bundleContext = FrameworkUtil.getBundle(
        JaxrsTest.class).getBundleContext();
	
	@Test
    public void testEndPoint() throws Exception {
        ServiceRegistration<?> serviceRegistration = null;

        try {
            TestAddon testAddon = new TestAddon();

            Dictionary<String, Object> properties = new Hashtable<>();
            properties.put("osgi.jaxrs.resource.base", "/test-addon");

            serviceRegistration = bundleContext.registerService(
                Object.class, testAddon, properties);

        }
        finally {
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
            }
        }
    }


    private Client createClient() {
        Thread thread = Thread.currentThread();

        ClassLoader contextClassLoader = thread.getContextClassLoader();

        try {
            thread.setContextClassLoader(
                org.apache.cxf.jaxrs.client.Client.class.getClassLoader());

            return ClientBuilder.newClient();
        }
        finally {
            thread.setContextClassLoader(contextClassLoader);
        }
    }

}
