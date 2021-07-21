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

import org.apache.cxf.bus.osgi.CXFActivator;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import test.types.TestHelper;

public class WhiteboardLifecycleTest extends TestHelper {

    @Test
    public void testCXFLifecycle() throws Exception {
    	assertNotNull(_runtimeTracker.getService());
    	
    	Bundle cxfBundle = FrameworkUtil.getBundle(CXFActivator.class);
		cxfBundle.stop();
		
		assertNull(_runtimeTracker.getService());
		
		cxfBundle.start();
		
		assertNotNull(_runtimeTracker.waitForService(5000));
    }

}