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
import java.util.Map;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.ext.RuntimeDelegate;
import javax.ws.rs.sse.SseEventSource;

import org.apache.aries.component.dsl.Transformer;
import org.apache.aries.component.dsl.internal.OnlyLastPublisher;
import org.apache.aries.jax.rs.whiteboard.internal.client.ClientBuilderFactory;
import org.apache.aries.jax.rs.whiteboard.internal.utils.PropertyHolder;
import org.apache.aries.component.dsl.OSGi;
import org.apache.aries.component.dsl.OSGiResult;
import org.apache.cxf.jaxrs.impl.RuntimeDelegateImpl;
import org.apache.cxf.jaxrs.sse.client.SseEventSourceBuilderImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.jaxrs.client.SseEventSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static org.apache.aries.component.dsl.OSGi.configuration;
import static org.apache.aries.component.dsl.OSGi.services;
import static org.apache.aries.jax.rs.whiteboard.internal.utils.LogUtils.ifInfoEnabled;
import static org.apache.aries.jax.rs.whiteboard.internal.utils.LogUtils.debugTracking;
import static org.apache.aries.jax.rs.whiteboard.internal.utils.Utils.canonicalize;
import static org.apache.aries.jax.rs.whiteboard.internal.Whiteboard.createWhiteboard;
import static org.apache.aries.component.dsl.OSGi.all;
import static org.apache.aries.component.dsl.OSGi.configurations;
import static org.apache.aries.component.dsl.OSGi.effects;
import static org.apache.aries.component.dsl.OSGi.ignore;
import static org.apache.aries.component.dsl.OSGi.just;
import static org.apache.aries.component.dsl.OSGi.once;
import static org.apache.aries.component.dsl.OSGi.register;
import static org.apache.aries.component.dsl.OSGi.serviceReferences;
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
            configurations("org.apache.aries.jax.rs.whiteboard").
                effects(
                    debugTracking(_log, () -> "whiteboard configuration")
                ).flatMap(configuration ->
                runWhiteboard(bundleContext, configuration)
            );

        _whiteboardsResult = whiteboards.run(bundleContext);

        _defaultOSGiResult =
            all(
                ignore(registerClient()),
                ignore(registerSseEventSourceFactory()),
                ignore(
                    defaultWhiteboardConfiguration().flatMap(
                        c -> runWhiteboard(bundleContext, c)
                    )
                )
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

    private static OSGi<Dictionary<String, ?>>
        defaultWhiteboardConfiguration() {

        return once(services(ConfigurationAdmin.class)).flatMap(cm -> {
            Configuration[] configurations;

            try {
                configurations = cm.listConfigurations(
                    "(service.pid=org.apache.aries.jax.rs.whiteboard.default)");
            }
            catch (Exception e) {
                e.printStackTrace();

                throw new RuntimeException(e);
            }

            Dictionary<String, Object> properties;

            if (configurations == null || configurations.length == 0) {
                properties = new Hashtable<>();

                properties.put(
                    Constants.SERVICE_PID,
                    "org.apache.aries.jax.rs.whiteboard.default");
            }
            else {
                properties = configurations[0].getProperties();
            }

            Transformer<Dictionary<String, ?>, Dictionary<String, ?>> onlyLast =
                op -> new OnlyLastPublisher<>(op, () -> properties);

            return
                all(
                    just(properties),
                    configuration("org.apache.aries.jax.rs.whiteboard.default").
                    filter(c -> !properties.equals(c))
                ).
                transform(onlyLast);
        });
    }

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

    private static OSGi<?> registerClient() {
        return register(
            ClientBuilder.class, new ClientBuilderFactory(),
            (Map<String, Object>) null).
            effects(
                ifInfoEnabled(_log, () -> "Registered ClientBuilder"),
                ifInfoEnabled(_log, () -> "Unregistered ClientBuilder")
            );
    }

    private static OSGi<?> registerSseEventSourceFactory() {
        return register(
            SseEventSourceFactory.class, new SseEventSourceFactory() {
                @Override
                public SseEventSource.Builder newBuilder(WebTarget target) {
                    return new SseEventSourceBuilderImpl(){{target(target);}};
                }

                @Override
                public SseEventSource newSource(WebTarget target) {
                    return newBuilder(target).build();
                }
            },
            new Hashtable<>()).
            effects(
                ifInfoEnabled(_log, () -> "Registered SseEventSourceFactory"),
                ifInfoEnabled(_log, () -> "Unregistered SseEventSourceFactory")
            );
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
                    just(createWhiteboard(bundleContext, configuration)).
                    effects(
                        ifInfoEnabled(
                            _log,
                            () -> "created whiteboard from configuration: " +
                                configuration),
                        ifInfoEnabled(
                            _log,
                            () -> "destroyed whiteboard from configuration: " +
                                configuration)
                    )
                .flatMap(
                    whiteboard ->
                        all(
                            effects(whiteboard::start, whiteboard::stop),
                            ignore(
                                endpoints.effects(
                                    whiteboard::addHttpEndpoints,
                                    whiteboard::removeHttpEndpoints)
                            ).
                            effects(
                                debugTracking(
                                    _log,
                                    () -> "endpoint for whiteboard: " +
                                        whiteboard)
                            )
                        )
                ))
            );
    }

}