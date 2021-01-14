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

import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLESTATE_JSON_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLESTATE_XML_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.aries.jax.rs.rest.management.RestManagementConstants;
import org.apache.aries.jax.rs.rest.management.handler.RestManagementMessageBodyHandler;
import org.apache.aries.jax.rs.rest.management.model.BundleStateDTO;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.junit4.context.BundleContextRule;
import org.xmlunit.assertj3.XmlAssert;

import net.javacrumbs.jsonunit.assertj.JsonAssertions;

public class BundleStateTest extends TestUtil {

    @Rule
    public BundleContextRule bundleContextRule = new BundleContextRule();

    @InjectBundleContext
    BundleContext bundleContext;

    @Test
    public void getBundleStateJSON() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("bundle").path("{bundleid}").path("state");

        Response response = target.resolveTemplate(
            "bundleid", 2l
        ).request(
            APPLICATION_BUNDLESTATE_JSON_TYPE
        ).get();

        String result = response.readEntity(String.class);

        JsonAssertions.assertThatJson(
            result
        ).and(
            j -> j.isObject(),
            j -> j.node("state").isNumber().isEqualTo("32"),
            j -> j.node("options").isNumber().isEqualTo("2")
        );
    }

    @Test
    public void getBundleStateXML() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("bundle").path("{bundleid}").path("state");

        Response response = target.resolveTemplate(
            "bundleid", 2l
        ).request(
            APPLICATION_BUNDLESTATE_XML_TYPE
        ).get();

        String result = response.readEntity(String.class);

        XmlAssert.assertThat(
            result
        ).isInvalid(
        ).valueByXPath(
            "//bundleState/state"
        ).isEqualTo("32");
   }

    @Test
    public void getBundleStateDTO() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("bundle").path("{bundleid}").path("state");

        target.register(RestManagementMessageBodyHandler.class);

        Response response = target.resolveTemplate(
            "bundleid", 2l
        ).request(
            APPLICATION_BUNDLESTATE_JSON_TYPE
        ).get();

        BundleStateDTO bundleStateDTO = response.readEntity(BundleStateDTO.class);

        assertThat(
            bundleStateDTO
        ).hasFieldOrPropertyWithValue(
            "state", 32
        );
    }

    @Test
    public void putBundleState_start() throws Exception {
        WebTarget target = createDefaultTarget().path(
            "framework").path("bundles");

        BundleDTO bundleDTO = null;

        try {
            Response response = target.request().post(
                Entity.entity(
                    BundleStateTest.class.getClassLoader().getResourceAsStream("minor-change-1.0.1.jar"),
                    RestManagementConstants.APPLICATION_VNDOSGIBUNDLE_TYPE)
            );

            assertThat(
                response.getStatus()
            ).isEqualTo(
                200
            );

            bundleDTO = response.readEntity(BundleDTO.class);

            assertThat(
                bundleDTO.symbolicName
            ).isEqualTo(
                "minor-and-removed-change"
            );
            assertThat(
                bundleDTO.state
            ).isEqualTo(
                2
            );

            target = createDefaultTarget().path(
                "framework").path("bundle").path("{bundleid}").path("state");

            BundleStateDTO bundleStateDTO = new BundleStateDTO();
            bundleStateDTO.state = Bundle.ACTIVE;

            response = target.resolveTemplate(
                "bundleid", bundleDTO.id
            ).request().put(
                Entity.entity(
                    bundleStateDTO,
                    APPLICATION_BUNDLESTATE_JSON_TYPE
                )
            );

            assertThat(
                response.getStatus()
            ).isEqualTo(
                200
            );

            bundleStateDTO = response.readEntity(BundleStateDTO.class);

            assertThat(
                bundleStateDTO.state
            ).isEqualTo(
                32
            );
        }
        finally {
            if (bundleDTO != null) {
                bundleContext.getBundle(bundleDTO.id).uninstall();
            }
        }
    }

    @Test
    public void putBundleState_resolve() throws Exception {
        WebTarget target = createDefaultTarget().path(
            "framework").path("bundles");

        BundleDTO bundleDTO = null;

        try {
            Response response = target.request().post(
                Entity.entity(
                    BundleStateTest.class.getClassLoader().getResourceAsStream("minor-change-1.0.1.jar"),
                    RestManagementConstants.APPLICATION_VNDOSGIBUNDLE_TYPE)
            );

            assertThat(
                response.getStatus()
            ).isEqualTo(
                200
            );

            bundleDTO = response.readEntity(BundleDTO.class);

            assertThat(
                bundleDTO.symbolicName
            ).isEqualTo(
                "minor-and-removed-change"
            );
            assertThat(
                bundleDTO.state
            ).isEqualTo(
                2
            );

            target = createDefaultTarget().path(
                "framework").path("bundle").path("{bundleid}").path("state");

            BundleStateDTO bundleStateDTO = new BundleStateDTO();
            bundleStateDTO.state = Bundle.RESOLVED;

            response = target.resolveTemplate(
                "bundleid", bundleDTO.id
            ).request().put(
                Entity.entity(
                    bundleStateDTO,
                    APPLICATION_BUNDLESTATE_JSON_TYPE
                )
            );

            assertThat(
                response.getStatus()
            ).isEqualTo(
                200
            );

            bundleStateDTO = response.readEntity(BundleStateDTO.class);

            assertThat(
                bundleStateDTO.state
            ).isEqualTo(
                Bundle.RESOLVED
            );
        }
        finally {
            if (bundleDTO != null) {
                bundleContext.getBundle(bundleDTO.id).uninstall();
            }
        }
    }

    @Test
    public void putBundleState_stop() throws Exception {
        WebTarget target = createDefaultTarget().path(
            "framework").path("bundles");

        BundleDTO bundleDTO = null;

        try {
            Response response = target.request().post(
                Entity.entity(
                    BundleStateTest.class.getClassLoader().getResourceAsStream("minor-change-1.0.1.jar"),
                    RestManagementConstants.APPLICATION_VNDOSGIBUNDLE_TYPE)
            );

            assertThat(
                response.getStatus()
            ).isEqualTo(
                200
            );

            bundleDTO = response.readEntity(BundleDTO.class);

            assertThat(
                bundleDTO.symbolicName
            ).isEqualTo(
                "minor-and-removed-change"
            );
            assertThat(
                bundleDTO.state
            ).isEqualTo(
                2
            );

            target = createDefaultTarget().path(
                "framework").path("bundle").path("{bundleid}").path("state");

            BundleStateDTO bundleStateDTO = new BundleStateDTO();
            bundleStateDTO.state = Bundle.ACTIVE;

            response = target.resolveTemplate(
                "bundleid", bundleDTO.id
            ).request().put(
                Entity.entity(
                    bundleStateDTO,
                    APPLICATION_BUNDLESTATE_JSON_TYPE
                )
            );

            assertThat(
                response.getStatus()
            ).isEqualTo(
                200
            );

            bundleStateDTO = response.readEntity(BundleStateDTO.class);

            assertThat(
                bundleStateDTO.state
            ).isEqualTo(
                32
            );

            ////////////////

            target = createDefaultTarget().path(
                "framework").path("bundle").path("{bundleid}").path("state");

            bundleStateDTO = new BundleStateDTO();
            bundleStateDTO.state = Bundle.RESOLVED;

            response = target.resolveTemplate(
                "bundleid", bundleDTO.id
            ).request().put(
                Entity.entity(
                    bundleStateDTO,
                    APPLICATION_BUNDLESTATE_JSON_TYPE
                )
            );

            assertThat(
                response.getStatus()
            ).isEqualTo(
                200
            );

            bundleStateDTO = response.readEntity(BundleStateDTO.class);

            assertThat(
                bundleStateDTO.state
            ).isEqualTo(
                Bundle.RESOLVED
            );
        }
        finally {
            if (bundleDTO != null) {
                bundleContext.getBundle(bundleDTO.id).uninstall();
            }
        }
    }

}
