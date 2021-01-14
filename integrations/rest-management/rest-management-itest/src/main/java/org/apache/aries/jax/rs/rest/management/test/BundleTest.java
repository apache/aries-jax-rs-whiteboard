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

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.aries.jax.rs.rest.management.handler.RestManagementMessageBodyHandler;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.junit4.context.BundleContextRule;
import org.xmlunit.assertj3.XmlAssert;

import net.javacrumbs.jsonunit.assertj.JsonAssertions;

public class BundleTest extends TestUtil {

    @Rule
    public BundleContextRule bundleContextRule = new BundleContextRule();

    @InjectBundleContext
    BundleContext bundleContext;

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
    public void getBundleDTO() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("bundle").path("{bundleid}");

        target.register(RestManagementMessageBodyHandler.class);

        Response response = target.resolveTemplate(
            "bundleid", 2l
        ).request(APPLICATION_BUNDLE_JSON_TYPE).get();

        BundleDTO bundleDTO = response.readEntity(BundleDTO.class);

        assertThat(
            bundleDTO.id
        ).isEqualTo(2);
    }

    @Test
    public void putBundleByLocation() throws Exception {
        try (HttpServer server = new HttpServer(
                BundleTest.class.getClassLoader().getResource("minor-change-1.0.1.jar"),
                "application/zip")) {

            WebTarget target = createDefaultTarget().path(
                "framework").path("bundles");

            target.register(RestManagementMessageBodyHandler.class);

            BundleDTO bundleDTO = null;

            try {
                Response response = target.request().post(
                    Entity.entity(String.format(
                        "http://localhost:%d/minor-change-1.0.1.jar",
                        server.getPort()),
                    MediaType.TEXT_PLAIN));

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

                target = createDefaultTarget().path(
                    "framework").path("bundle").path("{bundleid}");

                target.register(RestManagementMessageBodyHandler.class);

                response = target.resolveTemplate(
                    "bundleid", bundleDTO.id
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

                bundleDTO = response.readEntity(BundleDTO.class);

                assertThat(
                    bundleDTO.symbolicName
                ).isEqualTo(
                    "minor-and-removed-change"
                );
            }
            finally {
                if (bundleDTO != null) {
                    bundleContext.getBundle(bundleDTO.id).uninstall();
                }
            }
        }
    }

    @Test
    public void putBundleByUpload() throws Exception {
        WebTarget target = createDefaultTarget().path(
            "framework").path("bundles");

        target.register(RestManagementMessageBodyHandler.class);

        BundleDTO bundleDTO = null;

        try {
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

            bundleDTO = response.readEntity(BundleDTO.class);

            assertThat(
                bundleDTO.symbolicName
            ).isEqualTo(
                "minor-and-removed-change"
            );

            target = createDefaultTarget().path(
                "framework"
            ).path("bundle").path("{bundleid}");

            target.register(RestManagementMessageBodyHandler.class);

            response = target.resolveTemplate(
                "bundleid", bundleDTO.id
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

            bundleDTO = response.readEntity(BundleDTO.class);

            assertThat(
                bundleDTO.symbolicName
            ).isEqualTo(
                "minor-and-removed-change"
            );
        }
        finally {
            if (bundleDTO != null) {
                bundleContext.getBundle(bundleDTO.id).uninstall();
            }
        }
    }

    @Test
    public void deleteBundle() throws Exception {
        WebTarget target = createDefaultTarget().path(
            "framework").path("bundles");

        target.register(RestManagementMessageBodyHandler.class);

        BundleDTO bundleDTO = null;

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

        bundleDTO = response.readEntity(BundleDTO.class);

        assertThat(
            bundleDTO.symbolicName
        ).isEqualTo(
            "minor-and-removed-change"
        );

        target = createDefaultTarget().path(
            "framework"
        ).path("bundle").path("{bundleid}");

        target.register(RestManagementMessageBodyHandler.class);

        response = target.resolveTemplate(
            "bundleid", bundleDTO.id
        ).request().delete();

        assertThat(
            response.getStatus()
        ).isEqualTo(
            200
        );

        bundleDTO = response.readEntity(BundleDTO.class);

        assertThat(
            bundleDTO.state
        ).matches(
            state -> (state & UNINSTALLED) == UNINSTALLED
        );
    }

}
