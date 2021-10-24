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

import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLE_JSON_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLE_XML_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_VNDOSGIBUNDLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.osgi.framework.Bundle.UNINSTALLED;

import java.net.URL;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.aries.jax.rs.rest.management.RestManagementConstants;
import org.apache.aries.jax.rs.rest.management.schema.BundleSchema;
import org.junit.jupiter.api.Test;
import org.xmlunit.assertj3.XmlAssert;

import net.javacrumbs.jsonunit.assertj.JsonAssertions;

public class BundleTest extends TestUtil {

    @Test
    public void getBundleJSON() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("bundle").path("{bundleid}");

        Response response = target.resolveTemplate(
            "bundleid", 2l
        ).request(APPLICATION_BUNDLE_JSON_TYPE).get();

        String result = response.readEntity(String.class);

        JsonAssertions.assertThatJson(
            result
        ).and(
            j -> j.isObject(),
            j -> j.node("id").isNumber().isEqualTo("2"),
            j -> j.node("lastModified").isNumber(),
            j -> j.node("state").isNumber().isEqualByComparingTo("32"),
            j -> j.node("symbolicName").isString().isEqualTo("com.fasterxml.jackson.core.jackson-annotations"),
            j -> j.node("version").isString()
        );
    }

    @Test
    public void getBundleXML() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("bundle").path("{bundleid}");

        Response response = target.resolveTemplate("bundleid", 2l).request(
            APPLICATION_BUNDLE_XML_TYPE).get();

        String result = response.readEntity(String.class);

        XmlAssert.assertThat(
            result
        ).isInvalid().valueByXPath(
            "//bundle/id"
        ).contains("2");
   }

    @Test
    public void getBundleSchema() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("bundle").path("{bundleid}");

        Response response = target.resolveTemplate(
            "bundleid", 2l
        ).request(APPLICATION_BUNDLE_JSON_TYPE).get();

        BundleSchema bundleSchema = response.readEntity(BundleSchema.class);

        assertThat(
            bundleSchema.id
        ).isEqualTo(2);
    }

    @Test
    public void putBundleByLocation() throws Exception {
        URL resource = BundleTest.class.getClassLoader().getResource("minor-change-1.0.1.jar");
        try (HttpServer server = new HttpServer(resource, "application/zip")) {
            WebTarget target = createDefaultTarget().path(
                "framework").path("bundles");

            BundleSchema bundleSchema = null;

            Response response = target.request().post(
                Entity.entity(
                    resource.openStream(),
                    RestManagementConstants.APPLICATION_VNDOSGIBUNDLE_TYPE
                )
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

            target = createDefaultTarget().path(
                "framework").path("bundle").path("{bundleid}");

            response = target.resolveTemplate(
                "bundleid", bundleSchema.id
            ).request().put(
                Entity.entity(String.format(
                    "http://localhost:%d/minor-change-1.0.1.jar",
                    server.getPort()),
                MediaType.TEXT_PLAIN));

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
        }
    }

    @Test
    public void putBundleByUpload() throws Exception {
        WebTarget target = createDefaultTarget().path(
            "framework").path("bundles");

        BundleSchema bundleSchema = null;

        Response response = target.request().header(
            HttpHeaders.CONTENT_LOCATION, "minor-change-1.0.1.jar"
        ).post(
            Entity.entity(
                BundleTest.class.getClassLoader().getResourceAsStream("minor-change-1.0.1.jar"),
                APPLICATION_VNDOSGIBUNDLE)
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

        target = createDefaultTarget().path(
            "framework"
        ).path("bundle").path("{bundleid}");

        response = target.resolveTemplate(
            "bundleid", bundleSchema.id
        ).request().put(
            Entity.entity(
                BundleTest.class.getClassLoader().getResourceAsStream("minor-change-1.0.1.jar"),
                APPLICATION_VNDOSGIBUNDLE)
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
    }

    @Test
    public void deleteBundle() throws Exception {
        WebTarget target = createDefaultTarget().path(
            "framework").path("bundles");

        BundleSchema bundleSchema = null;

        Response response = target.request().header(
            HttpHeaders.CONTENT_LOCATION, "minor-change-1.0.1.jar"
        ).post(
            Entity.entity(
                BundleTest.class.getClassLoader().getResourceAsStream("minor-change-1.0.1.jar"),
                APPLICATION_VNDOSGIBUNDLE)
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

        target = createDefaultTarget().path(
            "framework"
        ).path("bundle").path("{bundleid}");

        response = target.resolveTemplate(
            "bundleid", bundleSchema.id
        ).request().delete();

        assertThat(
            response.getStatus()
        ).isEqualTo(
            200
        );

        bundleSchema = response.readEntity(BundleSchema.class);

        assertThat(
            bundleSchema.state
        ).matches(
            state -> (state & UNINSTALLED) == UNINSTALLED
        );
    }

}
