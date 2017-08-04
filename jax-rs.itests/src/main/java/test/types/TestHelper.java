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

package test.types;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.junit.After;
import org.junit.Before;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

public class TestHelper {

    public static BundleContext bundleContext = FrameworkUtil.getBundle(
        TestHelper.class).getBundleContext();

    private ServiceTracker<ClientBuilder, ClientBuilder> _clientBuilderTracker;

    @After
    public void after() {
        _clientBuilderTracker.close();
    }

    @Before
    public void before() {
        _clientBuilderTracker = new ServiceTracker<>(bundleContext, ClientBuilder.class, null);

        _clientBuilderTracker.open();
    }

    protected Client createClient() {
        ClientBuilder clientBuilder;

        try {
            clientBuilder = _clientBuilderTracker.waitForService(5000);

            return clientBuilder.build();
        }
        catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
    }

}
