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

package org.apache.aries.jax.rs.whiteboard.activator;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.aries.jax.rs.whiteboard.internal.ClientBuilderFactory;
import org.apache.aries.jax.rs.whiteboard.internal.Utils.PropertyHolder;
import org.apache.aries.osgi.functional.OSGi;
import org.apache.aries.osgi.functional.OSGiResult;
import org.apache.cxf.jaxrs.impl.RuntimeDelegateImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.canonicalize;
import static org.apache.aries.jax.rs.whiteboard.internal.Utils.ignoreResult;
import static org.apache.aries.jax.rs.whiteboard.internal.Whiteboard.createWhiteboard;
import static org.apache.aries.osgi.functional.OSGi.all;
import static org.apache.aries.osgi.functional.OSGi.configurations;
import static org.apache.aries.osgi.functional.OSGi.effects;
import static org.apache.aries.osgi.functional.OSGi.ignore;
import static org.apache.aries.osgi.functional.OSGi.just;
import static org.apache.aries.osgi.functional.OSGi.once;
import static org.apache.aries.osgi.functional.OSGi.register;
import static org.apache.aries.osgi.functional.OSGi.serviceReferences;
import static org.osgi.service.http.runtime.HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET;

public class CxfJaxrsBundleActivator implements BundleActivator {

    private static final Logger _log = LoggerFactory.getLogger(
        CxfJaxrsBundleActivator.class);

    static {
        RuntimeDelegate.setInstance(new RuntimeDelegateImpl());
    }

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        if (_log.isDebugEnabled()) {
            _log.debug("Starting the whiteboard factory");
        }

        OSGi<?> whiteboards =
            configurations("org.apache.aries.jax.rs.whiteboard").flatMap(
                configuration -> runWhiteboard(bundleContext, configuration)
            );

        _whiteboardsResult = whiteboards.run(bundleContext);

        Dictionary<String, Object> defaultConfiguration = new Hashtable<>();

        defaultConfiguration.put(
            Constants.SERVICE_PID,
            "org.apache.aries.jax.rs.whiteboard.default");

        _defaultOSGiResult =
            all(
                ignoreResult(
                    register(
                        ClientBuilder.class, new ClientBuilderFactory(), null)),
                ignoreResult(runWhiteboard(bundleContext, defaultConfiguration))
            )
        .run(bundleContext);

        if (_log.isDebugEnabled()) {
            _log.debug("Whiteboard factory started");
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (_log.isDebugEnabled()) {
            _log.debug("Stopping whiteboard factory");
        }

        _defaultOSGiResult.close();

        _whiteboardsResult.close();

        if (_log.isDebugEnabled()) {
            _log.debug("Stopped whiteboard factory");
        }
    }
    private OSGiResult _defaultOSGiResult;
    private OSGiResult _whiteboardsResult;

    private static String endpointFilter(PropertyHolder configuration ) {

        Object whiteBoardTargetProperty = configuration.get(
            HTTP_WHITEBOARD_TARGET);

        String targetFilter =
            whiteBoardTargetProperty != null ?
                whiteBoardTargetProperty.toString() :
                "(osgi.http.endpoint=*)";


        return format(
            "(&(objectClass=%s)%s)", HttpServiceRuntime.class.getName(),
            targetFilter);
    }

    private static OSGi<?> runWhiteboard(
        BundleContext bundleContext, Dictionary<String, ?> configuration) {

        OSGi<List<String>> endpoints =
            serviceReferences(endpointFilter(configuration::get)
            ).map(
                r -> Arrays.asList(
                    canonicalize(r.getProperty(HTTP_SERVICE_ENDPOINT)))
            );

        return
            once(
                serviceReferences(
                    endpointFilter(configuration::get),
                    __ -> false //never reload
                ).then(
                    just(createWhiteboard(bundleContext, configuration))
                .flatMap(
                    whiteboard ->
                        all(
                            ignore(
                                endpoints.effects(
                                    whiteboard::addHttpEndpoints,
                                    whiteboard::removeHttpEndpoints)
                            ),
                            effects(whiteboard::start, whiteboard::stop)
                        )
                ))
            );
    }

}