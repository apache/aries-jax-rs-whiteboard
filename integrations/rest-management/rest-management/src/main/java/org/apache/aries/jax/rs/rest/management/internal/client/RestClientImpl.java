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

package org.apache.aries.jax.rs.rest.management.internal.client;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.HttpHeaders.CONTENT_LOCATION;
import static javax.ws.rs.core.MediaType.*;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.*;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;

import org.apache.aries.jax.rs.rest.management.feature.RestManagementFeature;
import org.apache.aries.jax.rs.rest.management.schema.BundleExceptionSchema;
import org.apache.aries.jax.rs.rest.management.schema.BundleHeaderSchema;
import org.apache.aries.jax.rs.rest.management.schema.BundleSchema;
import org.apache.aries.jax.rs.rest.management.schema.BundleStateSchema;
import org.apache.aries.jax.rs.rest.management.schema.BundleListSchema;
import org.apache.aries.jax.rs.rest.management.schema.BundleSchemaListSchema;
import org.apache.aries.jax.rs.rest.management.schema.FrameworkStartLevelSchema;
import org.apache.aries.jax.rs.rest.management.schema.ServiceSchemaListSchema;
import org.apache.aries.jax.rs.rest.management.schema.ServiceListSchema;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.framework.startlevel.dto.BundleStartLevelDTO;
import org.osgi.framework.startlevel.dto.FrameworkStartLevelDTO;
import org.osgi.service.rest.client.RestClient;

public class RestClientImpl implements RestClient {

    private final WebTarget webTarget;

    public RestClientImpl(WebTarget webTarget) {
        this.webTarget = webTarget;

        this.webTarget.register(RestManagementFeature.class);
    }

    @Override
    public FrameworkStartLevelDTO getFrameworkStartLevel() throws Exception {
        try (Response response = maybeThrow(
            webTarget.path(
                "framework"
            ).path(
                "startlevel"
            ).request(
            ).get()
        )) {
            return response.readEntity(FrameworkStartLevelSchema.class);
        }
    }

    @Override
    public void setFrameworkStartLevel(FrameworkStartLevelDTO startLevel) throws Exception {
        try (Response response = maybeThrow(
            webTarget.path(
                "framework"
            ).path(
                "startlevel"
            ).request(
            ).put(
                Entity.entity(
                    FrameworkStartLevelSchema.build(startLevel), APPLICATION_FRAMEWORKSTARTLEVEL_JSON
                )
            )
        )) {}
    }

    @Override
    public Collection<String> getBundlePaths() throws Exception {
        try (Response response = maybeThrow(
            webTarget.path(
                "framework"
            ).path(
                "bundles"
            ).request(
            ).get()
        )) {
            return response.readEntity(BundleListSchema.class).bundles;
        }
    }

    @Override
    public Collection<BundleDTO> getBundles() throws Exception {
        try (Response response = maybeThrow(
            webTarget.path(
                "framework"
            ).path(
                "bundles"
            ).path(
                "representations"
            ).request(
            ).get()
        )) {
            return response.readEntity(
                BundleSchemaListSchema.class
            ).bundles.stream().map(
                BundleDTO.class::cast
            ).collect(toList());
        }
    }

    @Override
    public BundleDTO getBundle(long id) throws Exception {
        try (Response response = maybeThrow(
            webTarget.path(
                "framework"
            ).path(
                "bundle"
            ).path(
                "{bundleid}"
            ).resolveTemplate(
                "bundleid", id
            ).request(
            ).get()
        )) {
            return response.readEntity(BundleSchema.class);
        }
    }

    @Override
    public BundleDTO getBundle(String bundlePath) throws Exception {
        try (Response response = maybeThrow(
            webTarget.path(
                bundlePath
            ).request(
            ).get()
        )) {
            return response.readEntity(BundleSchema.class);
        }
    }

    @Override
    public int getBundleState(long id) throws Exception {
        try (Response response = maybeThrow(
            webTarget.path(
                "framework"
            ).path(
                "bundle"
            ).path(
                "{bundleid}"
            ).path(
                "state"
            ).resolveTemplate(
                "bundleid", id
            ).request(
            ).get()
        )) {
            return response.readEntity(BundleStateSchema.class).state;
        }
    }

    @Override
    public int getBundleState(String bundlePath) throws Exception {
        try (Response response = maybeThrow(
            webTarget.path(
                bundlePath
            ).path(
                "state"
            ).request(
            ).get()
        )) {
            return response.readEntity(BundleStateSchema.class).state;
        }
    }

