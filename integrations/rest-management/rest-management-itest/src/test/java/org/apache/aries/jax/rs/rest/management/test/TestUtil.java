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

package org.apache.aries.jax.rs.rest.management.test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.osgi.service.jaxrs.runtime.JaxrsServiceRuntimeConstants.JAX_RS_SERVICE_ENDPOINT;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.apache.aries.jax.rs.rest.management.feature.RestManagementFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.common.service.ServiceAware;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;

@ExtendWith({
    BundleContextExtension.class,
    ServiceExtension.class
})
public class TestUtil {

    @InjectService
    public ClientBuilder clientBuilder;

    @InjectService(
        filter = "(%s=http*)",
        filterArguments = JAX_RS_SERVICE_ENDPOINT,
        timeout = 2000l
    )
    public ServiceAware<JaxrsServiceRuntime> jaxrsServiceRuntimeAware;

    @InjectBundleContext
    public BundleContext bundleContext;

    @BeforeEach
    public void beforeEach(@InjectBundleContext BundleContext bundleContext) {
        Stream.of(bundleContext.getBundles()).filter(b -> b.getSymbolicName().equals("minor-and-removed-change")).forEach(
            b -> {
                try {
                    b.uninstall();
                } catch (BundleException e) {
                    throw new RuntimeException(e);
                }
            }
        );
    }

    protected WebTarget createDefaultTarget() {
        clientBuilder.connectTimeout(600000, SECONDS);
        clientBuilder.readTimeout(600000, SECONDS);

        Client client = clientBuilder.build();

        client.register(RestManagementFeature.class);

        return client.target(runtimeURI());
    }

    protected URI runtimeURI() {
        return runtimeURI("rms");
    }

    protected URI runtimeURI(String... parts) {
        Object jaxRSServiceEndpointProperty =
            jaxrsServiceRuntimeAware.getServiceReference().getProperty(
                JAX_RS_SERVICE_ENDPOINT);

        String[] runtimes = canonicalize(jaxRSServiceEndpointProperty);

        if (runtimes.length == 0) {
            throw new IllegalStateException(
                "No runtimes could be found on \"osgi.jaxrs.endpoint\" " +
                    "runtime service property " + jaxRSServiceEndpointProperty);
        }

        String uriString = runtimes[0];

        for (String part : parts) {
            uriString += part + "/";
        }

        return URI.create(uriString);
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

    @SuppressWarnings("restriction")
    public static class HttpServer implements AutoCloseable {

        private final com.sun.net.httpserver.HttpServer server;

        public HttpServer(URL resource, String contentType) throws IOException {
            server = com.sun.net.httpserver.HttpServer.create(
                new InetSocketAddress("localhost", 0), 0);

            server.createContext("/", exchange -> {
                String requestURI = exchange.getRequestURI().getPath();

                if (!requestURI.equals(resource.getPath())) {
                    sendError(exchange, 404, "File not found");
                    return;
                }

                URLConnection connection = resource.openConnection();
                exchange.getResponseHeaders().set(
                    "Content-Type", contentType);
                exchange.sendResponseHeaders(
                    200, connection.getContentLength());

                OutputStream out = exchange.getResponseBody();
                InputStream in = connection.getInputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) >= 0) {
                    out.write(buf, 0, n);
                }
                out.close();
                in.close();
            });
            server.setExecutor(Executors.newSingleThreadExecutor());
            server.start();
        }

        private void sendError(
                com.sun.net.httpserver.HttpExchange exchange, int code,
                String description)
            throws IOException {

            String message = "HTTP error " + code + ": " + description;
            byte[] messageBytes = message.getBytes("UTF-8");

            exchange.getResponseHeaders().set(
                "Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(code, messageBytes.length);
            OutputStream out = exchange.getResponseBody();
            out.write(messageBytes);
            out.close();
        }

        public int getPort() {
            return server.getAddress().getPort();
        }

        @Override
        public void close() {
            server.stop(0);
        }

    }

}
