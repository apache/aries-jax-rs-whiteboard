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

import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

public class WhiteboardServiceFactory implements ManagedServiceFactory {

    public WhiteboardServiceFactory(BundleContext bundleContext) {
        _bundleContext = bundleContext;
    }

    @Override
    public String getName() {
        return "org.apache.aries.jax.rs.whiteboard";
    }

    @Override
    @SuppressWarnings("unchecked")
    public void updated(String pid, Dictionary<String, ?> configuration)
        throws ConfigurationException {

        Whiteboard whiteboard = _whiteboards.remove(pid);

        if (whiteboard != null) {
            whiteboard.close();
        }

        _whiteboards.put(
            pid, new Whiteboard(_bundleContext, (Map<String, Object>)Maps.from(configuration)));
    }

    @Override
    public void deleted(String pid) {
        Whiteboard whiteboard = _whiteboards.remove(pid);

        if (whiteboard != null) {
            whiteboard.close();
        }
    }

    private final BundleContext _bundleContext;
    private final Map<String, Whiteboard> _whiteboards = new ConcurrentHashMap<>();

}