    @Override
    public void startBundle(long id) throws Exception {
        BundleStateSchema bundleStateDTO = new BundleStateSchema();
        bundleStateDTO.state = Bundle.ACTIVE;

        try (Response response = maybeThrow(
            webTarget.path(
                "framework"
            ).path(
                "bundle"
            ).path(
                "{bundleid}"
            ).path(
                "state"
            ).resolveTemplate(
                "bundleid", id
            ).request(
            ).put(
                Entity.entity(
                    bundleStateDTO,
                    APPLICATION_BUNDLESTATE_JSON_TYPE
                )
            )
        )) {
        }
    }

    @Override
    public void startBundle(String bundlePath) throws Exception {
        BundleStateSchema bundleStateDTO = new BundleStateSchema();
        bundleStateDTO.state = Bundle.ACTIVE;

        try (Response response = maybeThrow(
            webTarget.path(
                bundlePath
            ).path(
                "state"
            ).request(
            ).put(
                Entity.entity(
                    bundleStateDTO,
                    APPLICATION_BUNDLESTATE_JSON_TYPE
                )
            )
        )) {
        }
    }

    @Override
    public void startBundle(long id, int options) throws Exception {
        BundleStateSchema bundleStateDTO = new BundleStateSchema();
        bundleStateDTO.state = Bundle.ACTIVE;
        bundleStateDTO.options = options;

        try (Response response = maybeThrow(
            webTarget.path(
                "framework"
            ).path(
                "bundle"
            ).path(
                "{bundleid}"
            ).path(
                "state"
            ).resolveTemplate(
                "bundleid", id
            ).request(
            ).put(
                Entity.entity(
                    bundleStateDTO,
                    APPLICATION_BUNDLESTATE_JSON_TYPE
                )
            )
        )) {
        }
    }

    @Override
    public void startBundle(String bundlePath, int options) throws Exception {
        BundleStateSchema bundleStateDTO = new BundleStateSchema();
        bundleStateDTO.state = Bundle.ACTIVE;
        bundleStateDTO.options = options;

        try (Response response = maybeThrow(
            webTarget.path(
                bundlePath
            ).path(
                "state"
            ).request(
            ).put(
                Entity.entity(
                    bundleStateDTO,
                    APPLICATION_BUNDLESTATE_JSON_TYPE
                )
            )
        )) {
        }
    }

    @Override
    public void stopBundle(long id) throws Exception {
        BundleStateSchema bundleStateDTO = new BundleStateSchema();
        bundleStateDTO.state = Bundle.RESOLVED;

        try (Response response = maybeThrow(
            webTarget.path(
                "framework"
            ).path(
                "bundle"
            ).path(
                "{bundleid}"
            ).path(
                "state"
            ).resolveTemplate(
                "bundleid", id
            ).request(
            ).put(
                Entity.entity(
                    bundleStateDTO,
                    APPLICATION_BUNDLESTATE_JSON_TYPE
                )
            )
        )) {
        }
    }

    @Override
    public void stopBundle(String bundlePath) throws Exception {
        BundleStateSchema bundleStateDTO = new BundleStateSchema();
        bundleStateDTO.state = Bundle.RESOLVED;

        try (Response response = maybeThrow(
            webTarget.path(
                bundlePath
            ).path(
                "state"
            ).request(
            ).put(
                Entity.entity(
                    bundleStateDTO,
                    APPLICATION_BUNDLESTATE_JSON_TYPE
                )
            )
        )) {
        }
    }

    @Override
    public void stopBundle(long id, int options) throws Exception {
        BundleStateSchema bundleStateDTO = new BundleStateSchema();
        bundleStateDTO.state = Bundle.RESOLVED;
        bundleStateDTO.options = options;

        try (Response response = maybeThrow(
            webTarget.path(
                "framework"
            ).path(
                "bundle"
            ).path(
                "{bundleid}"
            ).path(
                "state"
            ).resolveTemplate(
                "bundleid", id
            ).request(
            ).put(
                Entity.entity(
                    bundleStateDTO,
                    APPLICATION_BUNDLESTATE_JSON_TYPE
                )
            )
        )) {
        }
    }

    @Override
    public void stopBundle(String bundlePath, int options) throws Exception {
        BundleStateSchema bundleStateDTO = new BundleStateSchema();
        bundleStateDTO.state = Bundle.RESOLVED;
        bundleStateDTO.options = options;

        try (Response response = maybeThrow(
            webTarget.path(
                bundlePath
            ).path(
                "state"
            ).request(
            ).put(
                Entity.entity(
                    bundleStateDTO,
                    APPLICATION_BUNDLESTATE_JSON_TYPE
                )
            )
        )) {
        }
    }

