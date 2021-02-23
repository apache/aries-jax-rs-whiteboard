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

import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLES_JSON_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLES_REPRESENTATIONS_JSON;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLES_REPRESENTATIONS_JSON_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLES_REPRESENTATIONS_XML;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLES_REPRESENTATIONS_XML_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_BUNDLES_XML_TYPE;
import static org.apache.aries.jax.rs.rest.management.RestManagementConstants.APPLICATION_VNDOSGIBUNDLE_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.aries.jax.rs.rest.management.RestManagementConstants;
import org.apache.aries.jax.rs.rest.management.schema.BundleSchemaListSchema;
import org.apache.aries.jax.rs.rest.management.schema.BundleSchema;
import org.apache.aries.jax.rs.rest.management.schema.BundleListSchema;
import org.junit.jupiter.api.Test;
import org.osgi.test.assertj.bundle.BundleAssert;
import org.xmlunit.assertj3.XmlAssert;

import net.javacrumbs.jsonunit.assertj.JsonAssertions;

public class BundlesTest extends TestUtil {

    @Test
    public void getBundlesJSON() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("bundles");

        Response response = target.request(
            APPLICATION_BUNDLES_JSON_TYPE
        ).get();

        String result = response.readEntity(String.class);

        JsonAssertions.assertThatJson(
            result
        ).and(
            j -> j.isObject(),
            j -> j.node(
                "bundles"
            ).isArray(
            ).element(
                0
            ).asString(
            ).contains(
                "http://", "/framework/bundle/0"
            )
        );
    }

    //@Test
    public void getBundlesJSON_DOT() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("bundles.json");

        Response response = target.request().get();

        String result = response.readEntity(String.class);

        JsonAssertions.assertThatJson(
            result
        ).and(
            j -> j.isObject(),
            j -> j.node("bundles").isArray().element(0).asString().contains(
                "http://", "/framework/bundle/0"
            )
        );
    }

    @Test
    public void getBundlesXML() {
        WebTarget target = createDefaultTarget().path("framework").path("bundles");

        Response response = target.request(
            RestManagementConstants.APPLICATION_BUNDLES_XML_TYPE).get();

        String result = response.readEntity(String.class);

        XmlAssert.assertThat(
            result
        ).isInvalid().valueByXPath(
            "//bundles/uri[1]"
        ).contains("/framework/bundle/0");
    }

    //@Test
    public void getBundlesXML_DOT() {
        WebTarget target = createDefaultTarget().path("framework").path("bundles.xml");

        Response response = target.request().get();

        String result = response.readEntity(String.class);

        XmlAssert.assertThat(
            result
        ).isInvalid().valueByXPath(
            "//bundles/uri[2]"
        ).contains("/framework/bundle/1");
   }

    @Test
    public void getBundlesDTOJSON() {
        WebTarget target = createDefaultTarget().path("framework").path("bundles");

        Response response = target.request().get();

        BundleListSchema bundlesDTO = response.readEntity(BundleListSchema.class);

        assertThat(bundlesDTO.bundles.size()).isGreaterThan(0);
    }

    @Test
    public void getBundlesDTOXML() {
        WebTarget target = createDefaultTarget().path("framework").path("bundles");

        Response response = target.request(
            APPLICATION_BUNDLES_XML_TYPE
        ).get();

        BundleListSchema bundlesDTO = response.readEntity(BundleListSchema.class);

        assertThat(bundlesDTO.bundles.size()).isGreaterThan(0);
    }

    @Test
    public void getBundleSchemasJSON() {
        WebTarget target = createDefaultTarget().path("framework").path(
            "bundles"
        ).path("representations");

        Response response = target.request(
            APPLICATION_BUNDLES_REPRESENTATIONS_JSON
        ).get();

        String result = response.readEntity(String.class);

        JsonAssertions.assertThatJson(
            result
        ).and(
            j -> j.isObject(),
            j -> j.node("bundles").and(
                j1 -> j1.isArray(),
                j1 -> j1.node("[0]").and(
                    j2 -> j2.node("id").isNumber(),
                    j2 -> j2.node("lastModified").isNumber(),
                    j2 -> j2.node("state").isNumber(),
                    j2 -> j2.node("symbolicName").isString(),
                    j2 -> j2.node("version").isString()
                )
            )
        );
    }

    @Test
    public void getBundleSchemasXML() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("bundles").path("representations");

        Response response = target.request(
            APPLICATION_BUNDLES_REPRESENTATIONS_XML
        ).get();

        String result = response.readEntity(String.class);

        XmlAssert.assertThat(
            result
        ).isInvalid().valueByXPath(
            "//bundles/bundle[2]/id"
        ).contains("1");
    }

    @Test
    public void getBundleSchemas_ListJSON() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("bundles").path("representations");

        Response response = target.request(
            APPLICATION_BUNDLES_REPRESENTATIONS_JSON_TYPE
        ).get();

        BundleSchemaListSchema bundleSchemas = response.readEntity(BundleSchemaListSchema.class);

        assertThat(
            bundleSchemas.bundles
        ).isNotEmpty();
    }

    @Test
    public void getBundleSchemas_ListJSON_FilterError() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("bundles").path("representations");

        Response response = target.queryParam(
            "osgi.wiring.package", "osgi.wiring.package=org.*"
        ).request(
            APPLICATION_BUNDLES_REPRESENTATIONS_JSON_TYPE
        ).get();

        assertThat(
            response.getStatus()
        ).isEqualTo(400);
    }

    @Test
    public void getBundleSchemas_ListJSON_Filter_nomatch() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("bundles").path("representations");

        Response response = target.queryParam(
            "osgi.wiring.package", "(osgi.wiring.package=org.osgi.service.jaxrs)"
        ).request(
            APPLICATION_BUNDLES_REPRESENTATIONS_JSON_TYPE
        ).get();

        BundleSchemaListSchema bundleSchemas = response.readEntity(BundleSchemaListSchema.class);

        assertThat(
            bundleSchemas.bundles
        ).hasSize(0);
    }

    @Test
    public void getBundleSchemas_ListJSON_Filter() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("bundles").path("representations");

        Response response = target.queryParam(
            "osgi.wiring.package", "(osgi.wiring.package=org.osgi.service.*)"
        ).request(
            APPLICATION_BUNDLES_REPRESENTATIONS_JSON_TYPE
        ).get();

        BundleSchemaListSchema bundleSchemas = response.readEntity(BundleSchemaListSchema.class);

        assertThat(
            bundleSchemas.bundles
        ).hasSize(5);
    }

    @Test
    public void getBundleSchemas_ListJSON_FilterOr() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("bundles").path("representations");

        Response response = target.queryParam(
            "osgi.wiring.package", "(osgi.wiring.package=org.osgi.service.jaxrs.whiteboard)"
        ).queryParam(
            "osgi.wiring.package", "(osgi.wiring.package=org.osgi.service.cm)"
        ).request(
            APPLICATION_BUNDLES_REPRESENTATIONS_JSON_TYPE
        ).get();

        BundleSchemaListSchema bundleSchemas = response.readEntity(BundleSchemaListSchema.class);

        assertThat(
            bundleSchemas.bundles
        ).hasSize(2);
    }

    @Test
    public void getBundleSchemas_ListJSON_FilterAnd() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("bundles").path("representations");

        Response response = target.queryParam(
            "osgi.wiring.package", "(osgi.wiring.package=org.osgi.service.*)"
        ).queryParam(
            "osgi.identity", "(!(osgi.identity=org.osgi.*))"
        ).request(
            APPLICATION_BUNDLES_REPRESENTATIONS_JSON_TYPE
        ).get();

        BundleSchemaListSchema bundleSchemas = response.readEntity(BundleSchemaListSchema.class);

        assertThat(
            bundleSchemas.bundles
        ).hasSize(3);
    }

    @Test
    public void getBundleSchemas_ListXML() {
        WebTarget target = createDefaultTarget().path(
            "framework"
        ).path("bundles").path("representations");

        Response response = target.request(
            APPLICATION_BUNDLES_REPRESENTATIONS_XML_TYPE
        ).get();

        BundleSchemaListSchema bundleSchemas = response.readEntity(BundleSchemaListSchema.class);

        assertThat(
            bundleSchemas.bundles
        ).isNotEmpty();
    }

    @Test
    public void postBundlesByLocation() throws Exception {
        try (HttpServer server = new HttpServer(
                BundlesTest.class.getClassLoader().getResource("minor-change-1.0.1.jar"),
                "application/zip")) {

            WebTarget target = createDefaultTarget().path(
                "framework").path("bundles");

            BundleSchema bundleSchema = null;

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

            bundleSchema = response.readEntity(BundleSchema.class);

            assertThat(
                bundleSchema.symbolicName
            ).isEqualTo(
                "minor-and-removed-change"
            );
        }
    }

    @Test
    public void postBundlesByUpload() throws Exception {
        WebTarget target = createDefaultTarget().path(
            "framework").path("bundles");

        BundleSchema bundleSchema = null;

        Response response = target.request().header(
            HttpHeaders.CONTENT_LOCATION, "minor-change-1.0.1.jar"
        ).post(
            Entity.entity(
                BundlesTest.class.getClassLoader().getResourceAsStream("minor-change-1.0.1.jar"),
                APPLICATION_VNDOSGIBUNDLE_TYPE)
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
    public void postBundlesByUploadNoContentLocation() throws Exception {
        WebTarget target = createDefaultTarget().path(
            "framework").path("bundles");

        BundleSchema bundleSchema = null;

        Response response = target.request().post(
            Entity.entity(
                BundlesTest.class.getClassLoader().getResourceAsStream("minor-change-1.0.1.jar"),
                APPLICATION_VNDOSGIBUNDLE_TYPE)
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
        BundleAssert.assertThat(
            bundleContext.getBundle(bundleSchema.id)
        ).hasLocationThat().contains(
            "org.apache.aries.jax.rs.whiteboard:"
        );
    }

}
