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

package org.apache.aries.jax.rs.rest.management.internal;

import static javax.ws.rs.core.MediaType.*;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.*;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;

import org.apache.aries.jax.rs.rest.management.handler.RestManagementMessageBodyHandler;
import org.apache.aries.jax.rs.rest.management.model.BundleStateDTO;
import org.apache.aries.jax.rs.rest.management.model.BundlesDTO;
import org.osgi.framework.Bundle;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.framework.startlevel.dto.BundleStartLevelDTO;
import org.osgi.framework.startlevel.dto.FrameworkStartLevelDTO;
import org.osgi.service.rest.client.RestClient;

public class RestClientImpl implements RestClient {

    private static final GenericType<List<BundleDTO>> BUNDLE_LIST =
        new GenericType<List<BundleDTO>>() {};

    private final WebTarget webTarget;

    public RestClientImpl(WebTarget webTarget) {
        this.webTarget = webTarget;

        this.webTarget.register(RestManagementMessageBodyHandler.class);
    }

    @Override
    public FrameworkStartLevelDTO getFrameworkStartLevel() throws Exception {
        try (Response response = maybeThrow(
            webTarget.path(
                "framework"
            ).path(
                "startlevel"
            ).request().get()
        )) {
            return response.readEntity(FrameworkStartLevelDTO.class);
        }
    }

    @Override
    public void setFrameworkStartLevel(FrameworkStartLevelDTO startLevel) throws Exception {
        try (Response response = maybeThrow(
            webTarget.path(
                "framework"
            ).path(
                "startlevel"
            ).request().put(
                Entity.entity(
                    startLevel, APPLICATION_FRAMEWORKSTARTLEVEL_JSON)
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
            ).request().get()
        )) {
            return response.readEntity(BundlesDTO.class).bundles;
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
            ).request().get()
        )) {
            return response.readEntity(BUNDLE_LIST);
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
            ).request().get()
        )) {
            return response.readEntity(BundleDTO.class);
        }
    }

    @Override
    public BundleDTO getBundle(String bundlePath) throws Exception {
        try (Response response = maybeThrow(
            webTarget.path(
                bundlePath
            ).request().get()
        )) {
            return response.readEntity(BundleDTO.class);
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
            ).request().get()
        )) {
            return response.readEntity(BundleStateDTO.class).state;
        }
    }

    @Override
    public int getBundleState(String bundlePath) throws Exception {
        try (Response response = maybeThrow(
            webTarget.path(
                bundlePath
            ).path(
                "state"
            ).request().get()
        )) {
            return response.readEntity(BundleStateDTO.class).state;
        }
    }

    @Override
    public void startBundle(long id) throws Exception {
        BundleStateDTO bundleStateDTO = new BundleStateDTO();
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
            ).request().put(
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
        BundleStateDTO bundleStateDTO = new BundleStateDTO();
        bundleStateDTO.state = Bundle.ACTIVE;

        try (Response response = maybeThrow(
            webTarget.path(
                bundlePath
            ).path(
                "state"
            ).request().put(
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
        BundleStateDTO bundleStateDTO = new BundleStateDTO();
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
            ).request().put(
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
        BundleStateDTO bundleStateDTO = new BundleStateDTO();
        bundleStateDTO.state = Bundle.ACTIVE;
        bundleStateDTO.options = options;

        try (Response response = maybeThrow(
            webTarget.path(
                bundlePath
            ).path(
                "state"
            ).request().put(
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
        BundleStateDTO bundleStateDTO = new BundleStateDTO();
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
            ).request().put(
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
        BundleStateDTO bundleStateDTO = new BundleStateDTO();
        bundleStateDTO.state = Bundle.RESOLVED;

        try (Response response = maybeThrow(
            webTarget.path(
                bundlePath
            ).path(
                "state"
            ).request().put(
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
        BundleStateDTO bundleStateDTO = new BundleStateDTO();
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
            ).request().put(
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
        BundleStateDTO bundleStateDTO = new BundleStateDTO();
        bundleStateDTO.state = Bundle.RESOLVED;
        bundleStateDTO.options = options;

        try (Response response = maybeThrow(
            webTarget.path(
                bundlePath
            ).path(
                "state"
            ).request().put(
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
//        try (Response response = maybeThrow(
//            webTarget.path(
//                "framework"
//            ).path(
//                "bundle"
//            ).path(
//                "{bundleid}"
//            ).path(
//                "header"
//            ).resolveTemplate(
//                    "bundleid", id
//            ).request().get()
//        )) {
//        }
        // TODO
        return null;
    }

    @Override
    public Map<String, String> getBundleHeaders(String bundlePath) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BundleStartLevelDTO getBundleStartLevel(long id) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BundleStartLevelDTO getBundleStartLevel(String bundlePath) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setBundleStartLevel(long id, int startLevel) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void setBundleStartLevel(String bundlePath, int startLevel) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public BundleDTO installBundle(String location) throws Exception {
        try (Response response = maybeThrow(
            webTarget.path(
                "framework/bundles"
            ).request(APPLICATION_BUNDLE_JSON).post(
                Entity.entity(location, TEXT_PLAIN)
            )
        )) {
            return response.readEntity(
                BundleDTO.class
            );
        }
    }

    @Override
    public BundleDTO installBundle(String location, InputStream in) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BundleDTO uninstallBundle(long id) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BundleDTO uninstallBundle(String bundlePath) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BundleDTO updateBundle(long id) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BundleDTO updateBundle(long id, String url) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BundleDTO updateBundle(long id, InputStream in) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<String> getServicePaths() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<String> getServicePaths(String filter) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<ServiceReferenceDTO> getServiceReferences() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<ServiceReferenceDTO> getServiceReferences(String filter) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServiceReferenceDTO getServiceReference(long id) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServiceReferenceDTO getServiceReference(String servicePath) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    private Response maybeThrow(Response response) throws Exception {
        StatusType statusInfo = response.getStatusInfo();

        if (statusInfo.getStatusCode() >= 300) {
            throw new Exception(statusInfo.getReasonPhrase());
        }

        return response;
    }

}