    @Override
    public Map<String, String> getBundleHeaders(long id) throws Exception {
        try (Response response = maybeThrow(
            webTarget.path(
                "framework"
            ).path(
                "bundle"
            ).path(
                "{bundleid}"
            ).path(
                "header"
            ).resolveTemplate(
                "bundleid", id
            ).request(
            ).get()
        )) {
            return response.readEntity(BundleHeaderSchema.class);
        }
    }

    @Override
    public Map<String, String> getBundleHeaders(String bundlePath) throws Exception {
        try (Response response = maybeThrow(
            webTarget.path(
                bundlePath
            ).request(
            ).get()
        )) {
            return response.readEntity(BundleHeaderSchema.class);
        }
    }

    @Override
    public BundleStartLevelDTO getBundleStartLevel(long id) throws Exception {
        try (Response response = maybeThrow(
            webTarget.path(
                "framework"
            ).path(
                "bundle"
            ).path(
                "{bundleid}"
            ).path(
                "startlevel"
            ).resolveTemplate(
                "bundleid", id
            ).request(
            ).get()
        )) {
            return response.readEntity(BundleStartLevelDTO.class);
        }
    }

    @Override
    public BundleStartLevelDTO getBundleStartLevel(String bundlePath) throws Exception {
        try (Response response = maybeThrow(
            webTarget.path(
                bundlePath
            ).path(
                "startlevel"
            ).request(
            ).get()
        )) {
            return response.readEntity(BundleStartLevelDTO.class);
        }
    }

    @Override
    public void setBundleStartLevel(long id, int startLevel) throws Exception {
        BundleStartLevelDTO bundleStartLevelDTO = new BundleStartLevelDTO();
        bundleStartLevelDTO.startLevel = startLevel;

        try (Response response = maybeThrow(
            webTarget.path(
                "framework"
            ).path(
                "bundle"
            ).path(
                "{bundleid}"
            ).path(
                "startlevel"
            ).resolveTemplate(
                "bundleid", id
            ).request(
            ).put(
                Entity.entity(
                    bundleStartLevelDTO,
                    APPLICATION_BUNDLESTARTLEVEL_JSON_TYPE
                )
            )
        )) {
        }
    }

    @Override
    public void setBundleStartLevel(String bundlePath, int startLevel) throws Exception {
        BundleStartLevelDTO bundleStartLevelDTO = new BundleStartLevelDTO();
        bundleStartLevelDTO.startLevel = startLevel;

        try (Response response = maybeThrow(
            webTarget.path(
                bundlePath
            ).path(
                "startlevel"
            ).request(
            ).put(
                Entity.entity(
                    bundleStartLevelDTO,
                    APPLICATION_BUNDLESTARTLEVEL_JSON_TYPE
                )
            )
        )) {
        }
    }

    @Override
    public BundleDTO installBundle(String location) throws Exception {
        try (Response response = maybeThrow(() -> {
            Response r = webTarget.path(
                "framework"
            ).path(
                "bundles"
            ).request(
                APPLICATION_BUNDLE_JSON
            ).post(
                Entity.entity(location, TEXT_PLAIN)
            );

            StatusType statusInfo = r.getStatusInfo();
            if (statusInfo.getStatusCode() == 400) {
                BundleExceptionSchema bundleExceptionSchema = r.readEntity(BundleExceptionSchema.class);
                throw new BundleException(bundleExceptionSchema.message, bundleExceptionSchema.typecode);
            }

            return r;
        })) {
            return response.readEntity(
                BundleSchema.class
            );
        }
    }

    @Override
    public BundleDTO installBundle(String location, InputStream in) throws Exception {
        try (Response response = maybeThrow(
            webTarget.path(
                "framework"
            ).path(
                "bundles"
            ).request(
                APPLICATION_BUNDLE_JSON
            ).header(
                CONTENT_LOCATION, location
            ).post(
                Entity.entity(in, APPLICATION_OCTET_STREAM_TYPE)
            )
        )) {
            return response.readEntity(
                BundleSchema.class
            );
        }
    }

    @Override
    public BundleDTO uninstallBundle(long id) throws Exception {
        try (Response response = maybeThrow(
            webTarget.path(
                "framework"
            ).path(
                "bundle"
            ).path(
                "{bundleid}"
            ).resolveTemplate(
                "bundleid", id
            ).request(
                APPLICATION_BUNDLE_JSON
            ).delete()
        )) {
            return response.readEntity(
                BundleSchema.class
            );
        }
    }

    @Override
    public BundleDTO uninstallBundle(String bundlePath) throws Exception {
        try (Response response = maybeThrow(
            webTarget.path(
                bundlePath
            ).request(
                APPLICATION_BUNDLE_JSON
            ).delete()
        )) {
            return response.readEntity(
                BundleSchema.class
            );
        }
    }

