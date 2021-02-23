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

import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLESTARTLEVEL_JSON_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLESTARTLEVEL_XML_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.aries.jax.rs.rest.management.schema.BundleStartLevelSchema;
import org.junit.jupiter.api.Test;
import org.xmlunit.assertj3.XmlAssert;

import net.javacrumbs.jsonunit.assertj.JsonAssertions;

public class BundleStartLevelTest extends TestUtil {

    @Test
    public void getBundleStartLevelJSON() {
        WebTarget target = createDefaultTarget().path(
            "framework").path("bundle").path("{bundleid}").path("startlevel");

        Response response = target.resolveTemplate("bundleid", 2l).request().get();

        String result = response.readEntity(String.class);

        JsonAssertions.assertThatJson(
            result
        ).and(
            j -> j.isObject(),
            j -> j.node("startLevel").isNumber().isEqualTo("1"),
            j -> j.node("activationPolicyUsed").isBoolean().isTrue(),
            j -> j.node("persistentlyStarted").isBoolean().isTrue()
        );
    }

    @Test
    public void getBundleStartLevelJSONExplicit() {
        WebTarget target = createDefaultTarget().path(
            "framework").path("bundle").path("{bundleid}").path("startlevel");

        Response response = target.resolveTemplate("bundleid", 2l).request(
            APPLICATION_BUNDLESTARTLEVEL_JSON_TYPE).get();

        String result = response.readEntity(String.class);

        JsonAssertions.assertThatJson(
            result
        ).and(
            j -> j.isObject(),
            j -> j.node("startLevel").isNumber().isEqualTo("1"),
            j -> j.node("activationPolicyUsed").isBoolean().isTrue(),
            j -> j.node("persistentlyStarted").isBoolean().isTrue()
        );
    }

    @Test
    public void getBundleStartLevelXML() {
        WebTarget target = createDefaultTarget().path(
            "framework").path("bundle").path("{bundleid}").path("startlevel");

        Response response = target.resolveTemplate("bundleid", 2l).request(
            APPLICATION_BUNDLESTARTLEVEL_XML_TYPE).get();

        String result = response.readEntity(String.class);

        XmlAssert.assertThat(
            result
        ).isInvalid(
        ).valueByXPath(
            "//bundleStartLevel/bundle"
        ).isEqualTo("2");
    }

    @Test
    public void getBundleStartLevelSchema() {
        WebTarget target = createDefaultTarget().path(
            "framework").path("bundle").path("{bundleid}").path("startlevel");

        Response response = target.resolveTemplate("bundleid", 2l).request().get();

        BundleStartLevelSchema bundleStartLevelDTO =
            response.readEntity(BundleStartLevelSchema.class);

        assertThat(
            bundleStartLevelDTO.startLevel
        ).isGreaterThan(
            0
        );
    }

    @Test
    public void putBundleStartLevelBadValues() {
        WebTarget target = createDefaultTarget().path(
            "framework").path("bundle").path("{bundleid}").path("startlevel");

        BundleStartLevelSchema bundleStartLevelDTO =
            new BundleStartLevelSchema();

        Response response = target.resolveTemplate("bundleid", 2l).request().put(
            Entity.entity(
                bundleStartLevelDTO, APPLICATION_BUNDLESTARTLEVEL_JSON_TYPE));

        assertThat(
            response.getStatus()
        ).isEqualTo(
            400
        );
    }

    @Test
    public void putBundleStartLevel() {
        WebTarget target = createDefaultTarget().path(
            "framework").path("bundle").path("{bundleid}").path("startlevel");

        BundleStartLevelSchema bundleStartLevelDTO =
            new BundleStartLevelSchema();

        bundleStartLevelDTO.startLevel = 2;

        try {
            Response response = target.resolveTemplate("bundleid", 2l).request().put(
                Entity.entity(
                    bundleStartLevelDTO, APPLICATION_BUNDLESTARTLEVEL_JSON_TYPE));

            assertThat(
                response.getStatus()
            ).isEqualTo(
                200
            );
        }
        finally {
            bundleStartLevelDTO.startLevel = 1;

            Response response = target.resolveTemplate("bundleid", 2l).request().put(
                Entity.entity(
                    bundleStartLevelDTO, APPLICATION_BUNDLESTARTLEVEL_JSON_TYPE));

            assertThat(
                response.getStatus()
            ).isEqualTo(
                200
            );
        }
    }

}
