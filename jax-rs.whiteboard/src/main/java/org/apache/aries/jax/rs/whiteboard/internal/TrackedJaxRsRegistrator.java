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
package org.apache.aries.jax.rs.whiteboard.internal;

import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class TrackedJaxRsRegistrator {
    private final CXFJaxRsServiceRegistrator _registrator;
    private final ServiceRegistration<?> _sreg;

    public TrackedJaxRsRegistrator(CXFJaxRsServiceRegistrator cxfJaxRsServiceRegistrator, 
                                   BundleContext bundleContext, 
                                   Map<String, Object> properties) {
        _registrator = cxfJaxRsServiceRegistrator;
        _sreg = bundleContext.
            registerService(CXFJaxRsServiceRegistrator.class, 
                            cxfJaxRsServiceRegistrator,
                            new Hashtable<>(properties));
    }



    public void close() {
        _registrator.close();
        _sreg.unregister();
    }
}