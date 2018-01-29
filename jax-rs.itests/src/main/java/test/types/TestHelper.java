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
import javax.ws.rs.client.WebTarget;

import org.junit.After;
import org.junit.Before;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;
import org.osgi.util.tracker.ServiceTracker;

import java.util.Collection;

public class TestHelper {

    public static BundleContext bundleContext =
        FrameworkUtil.
            getBundle(TestHelper.class).
            getBundleContext();

    protected ServiceTracker<JaxrsServiceRuntime, JaxrsServiceRuntime>
        _runtimeTracker;
    protected ServiceTracker<ClientBuilder, ClientBuilder>
        _clientBuilderTracker;
    protected JaxrsServiceRuntime _runtime;
    protected ServiceReference<JaxrsServiceRuntime> _runtimeServiceReference;

    @After
    public void after() {
        _runtimeTracker.close();

        _clientBuilderTracker.close();
    }

    @Before
    public void before() {
        _clientBuilderTracker = new ServiceTracker<>(
            bundleContext, ClientBuilder.class, null);

        _clientBuilderTracker.open();

        _runtimeTracker = new ServiceTracker<>(
            bundleContext, JaxrsServiceRuntime.class, null);

        _runtimeTracker.open();

        try {
            _runtime = _runtimeTracker.waitForService(15000L);
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        _runtimeServiceReference = _runtimeTracker.getServiceReference();
    }

    @SuppressWarnings("unchecked")
	private static String[] canonicalize(Object propertyValue) {
        if (propertyValue == null) {
            return new String[0];
        }
        if (propertyValue instanceof String[]) {
            return (String[]) propertyValue;
        }
        if (propertyValue instanceof Collection) {
            return ((Collection<String>) propertyValue).toArray(new String[0]);
        }

        return new String[]{propertyValue.toString()};
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

    protected WebTarget createDefaultTarget() {
        Client client = createClient();

        String[] runtimes = canonicalize(
            _runtimeServiceReference.getProperty("osgi.jaxrs.endpoint"));

        if (runtimes.length == 0) {
            throw new IllegalStateException(
                "No runtimes could be found on \"osgi.jaxrs.endpoint\" " +
                    "runtime service property ");
        }

        String runtime = runtimes[0];

        return client.target(runtime);
    }

}
