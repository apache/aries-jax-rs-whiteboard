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

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import junit.framework.TestCase;
import test.types.TestAddon;

public class JaxrsTest extends TestCase {

	static BundleContext bundleContext = FrameworkUtil.getBundle(
		JaxrsTest.class).getBundleContext();

	public void testEndPoint() throws Exception {
		ServiceRegistration<?> serviceRegistration = null;

		try {
			TestAddon testAddon = new TestAddon();

			Dictionary<String, Object> properties = new Hashtable<>();
			properties.put("osgi.jaxrs.resource.base", "/test-addon");

			serviceRegistration = bundleContext.registerService(
				Object.class, testAddon, properties);

			// TODO this availability should be checked through a jaxrs runtime service

			Filter filter = bundleContext.createFilter("(CXF_ENDPOINT_ADDRESS=/test-addon)");

			ServiceTracker<?, ?> st = new ServiceTracker<>(bundleContext, filter, null);

			st.open();

			if (st.waitForService(5000) == null) {
				fail();
			}

			// TODO add http client to connect to the endpoint
		}
		finally {
			if (serviceRegistration != null) {
				serviceRegistration.unregister();
			}
		}
	}

}