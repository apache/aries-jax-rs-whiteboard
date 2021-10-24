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
import org.apache.aries.jax.rs.rest.management.schema.BundleSchema;
import org.apache.aries.jax.rs.rest.management.schema.BundleStateSchema;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.xmlunit.assertj3.XmlAssert;

import net.javacrumbs.jsonunit.assertj.JsonAssertions;

public class BundleStateTest extends TestUtil {

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

        Response response = target.resolveTemplate(
            "bundleid", 2l
        ).request(
            APPLICATION_BUNDLESTATE_JSON_TYPE
        ).get();

        BundleStateSchema bundleStateDTO = response.readEntity(BundleStateSchema.class);

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

        BundleSchema bundleSchema = null;

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

        bundleSchema = response.readEntity(BundleSchema.class);

        assertThat(
            bundleSchema.symbolicName
        ).isEqualTo(
            "minor-and-removed-change"
        );
        assertThat(
            bundleSchema.state
        ).isEqualTo(
            2
        );

        target = createDefaultTarget().path(
            "framework").path("bundle").path("{bundleid}").path("state");

        BundleStateSchema bundleStateDTO = new BundleStateSchema();
        bundleStateDTO.state = Bundle.ACTIVE;

        response = target.resolveTemplate(
            "bundleid", bundleSchema.id
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

        bundleStateDTO = response.readEntity(BundleStateSchema.class);

        assertThat(
            bundleStateDTO.state
        ).isIn(
            2, 32
        );
    }

    @Test
    public void putBundleState_resolve() throws Exception {
        WebTarget target = createDefaultTarget().path(
            "framework").path("bundles");

        BundleSchema bundleSchema = null;

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

        bundleSchema = response.readEntity(BundleSchema.class);

        assertThat(
            bundleSchema.symbolicName
        ).isEqualTo(
            "minor-and-removed-change"
        );
        assertThat(
            bundleSchema.state
        ).isEqualTo(
            2
        );

        target = createDefaultTarget().path(
            "framework").path("bundle").path("{bundleid}").path("state");

        BundleStateSchema bundleStateDTO = new BundleStateSchema();
        bundleStateDTO.state = Bundle.RESOLVED;

        response = target.resolveTemplate(
            "bundleid", bundleSchema.id
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

        bundleStateDTO = response.readEntity(BundleStateSchema.class);

        assertThat(
            bundleStateDTO.state
        ).isEqualTo(
            Bundle.RESOLVED
        );
    }

    @Test
    public void putBundleState_stop() throws Exception {
        WebTarget target = createDefaultTarget().path(
            "framework").path("bundles");

        BundleSchema bundleSchema = null;

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

        bundleSchema = response.readEntity(BundleSchema.class);

        assertThat(
            bundleSchema.symbolicName
        ).isEqualTo(
            "minor-and-removed-change"
        );
        assertThat(
            bundleSchema.state
        ).isEqualTo(
            2
        );

        target = createDefaultTarget().path(
            "framework").path("bundle").path("{bundleid}").path("state");

        BundleStateSchema bundleStateDTO = new BundleStateSchema();
        bundleStateDTO.state = Bundle.ACTIVE;

        response = target.resolveTemplate(
            "bundleid", bundleSchema.id
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

        bundleStateDTO = response.readEntity(BundleStateSchema.class);

        assertThat(
            bundleStateDTO.state
        ).isIn(
            2, 32
        );

        ////////////////

        target = createDefaultTarget().path(
            "framework").path("bundle").path("{bundleid}").path("state");

        bundleStateDTO = new BundleStateSchema();
        bundleStateDTO.state = Bundle.RESOLVED;

        response = target.resolveTemplate(
            "bundleid", bundleSchema.id
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

        bundleStateDTO = response.readEntity(BundleStateSchema.class);

        assertThat(
            bundleStateDTO.state
        ).isEqualTo(
            Bundle.RESOLVED
        );
    }

}
