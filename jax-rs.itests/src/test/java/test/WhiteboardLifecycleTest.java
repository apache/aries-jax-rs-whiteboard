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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.ws.rs.client.ClientBuilder;

import org.apache.cxf.bus.osgi.CXFActivator;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.jaxrs.client.SseEventSourceFactory;
import org.osgi.util.tracker.ServiceTracker;

import test.types.TestHelper;

public class WhiteboardLifecycleTest extends TestHelper {

    @Test
    public void testCXFLifecycleWhiteboard() throws Exception {
    	testTracking(_runtimeTracker);
    }

	private void testTracking(ServiceTracker<?,?> tracker) throws BundleException, InterruptedException {
		assertNotNull(tracker.waitForService(5000));
    	
    	Bundle cxfBundle = FrameworkUtil.getBundle(CXFActivator.class);
		cxfBundle.stop();
		try {
			assertNull(tracker.getService());
		} finally {
			cxfBundle.start();
		}
		
		assertNotNull(tracker.waitForService(5000));
	}
    
    @Test
    public void testCXFLifecycleClientBuilder() throws Exception {
    	ServiceTracker<ClientBuilder, ClientBuilder> tracker = new ServiceTracker<>(bundleContext, ClientBuilder.class, null);
    	tracker.open();
    	
    	testTracking(tracker);
    	
    	tracker.close();
    }

    @Test
    public void testCXFLifecycleSseBuilderFactory() throws Exception {
    	ServiceTracker<SseEventSourceFactory, SseEventSourceFactory> tracker = new ServiceTracker<>(bundleContext, SseEventSourceFactory.class, null);
    	tracker.open();
    	
    	testTracking(tracker);
    	
    	tracker.close();
    }

}