    @Override
    public BundleDTO updateBundle(long id) throws Exception {
        try (Response response = maybeThrow(
            webTarget.path(
                "framework"
            ).path(
                "bundle"
            ).path(
                "{bundleid}"
            ).resolveTemplate(
                "bundleid", id
            ).request(
                APPLICATION_BUNDLE_JSON
            ).put(
                Entity.entity("", TEXT_PLAIN_TYPE)
            )
        )) {
            return response.readEntity(
                BundleSchema.class
            );
        }
    }

    @Override
    public BundleDTO updateBundle(long id, String url) throws Exception {
        try (Response response = maybeThrow(
            webTarget.path(
                "framework"
            ).path(
                "bundle"
            ).path(
                "{bundleid}"
            ).resolveTemplate(
                "bundleid", id
            ).request(
                APPLICATION_BUNDLE_JSON
            ).put(
                Entity.entity(url, TEXT_PLAIN_TYPE)
            )
        )) {
            return response.readEntity(
                BundleSchema.class
            );
        }
    }

    @Override
    public BundleDTO updateBundle(long id, InputStream in) throws Exception {
        try (Response response = maybeThrow(
            webTarget.path(
                "framework"
            ).path(
                "bundle"
            ).path(
                "{bundleid}"
            ).resolveTemplate(
                "bundleid", id
            ).request(
                APPLICATION_BUNDLE_JSON
            ).put(
                Entity.entity(in, APPLICATION_OCTET_STREAM_TYPE)
            )
        )) {
            return response.readEntity(
                BundleSchema.class
            );
        }
    }

    @Override
    public Collection<String> getServicePaths() throws Exception {
        try (Response response = maybeThrow(
            webTarget.path(
                "framework"
            ).path(
                "services"
            ).request(
                APPLICATION_SERVICES_JSON_TYPE
            ).get()
        )) {
            return response.readEntity(
                ServiceListSchema.class
            ).services;
        }
    }

    @Override
    public Collection<String> getServicePaths(String filter) throws Exception {
        try (Response response = maybeThrow(
            webTarget.path(
                "framework"
            ).path(
                "services"
            ).queryParam(
                "filter", filter
            ).request(
                APPLICATION_SERVICES_JSON_TYPE
            ).get()
        )) {
            return response.readEntity(
                ServiceListSchema.class
            ).services;
        }
    }

    @Override
    public Collection<ServiceReferenceDTO> getServiceReferences() throws Exception {
        try (Response response = maybeThrow(
            webTarget.path(
                "framework"
            ).path(
                "services"
            ).path(
                "representations"
            ).request(
                APPLICATION_SERVICES_REPRESENTATIONS_JSON_TYPE
            ).get()
        )) {
            return response.readEntity(
                ServiceSchemaListSchema.class
            ).services.stream().map(
                ServiceReferenceDTO.class::cast
            ).collect(toList());
        }
    }

    @Override
    public Collection<ServiceReferenceDTO> getServiceReferences(String filter) throws Exception {
        try (Response response = maybeThrow(
            webTarget.path(
                "framework"
            ).path(
                "services"
            ).path(
                "representations"
            ).queryParam(
                "filter", filter
            ).request(
                APPLICATION_SERVICES_REPRESENTATIONS_JSON_TYPE
            ).get()
        )) {
            return response.readEntity(
                ServiceSchemaListSchema.class
            ).services.stream().map(
                ServiceReferenceDTO.class::cast
            ).collect(toList());
        }
    }

    @Override
    public ServiceReferenceDTO getServiceReference(long id) throws Exception {
        try (Response response = maybeThrow(
            webTarget.path(
                "framework"
            ).path(
                "service"
            ).path(
                "{serviceid}"
            ).resolveTemplate(
                "serviceid", id
            ).request(
                APPLICATION_SERVICE_JSON_TYPE
            ).get()
        )) {
            return response.readEntity(
                ServiceReferenceDTO.class
            );
        }
    }

    @Override
    public ServiceReferenceDTO getServiceReference(String servicePath) throws Exception {
        try (Response response = maybeThrow(
            webTarget.path(
                servicePath
            ).request(
                APPLICATION_SERVICE_JSON_TYPE
            ).get()
        )) {
            return response.readEntity(
                ServiceReferenceDTO.class
            );
        }
    }

    private Response maybeThrow(Response response) throws Exception {
        return maybeThrow(() -> response);
    }

    private Response maybeThrow(Callable<Response> alternate) throws Exception {
        Response response = alternate.call();

        StatusType statusInfo = response.getStatusInfo();

        if (statusInfo.getStatusCode() >= 300) {
            throw new Exception(statusInfo.getReasonPhrase());
        }

        return response;
    }

}
